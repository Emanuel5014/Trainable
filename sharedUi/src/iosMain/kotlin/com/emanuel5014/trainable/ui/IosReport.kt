@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.report.HtmlReportFormatter
import com.emanuel5014.trainable.data.report.ReportGenerator
import com.emanuel5014.trainable.data.report.ReportText
import com.emanuel5014.trainable.domain.prescription.BlockChange
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.ProgramDiff
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.load_preview_line
import com.emanuel5014.trainable.sharedui.resources.program_edit_block_added
import com.emanuel5014.trainable.sharedui.resources.program_edit_block_removed
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.components.PrescriptionText
import com.emanuel5014.trainable.ui.screens.routines.ReportContent
import com.emanuel5014.trainable.ui.screens.routines.ReportModel
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import org.jetbrains.compose.resources.stringResource
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.UIKit.UIUserInterfaceStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.time.Clock

/**
 * The report on iOS: the shared screen with a WKWebView preview; Share opens the system share sheet with the HTML file,
 * Save the Files export sheet. The report is written in the phone's language until the app's own choice reaches iOS.
 */
@Composable
internal fun IosReportScreen(planIdsString: String, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val text by rememberUpdatedState(rememberIosReportText())
    val model = remember(planIdsString) {
        ReportModel(
            planIds = ReportModel.planIdsOf(planIdsString),
            reportGenerator = ReportGenerator(IosAppGraph.workouts, IosAppGraph.preferences) { text },
            htmlReportFormatter = HtmlReportFormatter(),
            languageCode = { Locale.current.language },
            scope = scope
        )
    }
    // The picker holds its delegate weakly: this one lives as long as the screen
    val saveDelegate = remember(model) {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                model.saved()
            }
        }
    }

    fun write(name: String): NSURL? {
        val html = model.uiState.value.html ?: return null
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + name)
        val bytes = html.encodeToByteArray()
        val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
        return if (data.writeToURL(url, atomically = true)) url else null.also { model.failed("Could not write $name") }
    }

    ReportContent(
        state = model,
        onNavigateBack = onNavigateBack,
        onSave = {
            val url = write(ReportModel.fileName(model.uiState.value, Clock.System.now().toEpochMilliseconds()))
            if (url != null) {
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                picker.delegate = saveDelegate
                topController(presenter).presentViewController(picker, animated = true, completion = null)
            }
        },
        onShare = {
            val url = write("trainable_report_${Clock.System.now().toEpochMilliseconds()}.html")
            if (url != null) {
                val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
                topController(presenter).presentViewController(sheet, animated = true, completion = null)
            }
        },
        preview = { html, modifier, endInset ->
            // The page follows the app's theme, not the phone's (its CSS reads prefers-color-scheme)
            val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            UIKitView(
                factory = {
                    WKWebView(frame = CGRectZero.readValue(), configuration = WKWebViewConfiguration()).apply {
                        // No white flash behind the page while it loads
                        setOpaque(false)
                        backgroundColor = UIColor.clearColor
                        scrollView.backgroundColor = UIColor.clearColor
                        loadHTMLString(html, baseURL = null)
                    }
                },
                modifier = modifier,
                update = { view ->
                    view.overrideUserInterfaceStyle =
                        if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    // The page runs under the home indicator; its end scrolls clear of it
                    view.scrollView.contentInsetAdjustmentBehavior =
                        UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
                    view.scrollView.setContentInset(UIEdgeInsetsMake(0.0, 0.0, endInset.value.toDouble(), 0.0))
                },
                properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true)
            )
        }
    )
}

private fun topController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}

/** The report's words on iOS: the app's prescription words plus the lines of a program edit, as Android writes them. */
@Composable
private fun rememberIosReportText(): ReportText {
    val prescription = LocalPrescriptionText.current
    val changed = stringResource(Res.string.load_preview_line)
    val added = stringResource(Res.string.program_edit_block_added)
    val removed = stringResource(Res.string.program_edit_block_removed)
    return remember(prescription, changed, added, removed) { IosReportText(prescription, changed, added, removed) }
}

private class IosReportText(
    private val prescription: PrescriptionText,
    private val changed: String,
    private val added: String,
    private val removed: String
) : ReportText {
    /** "%1$s" / "%1$d" placeholders, filled the way Android's getString does. */
    private fun fill(template: String, vararg args: Any): String =
        Regex("%(\\d+)\\$[sd]").replace(template) { m -> args.getOrNull(m.groupValues[1].toInt() - 1)?.toString() ?: m.value }

    override fun labels(weightUnit: String): PrescriptionLabels = prescription.labels(weightUnit)

    override fun programChangeLines(weightUnit: String, before: List<PrescriptionBlock>, after: List<PrescriptionBlock>): List<String> {
        val labels = labels(weightUnit)
        fun text(block: PrescriptionBlock) = PrescriptionFormatter.summary(listOf(block), labels)
        return ProgramDiff.changes(before, after).map { change ->
            when (change.kind) {
                BlockChange.Kind.CHANGED -> fill(changed, change.blockNumber, text(change.before!!), text(change.after!!))
                BlockChange.Kind.ADDED -> fill(added, change.blockNumber, text(change.after!!))
                BlockChange.Kind.REMOVED -> fill(removed, change.blockNumber, text(change.before!!))
            }
        }
    }

    override fun setPrescriptionBadges(set: SetLogEntity): List<String> = prescription.setPrescriptionBadges(set)

    override fun setPerformanceBadges(set: SetLogEntity): List<String> = prescription.setPerformanceBadges(set)
}
