"""Run nine screenshot measurements on one disposable GitHub-hosted runner."""
import collections
import hashlib
import json
import os
import platform
import re
import shutil
import signal
import subprocess
import sys
import tarfile
import threading
import time
from pathlib import Path

CACHE_PATHS = ('.gradle/caches', '.gradle/wrapper', '.konan')
SAMPLES = [('default', 1), ('1', 1), ('2', 1), ('1', 2), ('2', 2),
           ('default', 2), ('2', 3), ('default', 3), ('1', 3)]


def cache_fingerprint(home):
    aggregate = hashlib.sha256()
    count = size = 0
    for name in CACHE_PATHS:
        for path in sorted((home / name).rglob('*')):
            if path.is_file() and not path.is_symlink():
                digest = hashlib.sha256()
                with path.open('rb') as stream:
                    for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                        digest.update(chunk)
                label = str(path.relative_to(home))
                aggregate.update((label + '\0' + digest.hexdigest() + '\n').encode())
                count += 1
                size += path.stat().st_size
    return {'sha256': aggregate.hexdigest(), 'files': count, 'bytes': size}


def create_snapshot(home, archive):
    with tarfile.open(archive, 'w:gz', compresslevel=1) as output:
        for name in CACHE_PATHS:
            output.add(home / name, arcname=name)


def restore_snapshot(home, archive):
    for name in CACHE_PATHS:
        shutil.rmtree(home / name)
    subprocess.run(['tar', '-xzf', str(archive), '-C', str(home)], check=True)


def build_command(workers, scenarios, init_script):
    command = ['./gradlew', 'compareRoborazziIosSimulatorArm64']
    for module in sorted(scenarios):
        command += [f':feature:{module}:iosSimulatorArm64Test', '--rerun']
    command += ['--stacktrace', '--profile', '--console=plain', '--init-script', str(init_script)]
    if workers != 'default':
        command += ['--max-workers=' + workers]
    return command


def output(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT).strip()


def environment():
    return {'image_os': os.environ.get('ImageOS'), 'image_version': os.environ.get('ImageVersion'),
            'runner_name': os.environ.get('RUNNER_NAME'), 'arch': platform.machine(),
            'os': output(['sw_vers', '-productVersion']), 'cpu': output(['sysctl', '-n', 'hw.ncpu']),
            'memory': output(['sysctl', '-n', 'hw.memsize']), 'xcode': output(['xcodebuild', '-version']),
            'sdk': output(['xcrun', '--sdk', 'iphonesimulator', '--show-sdk-version']),
            'java': output(['java', '-version']), 'swift': output(['xcrun', 'swift', '--version'])}


def stop_build_daemons():
    listing = output(['jps', '-l'])
    names = {'org.gradle.launcher.daemon.bootstrap.GradleDaemon',
             'org.jetbrains.kotlin.daemon.KotlinCompileDaemon'}
    pids = [int(parts[0]) for row in listing.splitlines()
            if len(parts := row.split()) >= 2 and parts[1] in names]
    for pid in pids:
        try:
            os.kill(pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
    for _ in range(30):
        alive = []
        for pid in pids:
            try:
                os.kill(pid, 0)
                alive.append(pid)
            except ProcessLookupError:
                pass
        pids = alive
        if not pids:
            return
        time.sleep(0.5)
    for pid in pids:
        os.kill(pid, signal.SIGKILL)
    time.sleep(1)


def sample_resources(done, samples):
    while not done.is_set():
        try:
            rows = output(['ps', '-axo', 'rss=,%cpu=,comm=']).splitlines()
            rss = cpu = 0.0
            for row in rows:
                parts = row.strip().split(None, 2)
                if len(parts) != 3:
                    continue
                name = Path(parts[2]).name
                if name in {'java', 'konanc', 'clang', 'ld'} or name.endswith('.kexe'):
                    rss += float(parts[0]) * 1024
                    cpu += float(parts[1])
            samples.append({'epoch': time.time(), 'rss_bytes': int(rss), 'ps_cpu_percent': cpu})
        except (subprocess.SubprocessError, ValueError):
            pass
        done.wait(2)


def validate_results(workspace, scenarios, destination, log):
    rendered = re.findall(r'^> Task (:feature:[^:]+:iosSimulatorArm64Test)$', log, re.M)
    expected = {f':feature:{module}:iosSimulatorArm64Test' for module in scenarios}
    assert len(rendered) == len(expected) and set(rendered) == expected, rendered
    summary = {'platform': 'ios', 'base_sha': os.environ['SOURCE_SHA'],
               'head_sha': os.environ['SOURCE_SHA'], 'total': 0, 'changed': [], 'added': [], 'removed': []}
    for module, ids in scenarios.items():
        path = workspace / f'feature/{module}/build/test-results/roborazzi/iosSimulatorArm64/results-summary.json'
        results = json.loads(path.read_text())['results']
        names = [Path(item['golden_file_path']).name for item in results]
        assert sorted(names) == sorted(name + '.png' for name in ids), (module, names)
        for item in results:
            assert item['type'] in {'unchanged', 'changed'}, item
            summary['total'] += 1
            if item['type'] == 'changed':
                summary['changed'].append(module + '/' + Path(item['golden_file_path']).stem)
        target = destination / f'feature/{module}'
        target.mkdir(parents=True)
        shutil.copy2(path, target / 'results-summary.json')
    assert summary['total'] == 14, summary
    report = destination / 'build/screenshot-report'
    report.mkdir(parents=True)
    (report / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    profile = workspace / 'build/reports/profile'
    assert len(list(profile.glob('profile-*.html'))) == 1
    shutil.copytree(profile, destination / 'build/reports/profile')
    return summary


def prepare(root, workspace, home):
    root.mkdir(parents=True, exist_ok=True)
    seed = json.loads((home / 'caro-worker-cache/identity.json').read_text())
    fingerprint = cache_fingerprint(home)
    assert fingerprint == {key: seed[key] for key in fingerprint}, (seed, fingerprint)
    initial = environment()
    # The snapshot may come from a different runner image. All NEW measurements use this same runner.
    for key in ('arch', 'os', 'cpu', 'memory', 'xcode', 'sdk'):
        assert initial[key] == seed[key], (key, initial[key], seed[key])
    (root / 'environment.json').write_text(json.dumps(initial, indent=2) + '\n')
    (root / 'fingerprint.json').write_text(json.dumps(fingerprint, indent=2) + '\n')
    shutil.copytree(workspace / 'build/screenshot-baseline', root / 'golden')
    create_snapshot(home, root / 'frozen.tar.gz')
    (root / 'worker.init.gradle').write_text(
        'gradle.settingsEvaluated { println("BENCHMARK_WORKERS_MAX=" + gradle.startParameter.maxWorkerCount) }\n')
    print(json.dumps({'cache': fingerprint, 'runner': initial}, indent=2), flush=True)


def measure(root, workspace, home, workers, sample):
    cycle_started = time.monotonic()
    destination = root / f'worker-{workers}-sample-{sample}'
    metadata = destination / 'build/worker-benchmark'
    metadata.mkdir(parents=True)
    stop_build_daemons()
    subprocess.run(['xcrun', 'simctl', 'shutdown', 'all'], check=True)
    subprocess.run(['xcrun', 'simctl', 'erase', 'all'], check=True)
    # Only the disposable CI checkout is cleaned. Measurement files live outside it.
    subprocess.run(['git', 'clean', '-ffdX'], cwd=workspace, check=True, stdout=subprocess.DEVNULL)
    assert output(['git', '-C', str(workspace), 'rev-parse', 'HEAD']) == os.environ['SOURCE_SHA']
    assert output(['git', '-C', str(workspace), 'diff', '--exit-code']) == ''
    restore_snapshot(home, root / 'frozen.tar.gz')
    fingerprint = cache_fingerprint(home)
    assert fingerprint == json.loads((root / 'fingerprint.json').read_text()), fingerprint
    current = environment()
    assert current == json.loads((root / 'environment.json').read_text()), current
    (metadata / 'cache-identity.json').write_text(json.dumps({**fingerprint, **current}, indent=2) + '\n')
    scenarios = json.loads((workspace / 'scripts/screenshot/scenarios.json').read_text())
    assert scenarios == json.loads((root / 'golden/scenarios.json').read_text())
    for module, ids in scenarios.items():
        target = workspace / f'feature/{module}/build/outputs/roborazzi/iosSimulatorArm64'
        target.mkdir(parents=True)
        for name in ids:
            shutil.copy2(root / f'golden/{module}/{name}.png', target / f'{name}.png')
    (workspace / 'local.properties').write_text(
        'CARO_DEV_SERVER=https://example.invalid/\nCARO_QA_SERVER=https://example.invalid/\n'
        'CARO_PROD_SERVER=https://example.invalid/\nGOOGLE_WEB_CLIENT_ID=screenshot-test\n')
    command = build_command(workers, scenarios, root / 'worker.init.gradle')
    samples = []
    done = threading.Event()
    sampler = threading.Thread(target=sample_resources, args=(done, samples), daemon=True)
    started_epoch = time.time()
    started = time.monotonic()
    sampler.start()
    try:
        with (metadata / 'gradle.log').open('w') as log:
            process = subprocess.Popen(command, cwd=workspace, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, text=True)
            for line in process.stdout:
                print(line, end='', flush=True)
                log.write(line)
            result = process.wait()
            process.stdout.close()
    finally:
        elapsed = time.monotonic() - started
        done.set()
        sampler.join(timeout=10)
    log = (metadata / 'gradle.log').read_text()
    effective = re.search(r'BENCHMARK_WORKERS_MAX=(\d+)', log)
    assert effective, 'Missing actual worker limit'
    effective = int(effective.group(1))
    assert effective == (int(current['cpu']) if workers == 'default' else int(workers))
    record = {'workers': workers, 'sample': sample, 'effective_workers': effective,
              'source_sha': os.environ['SOURCE_SHA'], 'workflow_sha': os.environ['GITHUB_SHA'],
              'golden_run': os.environ['GOLDEN_RUN'], 'cache_key': os.environ['FROZEN_CACHE_KEY'],
              'started_epoch': started_epoch, 'finished_epoch': time.time(), 'elapsed_seconds': elapsed,
              'exit_code': result, 'command': command, 'resources': samples,
              'peak_sampled_rss_bytes': max((s['rss_bytes'] for s in samples), default=0),
              'peak_sampled_ps_cpu_percent': max((s['ps_cpu_percent'] for s in samples), default=0)}
    (metadata / 'measurement.json').write_text(json.dumps(record, indent=2) + '\n')
    assert result == 0, f'Gradle exited with {result}'
    summary = validate_results(workspace, scenarios, destination, log)
    record['cycle_seconds'] = time.monotonic() - cycle_started
    (metadata / 'measurement.json').write_text(json.dumps(record, indent=2) + '\n')
    with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as report:
        report.write(f'\n- worker {workers}, sample {sample}: {elapsed:.2f}s; '
                     f'{summary["total"]} scenarios; changed={len(summary["changed"])}\n')
    print('BENCHMARK_SAMPLE_COMPLETE ' + json.dumps({k: record[k] for k in
          ('workers', 'sample', 'elapsed_seconds', 'cycle_seconds', 'effective_workers')}), flush=True)


def summarize(root):
    records = [json.loads(path.read_text()) for path in sorted(root.glob('worker-*/build/worker-benchmark/measurement.json'))]
    assert len(records) == 9
    assert collections.Counter((r['workers'], r['sample']) for r in records) == collections.Counter(SAMPLES)
    assert all(r['exit_code'] == 0 and 'cycle_seconds' in r for r in records)
    (root / 'measurements.json').write_text(json.dumps(records, indent=2) + '\n')
    print('BENCHMARK_ALL_NINE_SAMPLES_COMPLETE', flush=True)


if __name__ == '__main__':
    root = Path(os.environ['RUNNER_TEMP']) / 'caro-worker-sequential'
    workspace = Path(os.environ['GITHUB_WORKSPACE'])
    home = Path.home()
    if sys.argv[1] == 'prepare':
        prepare(root, workspace, home)
    elif sys.argv[1] == 'sample':
        measure(root, workspace, home, sys.argv[2], int(sys.argv[3]))
    elif sys.argv[1] == 'summarize':
        summarize(root)
    else:
        raise SystemExit('Unknown benchmark command')
