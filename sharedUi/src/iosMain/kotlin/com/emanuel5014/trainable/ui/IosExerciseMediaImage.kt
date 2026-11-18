@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emanuel5014.trainable.data.repository.iosExerciseMediaPath
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.UIKit.*

internal data class IosExerciseMediaFrame(val image: UIImage, val animated: Boolean, val aspect: Float)

/** ImageIO holds the encoded animation; only its current downsampled frame is decoded, rather than all GIF frames. */
internal class IosExerciseMediaSource(private val source: CGImageSourceRef) {
    val count = CGImageSourceGetCount(source).toInt()
    fun close() = CFRelease(source)

    fun frame(index: Int, maxPx: Int): IosExerciseMediaFrame? {
        val options = NSMutableDictionary().apply {
            setValue(true, cfKey(kCGImageSourceCreateThumbnailFromImageAlways))
            setValue(true, cfKey(kCGImageSourceCreateThumbnailWithTransform))
            setValue(maxPx, cfKey(kCGImageSourceThumbnailMaxPixelSize))
        }
        val retained = CFBridgingRetain(options) ?: return null
        val cgImage = try { CGImageSourceCreateThumbnailAtIndex(source, index.toULong(), retained.reinterpret()) }
            finally { CFRelease(retained) } ?: return null
        return try {
            val image = UIImage.imageWithCGImage(cgImage)
            val aspect = CGImageGetWidth(cgImage).toFloat() / CGImageGetHeight(cgImage).coerceAtLeast(1uL).toFloat()
            IosExerciseMediaFrame(image, count > 1, aspect)
        } finally { CGImageRelease(cgImage) }
    }

    fun durationMillis(index: Int): Long {
        val properties = CGImageSourceCopyPropertiesAtIndex(source, index.toULong(), null) ?: return 100
        val dictionary = CFBridgingRelease(properties) as? Map<*, *> ?: return 100
        val seconds = dictionary.values.filterIsInstance<Map<*, *>>().firstNotNullOfOrNull { values ->
            (values["UnclampedDelayTime"] as? NSNumber)?.doubleValue
                ?: (values["DelayTime"] as? NSNumber)?.doubleValue
        } ?: 0.1
        return (seconds * 1000).toLong().coerceAtLeast(20)
    }

    companion object {
        fun open(path: String): IosExerciseMediaSource? {
            val url = CFBridgingRetain(NSURL.fileURLWithPath(path)) ?: return null
            val source = try { CGImageSourceCreateWithURL(url.reinterpret(), null) } finally { CFRelease(url) }
                ?: return null
            return IosExerciseMediaSource(source)
        }
    }
}

private fun cfKey(key: CFStringRef?): String =
    key?.let { CFBridgingRelease(CFRetain(it)) as? String }.orEmpty()

@Composable
internal fun IosExerciseMediaImage(fileName: String, modifier: Modifier, crop: Boolean, playing: Boolean,
                                   maxPx: Int, onLoaded: (Boolean, Float) -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) visible = true
            if (event == Lifecycle.Event.ON_PAUSE) visible = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var index by remember(fileName) { mutableIntStateOf(0) }
    val frame by produceState<IosExerciseMediaFrame?>(null, fileName, maxPx, playing, visible) {
        val path = iosExerciseMediaPath(fileName) ?: run { value = null; return@produceState }
        val source = IosExerciseMediaSource.open(path) ?: run { value = null; return@produceState }
        try {
            while (isActive) {
                val current = index.coerceIn(0, (source.count - 1).coerceAtLeast(0))
                value = withContext(Dispatchers.IO) { source.frame(current, maxPx) }
                if (!playing || !visible || source.count <= 1) awaitCancellation()
                delay(source.durationMillis(current))
                index = (current + 1) % source.count
            }
        } finally { source.close() }
    }
    val callback by rememberUpdatedState(onLoaded)
    UIKitView(
        factory = { UIImageView().apply { clipsToBounds = true; isAccessibilityElement = false } },
        modifier = modifier,
        update = { view ->
            view.contentMode = if (crop) UIViewContentMode.UIViewContentModeScaleAspectFill else UIViewContentMode.UIViewContentModeScaleAspectFit
            view.image = frame?.image
            frame?.let { callback(it.animated, it.aspect) }
        },
        onRelease = { it.image = null },
        properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false)
    )
}
