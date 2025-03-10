package com.pinterest.ktlint.cli.api

import com.pinterest.ktlint.cli.api.Baseline.Status.INVALID
import com.pinterest.ktlint.cli.api.Baseline.Status.NOT_FOUND
import com.pinterest.ktlint.cli.api.Baseline.Status.VALID
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.BASELINE_IGNORED
import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import mu.KotlinLogging
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrSelf

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Baseline of lint errors to be ignored in subsequent calls to ktlint.
 */
public class Baseline(
    /**
     * Path to the baseline file.
     */
    public val path: String? = null,
    /**
     * Status of the baseline file.
     */
    public val status: Status,
    /**
     * Lint errors grouped by (relative) file path.
     */
    public val lintErrorsPerFile: Map<String, List<KtlintCliError>> = emptyMap(),
) {
    public enum class Status {
        /**
         * Consumer did not request the Baseline file to be loaded.
         */
        DISABLED,

        /**
         * Baseline file is successfully parsed.
         */
        VALID,

        /**
         * Baseline file does not exist. File needs to be generated by the consumer first.
         */
        NOT_FOUND,

        /**
         * Baseline file is not successfully parsed. File needs to be regenerated by the consumer.
         */
        INVALID,
    }
}

/**
 * Loads the [Baseline] from the file located on [path].
 */
public fun loadBaseline(path: String): Baseline = BaselineLoader(path).load()

private class BaselineLoader(private val path: String) {
    var ruleReferenceWithoutRuleSetIdPrefix = 0

    fun load(): Baseline {
        require(path.isNotBlank()) { "Path for loading baseline may not be blank or empty" }

        Paths
            .get(path)
            .toFile()
            .takeIf { it.exists() }
            ?.let { baselineFile ->
                try {
                    return Baseline(
                        path = path,
                        lintErrorsPerFile = baselineFile.inputStream().parseBaseline(),
                        status = VALID,
                    ).also {
                        if (ruleReferenceWithoutRuleSetIdPrefix > 0) {
                            LOGGER.warn {
                                "Baseline file '$path' contains $ruleReferenceWithoutRuleSetIdPrefix reference(s) to rule ids without " +
                                    "a rule set id. For those references the rule set id 'standard' is assumed. It is advised to " +
                                    "regenerate this baseline file."
                            }
                        }
                    }
                } catch (e: IOException) {
                    LOGGER.error { "Unable to parse baseline file: $path" }
                } catch (e: ParserConfigurationException) {
                    LOGGER.error { "Unable to parse baseline file: $path" }
                } catch (e: SAXException) {
                    LOGGER.error { "Unable to parse baseline file: $path" }
                }

                // Baseline can not be parsed.
                try {
                    baselineFile.delete()
                } catch (e: IOException) {
                    LOGGER.error { "Unable to delete baseline file: $path" }
                }
                return Baseline(path = path, status = INVALID)
            }

        return Baseline(path = path, status = NOT_FOUND)
    }

    /**
     * Parses the [InputStream] of a baseline file and return the lint errors grouped by the relative file names.
     */
    private fun InputStream.parseBaseline(): Map<String, List<KtlintCliError>> {
        val lintErrorsPerFile = HashMap<String, List<KtlintCliError>>()
        with(parseDocument().getElementsByTagName("file")) {
            for (i in 0 until length) {
                with(item(i) as Element) {
                    val fileName = getAttribute("name")
                    lintErrorsPerFile[fileName] = parseBaselineFileElement()
                }
            }
        }
        return lintErrorsPerFile
    }

    private fun InputStream.parseDocument() =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(this)

    /**
     * Parses a "file" [Element] in the baseline file.
     */
    private fun Element.parseBaselineFileElement(): List<KtlintCliError> {
        val ktlintCliErrorsInFileElement = mutableListOf<KtlintCliError>()
        with(getElementsByTagName("error")) {
            for (i in 0 until length) {
                ktlintCliErrorsInFileElement.add(
                    with(item(i) as Element) {
                        parseBaselineErrorElement()
                    },
                )
            }
        }
        return ktlintCliErrorsInFileElement
    }

    /**
     * Parses an "error" [Element] in the baseline file.
     */
    private fun Element.parseBaselineErrorElement() =
        KtlintCliError(
            line = getAttribute("line").toInt(),
            col = getAttribute("column").toInt(),
            ruleId =
                getAttribute("source")
                    .let { ruleId ->
                        // Ensure backwards compatibility with baseline files in which the rule set id for standard rules is not saved
                        RuleId.prefixWithStandardRuleSetIdWhenMissing(ruleId)
                            .also { prefixedRuleId ->
                                if (prefixedRuleId != ruleId) {
                                    ruleReferenceWithoutRuleSetIdPrefix++
                                }
                            }
                    },
            detail = "", // Not available in the baseline
            status = BASELINE_IGNORED,
        )
}

/**
 * Checks if the list contains the given [KtlintCliError]. The [List.contains] function can not be used as [KtlintCliError.detail] is not
 * available in the baseline file and a normal equality check on the [KtlintCliError] fails.
 */
public fun List<KtlintCliError>.containsLintError(ktlintCliError: KtlintCliError): Boolean = any { it.isSameAs(ktlintCliError) }

private fun KtlintCliError.isSameAs(lintError: KtlintCliError) =
    col == lintError.col &&
        line == lintError.line &&
        RuleId.prefixWithStandardRuleSetIdWhenMissing(ruleId) == RuleId.prefixWithStandardRuleSetIdWhenMissing(lintError.ruleId)

/**
 * Checks if the list does not contain the given [KtlintCliError]. The [List.contains] function can not be used as [KtlintCliError.detail]
 * is not available in the baseline file and a normal equality check on the [KtlintCliError] fails.
 */
public fun List<KtlintCliError>.doesNotContain(ktlintCliError: KtlintCliError): Boolean = none { it.isSameAs(ktlintCliError) }

/**
 * Gets the relative route of the path. Also adjusts the slashes for uniformity between file systems.
 */
@Deprecated("Marked for removal from public API in KtLint 0.50")
public val Path.relativeRoute: String
    get() {
        val rootPath = Paths.get("").toAbsolutePath()
        return this
            .relativeToOrSelf(rootPath)
            .pathString
            .replace(File.separatorChar, '/')
    }
