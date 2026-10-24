package com.whatever.caro.feature.importer.regex

import java.io.File

internal class RegexSite(
    val location: String,
    val pattern: String?,
    val options: List<String>,
    val failure: String?,
)

// 컴파일된 Regex 객체는 private 상수에 갇혀 있으므로, 새 정규식이 레지스트리 등록 없이도 검사받도록 소스의 Regex(...) 리터럴을 직접 읽는다.
// 상수 보간과 문자열 이어붙이기, replace 호출만 평가하고 그 밖의 식은 failure로 남겨 조용히 건너뛰지 않는다.
internal object RegexLiteralScanner {
    private val REGEX_CALL = Regex("""(?<!\w)Regex\(""")
    private val TO_REGEX_CALL = Regex("""\.toRegex\(""")
    private val CONSTANT_DECLARATION =
        Regex("""(?m)^\s*(?:(?:private|internal|public|const)\s+)*val\s+([A-Z][A-Z0-9_]*)\s*(?::\s*[\w.<>?]+\s*)?=\s*""")
    private val REGEX_OPTION = Regex("""RegexOption\.(\w+)""")

    fun scan(
        root: File,
        modules: List<String>,
    ): List<RegexSite> {
        val files = productionFiles(root, modules)
        val texts = files.associateWith { it.readText() }
        val constants = constants(texts.values)
        return texts.flatMap { (file, text) -> sites(root, file, text, constants) }
    }

    private fun productionFiles(
        root: File,
        modules: List<String>,
    ): List<File> =
        modules.flatMap { module ->
            File(root, "$module/src")
                .listFiles { dir -> dir.isDirectory && dir.name.endsWith("Main") }
                .orEmpty()
                .flatMap { sourceSet -> sourceSet.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
        }

    private fun constants(texts: Collection<String>): Map<String, String> =
        texts
            .flatMap { text ->
                CONSTANT_DECLARATION.findAll(text).map { match ->
                    match.groupValues[1] to
                        ExpressionText.readStatement(text, match.range.last + 1)
                }
            }.toMap()

    private fun sites(
        root: File,
        file: File,
        text: String,
        constants: Map<String, String>,
    ): List<RegexSite> {
        val evaluator = ExpressionEvaluator(constants)
        val relative = file.relativeTo(root).path
        val regexCalls = REGEX_CALL.findAll(text).map { it.range.last to "Regex(" }
        val extensionCalls = TO_REGEX_CALL.findAll(text).map { it.range.first to "toRegex" }
        return (regexCalls + extensionCalls)
            .filterNot { (position, _) -> isCommentLine(text, position) }
            .map { (position, kind) ->
                val location = "$relative:${text.take(position).count { it == '\n' } + 1}"
                if (kind == "toRegex") {
                    RegexSite(location, null, emptyList(), "확장 함수 toRegex()는 수집기가 평가하지 못한다. Regex(...) 리터럴로 쓴다")
                } else {
                    site(location, ExpressionText.readArguments(text, position)?.first, evaluator)
                }
            }.toList()
    }

    private fun isCommentLine(
        text: String,
        position: Int,
    ): Boolean {
        val line = text.substring(text.lastIndexOf('\n', position) + 1, position).trimStart()
        return line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")
    }

    private fun site(
        location: String,
        arguments: List<String>?,
        evaluator: ExpressionEvaluator,
    ): RegexSite {
        if (arguments == null || arguments.isEmpty() || arguments.size > 2) {
            return RegexSite(location, null, emptyList(), "Regex(...) 인자를 읽지 못했다")
        }
        val options =
            arguments
                .getOrNull(1)
                ?.let { expression ->
                    REGEX_OPTION.findAll(expression).map { it.groupValues[1] }.toList()
                }.orEmpty()
        return runCatching { evaluator.evaluate(arguments.first()) }.fold(
            onSuccess = { pattern ->
                val optionFailure = "RegexOption 식을 읽지 못했다".takeIf { arguments.size == 2 && options.isEmpty() }
                RegexSite(location, pattern, options, optionFailure)
            },
            onFailure = { error -> RegexSite(location, null, options, error.message ?: error::class.simpleName) },
        )
    }
}
