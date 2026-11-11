package com.emanuel5014.trainable.ui.screens.analytics

/** The widgets the "Aggiungi widget" gallery offers, in the old add menu's order. */
enum class GalleryWidget { Calendar, BodyWeight, CategoryVolume, Volume, ProgressCards, ExerciseChart, PeriodComparison }

/** [presentKey] = list key of the widget already on screen (single-instance types only); null = addable. */
data class GalleryEntry(val type: GalleryWidget, val presentKey: String?)

const val KEY_HEADER = "header"
const val KEY_ONE_REP_MAXES = "one_rep_maxes"
const val KEY_PROGRESS_CARDS = "progress_cards"
const val KEY_ADD_TILE = "add_tile"

/** Always one entry per [GalleryWidget], in enum order. Volume and ExerciseChart can be added many times. */
fun galleryEntries(widgets: List<AnalyticsWidget>, showProgressCards: Boolean): List<GalleryEntry> {
    fun idOf(predicate: (AnalyticsWidget) -> Boolean) = widgets.firstOrNull(predicate)?.id
    return GalleryWidget.entries.map { type ->
        val presentKey = when (type) {
            GalleryWidget.Calendar -> idOf { it is AnalyticsWidget.Calendar }
            GalleryWidget.BodyWeight -> idOf { it is AnalyticsWidget.BodyWeight }
            GalleryWidget.CategoryVolume -> idOf { it is AnalyticsWidget.CategoryVolume }
            GalleryWidget.PeriodComparison -> idOf { it is AnalyticsWidget.TimePeriodComparison }
            GalleryWidget.ProgressCards -> KEY_PROGRESS_CARDS.takeIf { showProgressCards }
            GalleryWidget.Volume, GalleryWidget.ExerciseChart -> null
        }
        GalleryEntry(type, presentKey)
    }
}

/**
 * Keys of the analytics LazyColumn, in order: header, [one_rep_maxes], [progress_cards], widget ids…, add_tile.
 * AnalyticsContent emits its items in this same order; the index of a key here is its list index.
 */
fun analyticsListKeys(showOneRepMaxes: Boolean, showProgressCards: Boolean, widgetIds: List<String>): List<String> =
    buildList {
        add(KEY_HEADER)
        if (showOneRepMaxes) add(KEY_ONE_REP_MAXES)
        if (showProgressCards) add(KEY_PROGRESS_CARDS)
        addAll(widgetIds)
        add(KEY_ADD_TILE)
    }

/**
 * After an add, scroll to the newest widget only when the list really grew from [sizeAtAdd]
 * (an add can be refused, e.g. a merged exercise: then nothing must jump later on a removal).
 */
fun shouldScrollToNewWidget(sizeAtAdd: Int?, currentSize: Int): Boolean =
    sizeAtAdd != null && currentSize > sizeAtAdd
