package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.CardioExerciseLogs
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.CardioNames
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.forRun
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.share
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.distance
import com.emanuel5014.trainable.sharedui.resources.no_exercises_in_session_detail
import com.emanuel5014.trainable.sharedui.resources.routine_archived_badge
import com.emanuel5014.trainable.sharedui.resources.set_number
import com.emanuel5014.trainable.sharedui.resources.sets
import com.emanuel5014.trainable.sharedui.resources.share_duration
import com.emanuel5014.trainable.sharedui.resources.share_volume
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.ui.components.DeletedExercisePill
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.components.LocalRpeSettings
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.ProgramEditHistoryDialog
import com.emanuel5014.trainable.ui.components.ProgramEditPill
import com.emanuel5014.trainable.ui.components.RestoreExerciseDialog
import com.emanuel5014.trainable.ui.components.SetLogBadges
import com.emanuel5014.trainable.ui.components.cardioSetValueText
import com.emanuel5014.trainable.ui.components.journal.JournalDetail
import com.emanuel5014.trainable.ui.components.setValueText
import com.emanuel5014.trainable.ui.screens.routines.routineRowCorners
import com.emanuel5014.trainable.ui.screens.routines.toShape
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.fullDate
import org.jetbrains.compose.resources.stringResource

/** One run of an exercise in a workout: the same exercise done again later is a run of its own. */
private data class ExerciseWithSets(
    val exercise: com.emanuel5014.trainable.data.local.entity.ExerciseEntity,
    val sets: MutableList<com.emanuel5014.trainable.data.local.relation.SetWithExercise>
)

/** Exercise runs shown together: one run, or the runs of one superset. */
private data class HistoryBlock(
    val supersetId: String?,
    val exercises: List<ExerciseWithSets>
)

/** A strength block or a cardio activity, in workout order. */
private sealed interface UnifiedHistoryItem {
    val order: Int
    data class Strength(val block: HistoryBlock, override val order: Int) : UnifiedHistoryItem
    data class Cardio(val cardio: CardioExerciseLogs, override val order: Int) : UnifiedHistoryItem
}

/** What an opened workout shows: its exercise runs (for [forExerciseOf]) and the items in workout order. */
private data class HistoryWorkoutItems(
    val runs: List<ExerciseWithSets>,
    val items: List<UnifiedHistoryItem>
)

/** The program edits made to one exercise of a session; an exercise done twice has the edits of each run on its own. */
private fun List<SessionPrescriptionEditEntity>.forExerciseOf(block: ExerciseWithSets, all: List<ExerciseWithSets>) = forRun(
    exerciseId = block.exercise.id,
    order = block.sets.first().setLog.ordineEsercizio,
    runOrders = all.filter { it.exercise.id == block.exercise.id }.map { it.sets.first().setLog.ordineEsercizio }
)

/** Sets → exercise runs → superset blocks → items sorted together with the cardio activities. */
private fun historyWorkoutItems(details: SessionWithDetails): HistoryWorkoutItems {
    val sortedSets = details.sets.sortedWith(
        compareBy(
            { it.setLog.ordineEsercizio },
            { it.setLog.numeroSerie }
        )
    )

    // 1. Group sets by exercise while preserving order
    val exercisesWithSets = mutableListOf<ExerciseWithSets>()
    sortedSets.forEach { setWithEx ->
        val existing = exercisesWithSets.find { it.exercise.id == setWithEx.exercise.id && it.sets.first().setLog.ordineEsercizio == setWithEx.setLog.ordineEsercizio }
        if (existing != null) {
            existing.sets.add(setWithEx)
        } else {
            exercisesWithSets.add(ExerciseWithSets(setWithEx.exercise, mutableListOf(setWithEx)))
        }
    }

    // 2. Group exercises into blocks by supersetId
    val blocks = mutableListOf<HistoryBlock>()
    var currentSupersetId: String? = null
    var currentBlock = mutableListOf<ExerciseWithSets>()

    exercisesWithSets.forEach { item ->
        val sid = item.sets.first().setLog.supersetId
        if (sid != null) {
            if (sid == currentSupersetId) {
                currentBlock.add(item)
            } else {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
                    currentBlock = mutableListOf()
                }
                currentSupersetId = sid
                currentBlock.add(item)
            }
        } else {
            if (currentBlock.isNotEmpty()) {
                blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
                currentBlock = mutableListOf()
            }
            currentSupersetId = null
            blocks.add(HistoryBlock(null, listOf(item)))
        }
    }
    if (currentBlock.isNotEmpty()) {
        blocks.add(HistoryBlock(currentSupersetId, currentBlock.toList()))
    }

    // 3. Strength blocks and cardio activities in workout order
    val unifiedItems = mutableListOf<UnifiedHistoryItem>()
    blocks.forEach { block ->
        val ord = block.exercises.firstOrNull()?.sets?.firstOrNull()?.setLog?.ordineEsercizio ?: 0
        unifiedItems.add(UnifiedHistoryItem.Strength(block, ord))
    }
    details.cardio.groupedByExercise().forEach { cardio ->
        unifiedItems.add(UnifiedHistoryItem.Cardio(cardio, cardio.order))
    }

    return HistoryWorkoutItems(runs = exercisesWithSets, items = unifiedItems.sortedBy { it.order })
}

/** The opened workout: header, numbers, journal and every exercise, in a sheet over the history list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutDetailSheet(
    sessionDetails: SessionWithDetails,
    details: SessionWithDetails?,
    planName: String,
    languageCode: String,
    exercises: Map<Int, ExerciseEntity>,
    weightUnit: String,
    showJournal: Boolean,
    showEdit: Boolean,
    onShare: (() -> Unit)?,
    onEdit: () -> Unit,
    onEditJournal: () -> Unit,
    onTagClick: (Int) -> Unit,
    onRestoreExercise: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val session = sessionDetails.session
    val summary = remember(details ?: sessionDetails) { historyRowSummary(details ?: sessionDetails) }
    val cardioDistanceText = com.emanuel5014.trainable.ui.components.distanceLocalText(summary.cardioKm)
    val workoutItems = remember(details) { details?.let { historyWorkoutItems(it) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = ResponsiveSize.horizontalPadding,
                end = ResponsiveSize.horizontalPadding,
                // The sheet already keeps its content above the navigation bar (its default content insets
                // take safeDrawing bottom and consume it), so only the 16 dp is added here
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item(key = "header") {
                WorkoutSheetHeader(
                    planName = planName,
                    timestamp = session.timestamp,
                    programWeek = session.programWeek,
                    isPlanArchived = sessionDetails.plan.isArchived,
                    showEdit = showEdit,
                    onEdit = onEdit,
                    onShare = onShare,
                    modifier = Modifier.padding(bottom = 13.dp)
                )
            }

            val tiles = buildList {
                summary.durationMs?.let { add(StatTile(formatSessionDuration(it), Res.string.share_duration)) }
                if (summary.isCardioOnly) {
                    if (summary.cardioKm > 0f) {
                        add(StatTile(cardioDistanceText, Res.string.distance))
                    }
                } else {
                    if (summary.sets > 0) add(StatTile(summary.sets.toString(), Res.string.sets))
                    if (summary.volumeKg > 0f) add(StatTile(formatVolume(summary.volumeKg, weightUnit), Res.string.share_volume))
                }
            }
            if (tiles.isNotEmpty()) {
                item(key = "stats") {
                    WorkoutStatTiles(tiles, Modifier.padding(bottom = 13.dp))
                }
            }

            if (showJournal) {
                item(key = "journal") {
                    JournalDetail(
                        rating = session.rating,
                        tags = sessionDetails.tags,
                        note = session.journalNote,
                        onTagClick = onTagClick,
                        onEdit = onEditJournal,
                        modifier = Modifier.padding(bottom = 13.dp)
                    )
                }
            }

            when {
                details == null || workoutItems == null -> item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        GymLoadingIndicator(size = 24.dp)
                    }
                }
                details.sets.isEmpty() && details.cardio.isEmpty() -> item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.no_exercises_in_session_detail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                else -> {
                    val count = workoutItems.items.size
                    itemsIndexed(workoutItems.items) { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(routineRowCorners(index, count).toShape())
                                .background(SurfaceContainerLow)
                                .padding(14.dp)
                        ) {
                            when (item) {
                                is UnifiedHistoryItem.Strength -> StrengthBlockContent(
                                    block = item.block,
                                    runs = workoutItems.runs,
                                    prescriptionEdits = details.prescriptionEdits,
                                    languageCode = languageCode,
                                    weightUnit = weightUnit,
                                    onRestoreExercise = onRestoreExercise
                                )
                                is UnifiedHistoryItem.Cardio -> HistoryCardioBlock(item.cardio, exercises, languageCode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSheetHeader(
    planName: String,
    timestamp: Long,
    programWeek: Int?,
    isPlanArchived: Boolean,
    showEdit: Boolean,
    onEdit: () -> Unit,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val advancedProgramming = LocalAdvancedProgramming.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = SessionTitle.of(planName).displayText(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (advancedProgramming && programWeek != null) {
                    PrescriptionPill(
                        text = stringResource(Res.string.week_short, programWeek),
                        containerColor = Primary.copy(alpha = 0.12f),
                        contentColor = Primary,
                        emphasized = true
                    )
                }
                if (isPlanArchived) {
                    ArchivedRoutineBadge()
                }
            }
            Text(
                text = fullDate(timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
        if (showEdit) {
            GymIconButton(
                icon = Icons.Rounded.Edit,
                onClick = onEdit,
                description = stringResource(Res.string.edit),
                containerColor = SurfaceContainerHigh,
                contentColor = Primary
            )
        }
        if (onShare != null) {
            GymIconButton(
                icon = Icons.Rounded.Share,
                onClick = onShare,
                description = stringResource(Res.string.share),
                containerColor = Primary.copy(alpha = 0.1f),
                contentColor = Primary
            )
        }
    }
}

private data class StatTile(val value: String, val label: org.jetbrains.compose.resources.StringResource)

@Composable
private fun WorkoutStatTiles(tiles: List<StatTile>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { tile ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = tile.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(tile.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A strength block in its card: one exercise, or a superset with a thin line along its exercises. */
@Composable
private fun StrengthBlockContent(
    block: HistoryBlock,
    runs: List<ExerciseWithSets>,
    prescriptionEdits: List<SessionPrescriptionEditEntity>,
    languageCode: String,
    weightUnit: String,
    onRestoreExercise: (Int) -> Unit
) {
    if (block.supersetId == null) {
        block.exercises.forEachIndexed { exIndex, exWithSets ->
            HistoryExerciseGroup(
                exWithSets = exWithSets,
                isSuperset = false,
                languageCode = languageCode,
                weightUnit = weightUnit,
                modifier = Modifier.padding(bottom = if (exIndex < block.exercises.lastIndex) 16.dp else 0.dp),
                programEdits = prescriptionEdits.forExerciseOf(exWithSets, runs),
                onRestoreExercise = onRestoreExercise
            )
        }
        return
    }
    HistorySupersetLabel(Modifier.padding(bottom = 10.dp))
    val barColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val width = 2.dp.toPx()
                drawRoundRect(
                    color = barColor,
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(width / 2, width / 2)
                )
            }
            .padding(start = 10.dp)
    ) {
        block.exercises.forEachIndexed { exIndex, exWithSets ->
            HistoryExerciseGroup(
                exWithSets = exWithSets,
                isSuperset = true,
                languageCode = languageCode,
                weightUnit = weightUnit,
                modifier = Modifier.padding(bottom = if (exIndex < block.exercises.lastIndex) 16.dp else 0.dp),
                programEdits = prescriptionEdits.forExerciseOf(exWithSets, runs),
                onRestoreExercise = onRestoreExercise
            )
        }
    }
}

/** The "SUPERSET" label with its link icon, on top of a superset block. */
@Composable
private fun HistorySupersetLabel(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            Icons.Rounded.Link,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(Res.string.superset),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Primary,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * One exercise run: its name (deleted from the list → greyed with a restore pill; changed during the workout → the
 * last program edit), then one line per set: number · value · badges (the badges go under the value only when the
 * line is too short), with the set note under it.
 */
@Composable
private fun HistoryExerciseGroup(
    exWithSets: ExerciseWithSets,
    isSuperset: Boolean,
    languageCode: String,
    weightUnit: String,
    modifier: Modifier = Modifier,
    /** Changes made to the prescription of this exercise during the workout, oldest first. */
    programEdits: List<SessionPrescriptionEditEntity> = emptyList(),
    /** Adds an exercise deleted from the exercise list back to it. */
    onRestoreExercise: (Int) -> Unit = {}
) {
    val exerciseName = ExerciseTranslations.translate(exWithSets.exercise.nome, languageCode)
    val isDeleted = exWithSets.exercise.isDeleted
    var showRestoreDialog by remember { mutableStateOf(false) }
    if (showRestoreDialog) {
        RestoreExerciseDialog(
            exerciseNames = listOf(exerciseName),
            onRestore = {
                onRestoreExercise(exWithSets.exercise.id)
                showRestoreDialog = false
            },
            onSkip = { showRestoreDialog = false },
            onDismiss = { showRestoreDialog = false },
            skipLabel = stringResource(Res.string.cancel)
        )
    }
    var showProgramEdits by remember { mutableStateOf(false) }
    if (showProgramEdits) {
        ProgramEditHistoryDialog(
            exerciseName = exerciseName,
            edits = programEdits,
            weightUnit = weightUnit,
            onDismiss = { showProgramEdits = false }
        )
    }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = exerciseName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                // Deleted from the exercise list: still in the history, greyed out
                color = if (isDeleted) OnSurfaceVariant.copy(alpha = 0.7f) else Primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isDeleted) {
                DeletedExercisePill(Modifier.clickable { showRestoreDialog = true })
            }
            programEdits.lastOrNull()?.let { edit ->
                ProgramEditPill(edit, Modifier.clickable { showProgramEdits = true })
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        exWithSets.sets.sortedBy { it.setLog.numeroSerie }.forEach { setWithEx ->
            val set = setWithEx.setLog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = set.numeroSerie.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        // On the value's first line, also when taller badges centre the value on the line
                        modifier = Modifier.width(24.dp).alignByBaseline()
                    )
                    SetValueLine(
                        modifier = Modifier.weight(1f).alignByBaseline(),
                        value = {
                            Text(
                                text = setValueText(set, weightUnit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        },
                        badges = { SetLogBadges(set = set) }
                    )
                }
                if (!set.note.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp, start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null,
                            tint = OnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = set.note.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

/** Where the parts of a set line go, in px: see [setLinePlacement]. */
data class SetLinePlacement(
    val width: Int,
    val height: Int,
    val valueY: Int,
    val badgesX: Int,
    val badgesY: Int,
    val badgesBelow: Boolean
)

/**
 * A set line: the value at its natural width on the left and the badges at the right end of the same line when
 * value + [gap] + badges fit in [maxWidth] (both centred on the line); otherwise the badges go under the value,
 * left-aligned, [wrapGap] below it. Never shrinks the value for the badges. No badges (width 0) → just the value.
 */
fun setLinePlacement(
    maxWidth: Int,
    valueWidth: Int,
    valueHeight: Int,
    badgesWidth: Int,
    badgesHeight: Int,
    gap: Int,
    wrapGap: Int
): SetLinePlacement {
    if (badgesWidth <= 0 || badgesHeight <= 0) {
        return SetLinePlacement(maxWidth, valueHeight, valueY = 0, badgesX = 0, badgesY = 0, badgesBelow = false)
    }
    return if (valueWidth + gap + badgesWidth <= maxWidth) {
        val height = maxOf(valueHeight, badgesHeight)
        SetLinePlacement(
            width = maxWidth,
            height = height,
            valueY = (height - valueHeight) / 2,
            badgesX = maxWidth - badgesWidth,
            badgesY = (height - badgesHeight) / 2,
            badgesBelow = false
        )
    } else {
        SetLinePlacement(
            width = maxWidth,
            height = valueHeight + wrapGap + badgesHeight,
            valueY = 0,
            badgesX = 0,
            badgesY = valueHeight + wrapGap,
            badgesBelow = true
        )
    }
}

/** Value and badges of one set, laid out by [setLinePlacement]; its first baseline is the value's. */
@Composable
private fun SetValueLine(
    value: @Composable () -> Unit,
    badges: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        contents = listOf(value, badges),
        modifier = modifier
    ) { (valueMeasurables, badgeMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val valuePlaceable = valueMeasurables.firstOrNull()?.measure(loose)
        // SetLogBadges emits nothing when the set has no badges
        val badgesPlaceable = badgeMeasurables.firstOrNull()?.measure(loose)
        val valueWidth = valuePlaceable?.width ?: 0
        val valueHeight = valuePlaceable?.height ?: 0
        val badgesWidth = badgesPlaceable?.width ?: 0
        val gap = 8.dp.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth
        else valueWidth + gap + badgesWidth
        val placement = setLinePlacement(
            maxWidth = maxWidth,
            valueWidth = valueWidth,
            valueHeight = valueHeight,
            badgesWidth = badgesWidth,
            badgesHeight = badgesPlaceable?.height ?: 0,
            gap = gap,
            wrapGap = 4.dp.roundToPx()
        )
        val baseline = valuePlaceable?.get(FirstBaseline)?.takeIf { it != androidx.compose.ui.layout.AlignmentLine.Unspecified }
        layout(
            width = placement.width.coerceIn(constraints.minWidth, constraints.maxWidth),
            height = placement.height.coerceIn(constraints.minHeight, constraints.maxHeight),
            alignmentLines = baseline?.let { mapOf(FirstBaseline to placement.valueY + it) } ?: emptyMap()
        ) {
            valuePlaceable?.placeRelative(0, placement.valueY)
            badgesPlaceable?.placeRelative(placement.badgesX, placement.badgesY)
        }
    }
}

/** The icon of a cardio activity: bike, walk, or run for everything else. */
internal fun cardioIcon(categoria: String): ImageVector {
    val catLower = categoria.lowercase()
    return when {
        catLower.contains("bici") || catLower.contains("bike") || catLower.contains("cycling") || catLower.contains("cyclette") -> Icons.AutoMirrored.Rounded.DirectionsBike
        catLower.contains("camminata") || catLower.contains("walk") -> Icons.AutoMirrored.Rounded.DirectionsWalk
        else -> Icons.AutoMirrored.Rounded.DirectionsRun
    }
}

/** A cardio activity: its name, then one line (distance, time) or one line per set, with the notes. */
@Composable
private fun HistoryCardioBlock(cardioLogs: CardioExerciseLogs, exercises: Map<Int, ExerciseEntity>, languageCode: String, modifier: Modifier = Modifier) {
    val cardio = cardioLogs.first
    val cardioSets = cardioLogs.sets
    val translatedTitle = CardioNames.of(cardio, exercises, languageCode)
    val rpeScale = LocalRpeSettings.current.scale
    val rpeText = LocalPrescriptionText.current
    fun rpeSuffix(rpe: Float?) = rpe?.let { " · " + rpeText.rpeBadge(it, rpeScale) }.orEmpty()
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(cardioIcon(translatedTitle), contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = translatedTitle.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (cardioSets.size > 1) {
            // Several sets read like the sets of an exercise
            cardioSets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.set_number, index + 1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = cardioSetValueText(set) + rpeSuffix(set.rpe),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
                set.note?.takeIf { it.isNotBlank() }?.let { CardioHistoryNote(it) }
            }
        } else Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = com.emanuel5014.trainable.ui.components.distanceLocalText(cardio.distanza),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = run {
                    val h = cardio.durataSecondi / 3600
                    val m = (cardio.durataSecondi % 3600) / 60
                    val s = cardio.durataSecondi % 60
                    (if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s") + rpeSuffix(cardio.rpe)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
        if (cardioSets.size == 1) cardio.note?.takeIf { it.isNotBlank() }?.let { CardioHistoryNote(it) }
    }
}

/** The note of a cardio set, under it. */
@Composable
private fun CardioHistoryNote(note: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = note, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontStyle = FontStyle.Italic)
    }
}

/** Marks a workout whose routine has been archived since. */
@Composable
internal fun ArchivedRoutineBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(SurfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Archive,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = stringResource(Res.string.routine_archived_badge).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
