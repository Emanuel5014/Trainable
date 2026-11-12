package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.util.DistanceUnitConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.CardioEntryValues
import com.emanuel5014.trainable.domain.exercise.CardioHistory
import com.emanuel5014.trainable.domain.exercise.DurationDigits
import com.emanuel5014.trainable.domain.exercise.SuggestedExercise
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.platform.formatLocalDecimals
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.cardio_entry_label
import com.emanuel5014.trainable.sharedui.resources.cardio_missing_values
import com.emanuel5014.trainable.sharedui.resources.cardio_name_used_by_deleted_strength
import com.emanuel5014.trainable.sharedui.resources.cardio_remove_set
import com.emanuel5014.trainable.sharedui.resources.change
import com.emanuel5014.trainable.sharedui.resources.compare_pace
import com.emanuel5014.trainable.sharedui.resources.confirm
import com.emanuel5014.trainable.sharedui.resources.distance
import com.emanuel5014.trainable.sharedui.resources.duration
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.routine_notes
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.set_number
import com.emanuel5014.trainable.sharedui.resources.speed
import com.emanuel5014.trainable.sharedui.resources.today
import com.emanuel5014.trainable.sharedui.resources.`when`
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.fullDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

/** Where the cardio entry sheet was opened from; it decides what the sheet shows (spec 6.2). */
enum class CardioEntryMode { ADD_FROM_HISTORY, ADD_TO_WORKOUT, EDIT_SET }

/**
 * What the sheet gives back. [timestamp] only from History (the moment chosen, never in the future), [rpe] only when
 * editing a set (the set's own value when the scale is off).
 */
data class CardioEntry(val seconds: Int, val distanceKm: Float, val timestamp: Long?, val note: String?, val rpe: Float?)

/**
 * The bottom sheet that logs or edits one cardio set of [exercise]: duration and distance tiles, pace and speed, a note,
 * the date and time from History, the RPE when a set is edited. Built like the History workout sheet (big title, rounded
 * stat tiles) and scrolls, so it fits small screens and large fonts. The distance is typed and shown in [distanceUnit]
 * ("km" or "mi") and given back in km; a distance left as it was opened comes back exactly as stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioEntrySheet(
    exercise: ExerciseEntity,
    mode: CardioEntryMode,
    initial: CardioEntry?,
    distanceUnit: String,
    rpeScale: RpeScale?,
    languageCode: String,
    onChangeExercise: (() -> Unit)?,
    onSave: (CardioEntry) -> Unit,
    onDismiss: () -> Unit,
    /** The name shown as title; [exercise]'s translated name when null (a set passes `CardioNames.of`). */
    title: String? = null,
    /** Editing a set: removes it (null when it is the only set of its exercise). */
    onDelete: (() -> Unit)? = null,
    /** Editing a set of an exercise done in several sets: its number, shown above the title. */
    setNumber: Int? = null
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    val separator = remember { formatLocalDecimals(1.5, 1).getOrElse(1) { '.' } }

    var digits by remember { mutableStateOf(DurationDigits.digitsOf(initial?.seconds ?: 0)) }
    var distanceText by remember {
        mutableStateOf(CardioEntryValues.distanceText(DistanceUnitConverter.toDisplay(initial?.distanceKm ?: 0f, distanceUnit), separator))
    }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var rpe by remember { mutableStateOf(initial?.rpe) }
    // The day the date picker gave (UTC midnight) and the time; null = now
    var pickedMoment by remember {
        mutableStateOf(initial?.timestamp?.let { PickedMoment.of(it, zone) })
    }
    var showMissing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<Long?>(null) }

    // A value left as it was opened is saved exactly as stored (the tiles round to 3 decimals and cap at 99:59:59);
    // the distance is compared in the unit shown, so km never go through miles and back when untouched
    val read = CardioEntryValues.read(digits, distanceText, initial?.seconds, initial?.distanceKm, separator, distanceUnit)
    val seconds = read.seconds
    val km = read.km
    val missing = CardioEntryValues.isMissing(read, editing = mode == CardioEntryMode.EDIT_SET)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.horizontalPadding, spacing = 12.dp) {
                EntryHeader(
                    label = when {
                        mode != CardioEntryMode.EDIT_SET -> stringResource(Res.string.cardio_entry_label)
                        setNumber != null -> stringResource(Res.string.set_number, setNumber)
                        else -> stringResource(Res.string.edit)
                    },
                    title = title ?: ExerciseTranslations.translate(exercise.nome, languageCode),
                    onChange = onChangeExercise.takeIf { mode != CardioEntryMode.EDIT_SET },
                    onDelete = onDelete.takeIf { mode == CardioEntryMode.EDIT_SET }
                )

                if (mode == CardioEntryMode.ADD_FROM_HISTORY) {
                    WhenRow(
                        timestamp = pickedMoment?.timestamp(zone) ?: Clock.System.now().toEpochMilliseconds(),
                        zone = zone,
                        onClick = { showDatePicker = true }
                    )
                }

                ValueTiles(
                    digits = digits,
                    onDigitsChange = {
                        digits = it
                        showMissing = false
                    },
                    distanceText = distanceText,
                    unitLabel = distanceUnitLabel(distanceUnit),
                    onDistanceChange = {
                        distanceText = it
                        showMissing = false
                    }
                )

                val pace = DistanceUnitConverter.paceSeconds(seconds, km, distanceUnit)
                val speed = CardioHistory.speedKmh(seconds, km)
                if (showMissing && missing) {
                    Text(
                        text = stringResource(Res.string.cardio_missing_values),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else if (pace != null && speed != null) {
                    val paceText = DurationDigits.format(pace) + " /" + distanceUnitLabel(distanceUnit)
                    val speedText = speedValueText(speed, distanceUnit)
                    val description = stringResource(Res.string.compare_pace) + " " + paceText + ", " +
                        stringResource(Res.string.speed) + " " + speedText
                    Text(
                        text = "$paceText · $speedText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .semantics { contentDescription = description }
                    )
                }

                NoteRow(note = note, onNoteChange = { note = it })

                if (mode == CardioEntryMode.EDIT_SET && rpeScale != null) {
                    RpeSelector(value = rpe, onValueChange = { rpe = it }, scale = rpeScale, modifier = Modifier.padding(top = 4.dp))
                }
            }
            SheetFormFooter(horizontalPadding = ResponsiveSize.horizontalPadding) {
                GymButton(
                    onClick = {
                        if (missing) {
                            showMissing = true
                        } else {
                            val now = Clock.System.now().toEpochMilliseconds()
                            onSave(
                                CardioEntry(
                                    seconds = seconds,
                                    distanceKm = km,
                                    timestamp = if (mode == CardioEntryMode.ADD_FROM_HISTORY) {
                                        pickedMoment?.let { CardioEntryValues.timestampOf(it.dateMillis, it.hour, it.minute, zone, now) } ?: now
                                    } else null,
                                    note = note.trim().ifBlank { null },
                                    rpe = if (mode == CardioEntryMode.EDIT_SET) rpe else null
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GymButtonText(stringResource(Res.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (showDatePicker) {
        val now = remember { Clock.System.now().toEpochMilliseconds() }
        val selectable = remember(now) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    CardioEntryValues.isSelectableDay(utcTimeMillis, now, zone)

                override fun isSelectableYear(year: Int): Boolean =
                    year <= Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).year
            }
        }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = pickedMoment?.dateMillis ?: CardioEntryValues.pickerDateOf(now, zone),
            selectableDates = selectable
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDate = dateState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Surface)
        ) {
            DatePicker(state = dateState)
        }
    }

    pendingDate?.let { date ->
        val start = pickedMoment ?: PickedMoment.of(Clock.System.now().toEpochMilliseconds(), zone)
        val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { pendingDate = null },
            title = { Text(stringResource(Res.string.`when`), style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    pickedMoment = PickedMoment(date, timeState.hour, timeState.minute)
                    pendingDate = null
                }) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDate = null }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                }
            },
            containerColor = Surface
        ) {
            TimePicker(state = timeState)
        }
    }
}

/** Picker label, then the cardio's name with "Change" on its right, or the delete button when a set is edited. */
@Composable
private fun EntryHeader(label: String, title: String, onChange: (() -> Unit)?, onDelete: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = ResponsiveSize.labelLargeSize),
            color = Primary,
            fontWeight = FontWeight.ExtraBold
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onChange != null) {
                M3Surface(onClick = onChange, shape = CircleShape, color = Primary.copy(alpha = 0.1f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(Res.string.change),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            maxLines = 1
                        )
                    }
                }
            }
            if (onDelete != null) {
                GymIconButton(
                    icon = Icons.Rounded.Delete,
                    onClick = onDelete,
                    description = stringResource(Res.string.cardio_remove_set),
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error
                )
            }
        }
    }
}

/** The rounded "When" row: the day and time of a cardio added from History. */
@Composable
private fun WhenRow(timestamp: Long, zone: TimeZone, onClick: () -> Unit) {
    val today = stringResource(Res.string.today).replaceFirstChar { it.uppercase() }
    val text = remember(timestamp, today) {
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone)
        val nowDate = Clock.System.now().toLocalDateTime(zone).date
        val day = if (local.date == nowDate) today else fullDate(timestamp)
        "$day, ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
    M3Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                Text(text = stringResource(Res.string.`when`), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        }
    }
}

/** Duration and distance side by side, one above the other when the font is very large. */
@Composable
private fun ValueTiles(
    digits: String,
    onDigitsChange: (String) -> Unit,
    distanceText: String,
    unitLabel: String,
    onDistanceChange: (String) -> Unit
) {
    val distanceFocus = remember { FocusRequester() }
    val stacked = LocalDensity.current.fontScale >= 1.6f
    val duration: @Composable (Modifier) -> Unit = { m ->
        DurationTile(digits, onDigitsChange, onNext = { distanceFocus.requestFocus() }, modifier = m)
    }
    val distance: @Composable (Modifier) -> Unit = { m ->
        DistanceTile(distanceText, unitLabel, onDistanceChange, distanceFocus, modifier = m)
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            duration(Modifier.fillMaxWidth())
            distance(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            duration(Modifier.weight(1f))
            distance(Modifier.weight(1f))
        }
    }
}

/**
 * A tile of the History workout sheet (same shape, colour and padding: value over a small label) holding a field;
 * tapping anywhere on it types into the field, and a primary outline shows while it is being typed in.
 */
@Composable
private fun EntryTile(
    label: String,
    focused: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .then(if (focused) Modifier.border(BorderStroke(2.dp, Primary), RoundedCornerShape(16.dp)) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        field()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun tileValueStyle() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = OnSurface)

@Composable
private fun DurationTile(digits: String, onDigitsChange: (String) -> Unit, onNext: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val style = tileValueStyle()
    EntryTile(label = stringResource(Res.string.duration), focused = focused, focusRequester = focus, modifier = modifier) {
        BasicTextField(
            // The cursor stays at the end: digits come in from the right like on a timer
            value = TextFieldValue(digits, TextRange(digits.length)),
            onValueChange = { value ->
                val typed = value.text.filter { it in '0'..'9' }.trimStart('0')
                if (typed.length <= DurationDigits.MAX_DIGITS && typed != digits) onDigitsChange(typed)
            },
            textStyle = style,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { onNext() }),
            visualTransformation = DurationTransformation,
            cursorBrush = SolidColor(Primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onFocusChanged { state ->
                    // Leaving the tile carries places above 59 over (90 seconds become 1:30)
                    if (focused && !state.isFocused) {
                        val normalised = DurationDigits.digitsOf(DurationDigits.seconds(digits))
                        if (normalised != digits) onDigitsChange(normalised)
                    }
                    focused = state.isFocused
                },
            decorationBox = { inner ->
                Box {
                    if (digits.isEmpty()) Text("0:00", style = style, color = OnSurfaceVariant.copy(alpha = 0.5f), maxLines = 1)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun DistanceTile(text: String, unitLabel: String, onTextChange: (String) -> Unit, focus: FocusRequester, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(TextRange(text.length)) }
    val style = tileValueStyle()
    val unitColor = OnSurfaceVariant
    val unit = remember(unitColor, unitLabel) { SuffixTransformation(" $unitLabel", SpanStyle(color = unitColor, fontWeight = FontWeight.Medium)) }
    EntryTile(label = stringResource(Res.string.distance), focused = focused, focusRequester = focus, modifier = modifier) {
        BasicTextField(
            value = TextFieldValue(text, selection.coerceIn(text.length)),
            onValueChange = { value ->
                val cleaned = CardioEntryValues.cleanDistanceInput(value.text) ?: return@BasicTextField
                selection = if (cleaned == value.text) value.selection else TextRange(cleaned.length)
                if (cleaned != text) onTextChange(cleaned)
            },
            textStyle = style,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            visualTransformation = unit,
            cursorBrush = SolidColor(Primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("0 $unitLabel", style = style, color = OnSurfaceVariant.copy(alpha = 0.5f), maxLines = 1)
                    inner()
                }
            }
        )
    }
}

private fun TextRange.coerceIn(length: Int) = TextRange(start.coerceIn(0, length), end.coerceIn(0, length))

/** The rounded note row; optional, several lines. */
@Composable
private fun NoteRow(note: String, onNoteChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val style = MaterialTheme.typography.bodyLarge.copy(color = OnSurface)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focus.requestFocus()
                keyboard?.show()
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = Primary, modifier = Modifier.padding(top = 1.dp))
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            textStyle = style,
            maxLines = 5,
            cursorBrush = SolidColor(Primary),
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    if (note.isEmpty()) Text(stringResource(Res.string.routine_notes), style = style, color = OnSurfaceVariant)
                    inner()
                }
            }
        )
    }
}

/** A day from the date picker (UTC midnight) and a time of day. */
private data class PickedMoment(val dateMillis: Long, val hour: Int, val minute: Int) {
    fun timestamp(zone: TimeZone): Long =
        CardioEntryValues.timestampOf(dateMillis, hour, minute, zone, Clock.System.now().toEpochMilliseconds())

    companion object {
        fun of(timestamp: Long, zone: TimeZone): PickedMoment {
            val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone)
            return PickedMoment(CardioEntryValues.pickerDateOf(timestamp, zone), local.hour, local.minute)
        }
    }
}

/** Shows the typed digits grouped like a clock ("3215" as 32:15); the cursor is always after the last one. */
private object DurationTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = DurationDigits.grouped(text.text)
        return TransformedText(AnnotatedString(shown), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = shown.length
            override fun transformedToOriginal(offset: Int): Int = text.length
        })
    }
}

/** Adds [suffix] after the text (nothing when it is empty); the cursor never goes into it. */
private class SuffixTransformation(private val suffix: String, private val style: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val shown = buildAnnotatedString {
            append(text)
            withStyle(style) { append(suffix) }
        }
        return TransformedText(shown, object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(text.length)
        })
    }
}

/**
 * Adding a cardio: the cardio picker (cardio only, suggestions on top), then [CardioEntrySheet]. "Change" on the sheet
 * opens the picker again over it and keeps what was typed. With [initialExercise] the sheet opens at once.
 * "Create cardio exercise" refuses a name [isNameOfHiddenNonCardio] says belongs to a hidden exercise that is not a
 * cardio: that exercise is left as it is and the form stays open with the reason.
 */
@Composable
fun CardioAddFlow(
    mode: CardioEntryMode,
    exercises: List<ExerciseEntity>,
    languageCode: String,
    loadSuggestions: suspend () -> List<SuggestedExercise>,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit,
    isNameOfHiddenNonCardio: suspend (String) -> Boolean,
    onSave: (ExerciseEntity, CardioEntry) -> Unit,
    onDismiss: () -> Unit,
    initialExercise: ExerciseEntity? = null,
    distanceUnit: String = LocalDistanceUnit.current
) {
    var exercise by remember { mutableStateOf(initialExercise) }
    var showPicker by remember { mutableStateOf(initialExercise == null) }
    val suggestions by produceState(emptyList<SuggestedExercise>()) { value = loadSuggestions() }
    val usedByDeletedStrength = stringResource(Res.string.cardio_name_used_by_deleted_strength)
    val cardio = remember(exercises, languageCode) {
        exercises
            .filter { !it.isDeleted && it.categoria.equals(CARDIO, ignoreCase = true) }
            .sortedBy { ExerciseTranslations.translate(it.nome, languageCode).lowercase() }
    }

    exercise?.let { current ->
        CardioEntrySheet(
            exercise = current,
            mode = mode,
            initial = null,
            distanceUnit = distanceUnit,
            rpeScale = null,
            languageCode = languageCode,
            onChangeExercise = { showPicker = true },
            onSave = { entry -> onSave(current, entry) },
            onDismiss = onDismiss
        )
    }

    if (showPicker) {
        ExercisePickerBottomSheet(
            exercises = cardio,
            categories = listOf(CARDIO),
            onExerciseSelected = {
                showPicker = false
                // Never anything but a cardio: "create" refuses the name of a hidden strength exercise (refuseNewExercise)
                if (it.categoria.equals(CARDIO, ignoreCase = true)) exercise = it
                else if (exercise == null) onDismiss()
            },
            onAddCustomExercise = onAddCustomExercise,
            // After a pick the picker dismisses itself too: by then it is already closed here, so nothing runs twice
            onDismiss = {
                if (showPicker) {
                    showPicker = false
                    if (exercise == null) onDismiss()
                }
            },
            languageCode = languageCode,
            lockedCategory = CARDIO,
            suggested = suggestions,
            refuseNewExercise = { name -> usedByDeletedStrength.takeIf { isNameOfHiddenNonCardio(name) } }
        )
    }
}

private const val CARDIO = "Cardio"
