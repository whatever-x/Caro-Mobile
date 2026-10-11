package com.whatever.caro.feature.importer.regex

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain

private val REJECTED =
    mapOf(
        """\{\{c\d+(?:,\d+)*::|}}""" to "이스케이프되지 않은 '}'",
        "a}" to "이스케이프되지 않은 '}'",
        "a{" to "이스케이프되지 않은 '{'",
        "{{c1::" to "이스케이프되지 않은 '{'",
        "a{,3}" to "이스케이프되지 않은 '{'",
        "a{3,2}" to "거꾸로",
        "{2}a" to "대상이 없거나",
        "a{2}{3}" to "연속",
        "*a" to "대상이 없다",
        "a**" to "연속",
        "(a" to "닫히지 않은 그룹",
        "a)" to "짝이 없는",
        "[a" to "닫히지 않은 문자 클래스",
        "[[:alpha:]]" to "'['",
        "[a[b]]" to "'['",
        "[a&&[^b]]" to "교집합",
        "[a--b]" to "차집합",
        "[z-a]" to "거꾸로",
        "[a-\\d]" to "집합",
        "[\\d-x]" to "'-'",
        "[a{2}]" to "클래스 안의",
        "[\\b]" to "뜻이 갈린다",
        "\\p{Alpha}" to "일반 범주",
        "\\p{javaLowerCase}" to "일반 범주",
        "\\p{IsHan}" to "일반 범주",
        "\\pL" to "일반 범주",
        "[\\p{Han}]" to "일반 범주",
        "(?<=a+)b" to "lookbehind",
        "(?<!a*)b" to "lookbehind",
        "(?<=a{1,})b" to "lookbehind",
        "(?P<name>a)" to "인라인 플래그",
        "(?'name'a)" to "인라인 플래그",
        "(?#comment)a" to "인라인 플래그",
        "(?u)a" to "인라인 플래그",
        "(?x)a b" to "인라인 플래그",
        "(?<1a>x)" to "이름 있는 그룹",
        "\\h" to "이식성이 없는",
        "\\R" to "이식성이 없는",
        "\\Qa.b\\E" to "이식성이 없는",
        "\\e" to "이식성이 없는",
        "\\u12" to "16진수",
        "\\x4" to "16진수",
        "a\\" to "끝에 남은",
    )

private val ACCEPTED =
    listOf(
        """\{\{c\d+(?:,\d+)*::|\}\}""",
        """&(?:#(\d{1,7});?|#[xX]([0-9a-fA-F]{1,6});?|([A-Za-z][A-Za-z0-9]{1,31});)""",
        """^([+-])?(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?${'$'}""",
        """\n{3,}""",
        """a{0,}""",
        """<rp(?=[\s/>])[^<>]*>[\s\S]*?(?:</rp\s*>|(?=<r[tbp](?=[\s/>])|</ruby\s*>)|${'$'})""",
        """(?<![\d,])(10,?000|5,?000)(?![\d,])""",
        """(?<=ab)c""",
        """(?<=a{1,3})b""",
        """(?<!a?)b""",
        """(?<year>\d{4})""",
        """[ \t]""",
        """[^<>]""",
        """[+-]""",
        """[-a]""",
        """[a-z-]""",
        """[ᄀ-ᇿ가-힣]""",
        """[A-Z]""",
        """\p{L}\p{Nd}\P{Lu}[\p{L}\d]""",
        """(?i)a(?:b)(?-i)c""",
        """(?s:a.b)""",
        """a++b*+c?+d+?e*?""",
        """ \x41\.\(\{\}\[\]""",
        """열 (\d+)(?: \(\d+\))?""",
        """(?>a|b)\1""",
    )

class IcuRegexCheckerTest :
    FunSpec({
        REJECTED.forEach { (pattern, reason) ->
            test("ICU에서 깨지거나 뜻이 갈리는 패턴 ${pattern}을 거부한다") {
                val violations = IcuRegexChecker.violations(pattern)

                violations.shouldNotBeEmpty()
                violations.joinToString() shouldContain reason
            }
        }

        ACCEPTED.forEach { pattern ->
            test("JVM, ICU, Native가 같게 받는 패턴 ${pattern}은 통과시킨다") {
                IcuRegexChecker.violations(pattern).shouldBeEmpty()
            }
        }

        listOf("CANON_EQ", "COMMENTS", "LITERAL", "UNIX_LINES").forEach { option ->
            test("ICU가 지원하지 않거나 뜻이 갈리는 옵션 ${option}을 거부한다") {
                IcuRegexChecker.violations("a", listOf(option)).joinToString() shouldContain option
            }
        }

        listOf("IGNORE_CASE", "MULTILINE", "DOT_MATCHES_ALL").forEach { option ->
            test("양쪽이 같게 지원하는 옵션 ${option}은 통과시킨다") {
                IcuRegexChecker.violations("a", listOf(option)).shouldBeEmpty()
            }
        }
    })
