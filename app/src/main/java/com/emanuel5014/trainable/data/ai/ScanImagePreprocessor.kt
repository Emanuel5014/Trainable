package com.emanuel5014.trainable.data.ai

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A single-channel image, one float (0..255) per pixel in row-major order. */
class GrayPlane(val width: Int, val height: Int, val pixels: FloatArray) {
    init {
        require(pixels.size == width * height) { "pixel count does not match ${width}x$height" }
    }
}

/**
 * Makes photographed handwriting readable for the on-device vision model.
 *
 * The model looks at a small square version of the picture (a few hundred pixels per side), so on a
 * photo of a whole sheet where a handful of lines are written in one corner the text ends up a pixel
 * or two tall and the read is garbage. [findTextRegion] locates the block of writing on the page and
 * [enhance] flattens the lighting so faint pencil and shadows don't matter; the caller crops to the
 * region before handing the picture to the model.
 *
 * Pure Kotlin on float arrays so it can be unit-tested on the JVM.
 */
object ScanImagePreprocessor {

    /** Longest side the analysis expects; bigger pictures should be scaled down to this first. */
    const val ANALYSIS_SIZE = 1000

    data class Region(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    private data class Letter(val x0: Int, val y0: Int, val x1: Int, val y1: Int, val area: Int)

    private class Cluster(var x0: Int, var y0: Int, var x1: Int, var y1: Int, var ink: Int, var letters: Int)

    fun toGray(argb: IntArray, width: Int, height: Int): GrayPlane {
        val out = FloatArray(width * height)
        for (i in out.indices) {
            val c = argb[i]
            out[i] = 0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)
        }
        return GrayPlane(width, height, out)
    }

    /**
     * The bounding box of the main block of writing, or null when the picture has no recognisable
     * text block or the block already fills the frame (nothing to gain from cropping).
     *
     * Strokes are found with a black top-hat (thin dark marks on a locally bright background), which
     * ignores wide dark areas such as a wooden table. Candidate "letters" are then filtered by size,
     * shape and by how calm the surrounding background is (paper is flat, wood grain is not) and
     * grouped into blocks; the block with the most ink wins.
     */
    fun findTextRegion(gray: GrayPlane): Region? {
        val w = gray.width
        val h = gray.height
        val d = max(w, h)
        if (d < 200) return null

        val strokeRadius = max(5, (d / 140.0).roundToInt() or 1) / 2
        val closing = closing(gray.pixels, w, h, strokeRadius)
        val tophat = FloatArray(w * h) { closing[it] - gray.pixels[it] }
        val busyRadius = max(4, (d * 0.0125f).toInt())
        val busy = localRange(closing, w, h, busyRadius)

        var letters = collectLetters(tophat, busy, w, h, threshold = 22f)
        if (letters.size < MIN_LETTERS) letters = collectLetters(tophat, busy, w, h, threshold = 12f)
        if (letters.size < MIN_LETTERS) return null

        val cluster = mainCluster(letters, d) ?: return null

        val padX = max(d * 0.015f, (cluster.x1 - cluster.x0) * 0.05f).toInt()
        val padY = max(d * 0.015f, (cluster.y1 - cluster.y0) * 0.05f).toInt()
        val region = Region(
            left = max(0, cluster.x0 - padX),
            top = max(0, cluster.y0 - padY),
            right = min(w, cluster.x1 + 1 + padX),
            bottom = min(h, cluster.y1 + 1 + padY)
        )
        val fraction = region.width.toFloat() * region.height / (w.toFloat() * h)
        return region.takeIf { fraction in 0.015f..0.88f }
    }

    /**
     * Divides out the paper's lighting (shadows, tint, gradients), then stretches contrast so the
     * ink is dark and the paper is white. Output is 0..255.
     */
    fun enhance(gray: GrayPlane): GrayPlane {
        val w = gray.width
        val h = gray.height
        val d = max(w, h)
        val window = max(9, (d / 40) or 1)
        val paper = boxBlur(closing(gray.pixels, w, h, window / 2), w, h, window / 2)

        val ratio = FloatArray(w * h) { min(1.2f, gray.pixels[it] / max(paper[it], 1f)) }

        // 0.5th percentile of the ratio = the darkest ink; it becomes black
        val histogram = IntArray(HISTOGRAM_BINS)
        ratio.forEach { histogram[(it / 1.2f * (HISTOGRAM_BINS - 1)).toInt().coerceIn(0, HISTOGRAM_BINS - 1)]++ }
        var seen = 0
        var bin = 0
        val target = (ratio.size * 0.005f).toInt()
        while (bin < HISTOGRAM_BINS - 1 && seen + histogram[bin] <= target) {
            seen += histogram[bin]
            bin++
        }
        val low = (bin.toFloat() / (HISTOGRAM_BINS - 1) * 1.2f).coerceIn(0.2f, 0.8f)

        val out = FloatArray(w * h) {
            val v = ((ratio[it] - low) / (1f - low)).coerceIn(0f, 1f)
            Math.pow(v.toDouble(), INK_GAMMA).toFloat() * 255f
        }
        return GrayPlane(w, h, out)
    }

    // ---- letter candidates -------------------------------------------------------------------

    private fun collectLetters(tophat: FloatArray, busy: FloatArray, w: Int, h: Int, threshold: Float): List<Letter> {
        val d = max(w, h)
        val minHeight = max(4, (d * 0.006f).toInt())
        val maxHeight = (d * 0.09f).toInt()
        val maxWidth = (d * 0.2f).toInt()

        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        val letters = ArrayList<Letter>()

        for (start in tophat.indices) {
            if (visited[start] || tophat[start] <= threshold) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var minX = Int.MAX_VALUE
            var minY = Int.MAX_VALUE
            var maxX = -1
            var maxY = -1
            var busySum = 0f
            while (head < tail) {
                val p = queue[head++]
                val x = p % w
                val y = p / w
                busySum += busy[p]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                for (ny in max(0, y - 1)..min(h - 1, y + 1)) {
                    for (nx in max(0, x - 1)..min(w - 1, x + 1)) {
                        val q = ny * w + nx
                        if (!visited[q] && tophat[q] > threshold) {
                            visited[q] = true
                            queue[tail++] = q
                        }
                    }
                }
            }

            val area = tail
            val boxW = maxX - minX + 1
            val boxH = maxY - minY + 1
            if (area < 8 || boxH < minHeight || boxH > maxHeight || boxW > maxWidth) continue
            if (boxW.toFloat() / boxH > MAX_LETTER_ASPECT || boxH.toFloat() / boxW > 12f) continue
            if (area.toFloat() / (boxW * boxH) < 0.10f) continue
            // Paper is flat behind the ink; wood grain and printed grids are not
            if (busySum / area > MAX_BACKGROUND_BUSYNESS) continue
            letters += Letter(minX, minY, maxX, maxY, area)
        }
        // A photo of pure texture can yield thousands of candidates; the biggest ones are enough
        return if (letters.size > MAX_LETTERS) letters.sortedByDescending { it.area }.take(MAX_LETTERS) else letters
    }

    private fun mainCluster(letters: List<Letter>, d: Int): Cluster? {
        val gapX = (d * 0.06f).toInt()
        val gapY = (d * 0.032f).toInt()

        // Union-find over letters that sit close enough to be part of the same block of writing
        val parent = IntArray(letters.size) { it }
        fun find(i: Int): Int {
            var r = i
            while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
            return r
        }
        for (i in letters.indices) {
            for (j in i + 1 until letters.size) {
                val a = letters[i]
                val b = letters[j]
                val dx = max(0, max(a.x0 - b.x1, b.x0 - a.x1))
                val dy = max(0, max(a.y0 - b.y1, b.y0 - a.y1))
                if (dx <= gapX && dy <= gapY) parent[find(i)] = find(j)
            }
        }

        val clusters = HashMap<Int, Cluster>()
        letters.forEachIndexed { i, l ->
            val c = clusters.getOrPut(find(i)) { Cluster(l.x0, l.y0, l.x1, l.y1, 0, 0) }
            c.x0 = min(c.x0, l.x0); c.y0 = min(c.y0, l.y0)
            c.x1 = max(c.x1, l.x1); c.y1 = max(c.y1, l.y1)
            c.ink += l.area
            c.letters++
        }

        val main = clusters.values.maxByOrNull { it.ink } ?: return null
        if (main.letters < MIN_LETTERS) return null

        // Absorb nearby fragments (a title set apart from the lines, week labels with nothing after them)
        val absorbGap = (d * 0.06f).toInt()
        val pending = clusters.values.filter { it !== main && it.letters >= 3 }.toMutableList()
        var changed = true
        while (changed) {
            changed = false
            val iterator = pending.iterator()
            while (iterator.hasNext()) {
                val c = iterator.next()
                if (c.x0 <= main.x1 + absorbGap && c.x1 >= main.x0 - absorbGap &&
                    c.y0 <= main.y1 + absorbGap && c.y1 >= main.y0 - absorbGap
                ) {
                    main.x0 = min(main.x0, c.x0); main.y0 = min(main.y0, c.y0)
                    main.x1 = max(main.x1, c.x1); main.y1 = max(main.y1, c.y1)
                    main.ink += c.ink
                    main.letters += c.letters
                    iterator.remove()
                    changed = true
                }
            }
        }
        return main
    }

    // ---- morphology and blur (separable, O(n)) -------------------------------------------------

    private fun closing(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray =
        extreme(extreme(src, w, h, radius, max = true), w, h, radius, max = false)

    /** max - min over a (2r+1) square: how much the background varies around each pixel. */
    private fun localRange(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val hi = extreme(src, w, h, radius, max = true)
        val lo = extreme(src, w, h, radius, max = false)
        return FloatArray(src.size) { hi[it] - lo[it] }
    }

    private fun extreme(src: FloatArray, w: Int, h: Int, radius: Int, max: Boolean): FloatArray {
        val rows = extremeRows(src, w, h, radius, max)
        val transposed = transpose(rows, w, h)
        val cols = extremeRows(transposed, h, w, radius, max)
        return transpose(cols, h, w)
    }

    /** Sliding-window extreme along each row using a monotonic deque. */
    private fun extremeRows(src: FloatArray, w: Int, h: Int, radius: Int, max: Boolean): FloatArray {
        val out = FloatArray(src.size)
        val deque = IntArray(w)
        for (y in 0 until h) {
            val base = y * w
            var head = 0
            var tail = 0
            for (i in 0 until w + radius) {
                if (i < w) {
                    val v = src[base + i]
                    while (tail > head && (if (max) src[base + deque[tail - 1]] <= v else src[base + deque[tail - 1]] >= v)) tail--
                    deque[tail++] = i
                }
                val p = i - radius
                if (p >= 0) {
                    while (deque[head] < p - radius) head++
                    out[base + p] = src[base + deque[head]]
                }
            }
        }
        return out
    }

    private fun transpose(src: FloatArray, w: Int, h: Int): FloatArray {
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) out[x * h + y] = src[y * w + x]
        }
        return out
    }

    private fun boxBlur(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val rows = blurRows(src, w, h, radius)
        return transpose(blurRows(transpose(rows, w, h), h, w, radius), h, w)
    }

    private fun blurRows(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            val base = y * w
            var sum = 0f
            var count = 0
            // Prime the window for x = 0: [0, radius]
            for (i in 0..min(radius, w - 1)) { sum += src[base + i]; count++ }
            for (x in 0 until w) {
                out[base + x] = sum / count
                val add = x + radius + 1
                if (add < w) { sum += src[base + add]; count++ }
                val drop = x - radius
                if (drop >= 0) { sum -= src[base + drop]; count-- }
            }
        }
        return out
    }

    private const val MIN_LETTERS = 12
    private const val MAX_LETTERS = 2500
    private const val MAX_LETTER_ASPECT = 3.6f
    private const val MAX_BACKGROUND_BUSYNESS = 24f
    private const val HISTOGRAM_BINS = 256
    private const val INK_GAMMA = 1.3
}
