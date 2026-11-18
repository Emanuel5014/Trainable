package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.domain.exercise.Equipment
import com.emanuel5014.trainable.domain.exercise.Muscle
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.equipment_barbell
import com.emanuel5014.trainable.sharedui.resources.equipment_bodyweight
import com.emanuel5014.trainable.sharedui.resources.equipment_cable
import com.emanuel5014.trainable.sharedui.resources.equipment_dumbbell
import com.emanuel5014.trainable.sharedui.resources.equipment_landmine
import com.emanuel5014.trainable.sharedui.resources.equipment_machine
import com.emanuel5014.trainable.sharedui.resources.equipment_other
import com.emanuel5014.trainable.sharedui.resources.equipment_smith_machine
import com.emanuel5014.trainable.sharedui.resources.muscle_abductors
import com.emanuel5014.trainable.sharedui.resources.muscle_abs
import com.emanuel5014.trainable.sharedui.resources.muscle_adductors
import com.emanuel5014.trainable.sharedui.resources.muscle_biceps
import com.emanuel5014.trainable.sharedui.resources.muscle_calves
import com.emanuel5014.trainable.sharedui.resources.muscle_forearms
import com.emanuel5014.trainable.sharedui.resources.muscle_front_delts
import com.emanuel5014.trainable.sharedui.resources.muscle_glutes
import com.emanuel5014.trainable.sharedui.resources.muscle_hamstrings
import com.emanuel5014.trainable.sharedui.resources.muscle_lats
import com.emanuel5014.trainable.sharedui.resources.muscle_lower_back
import com.emanuel5014.trainable.sharedui.resources.muscle_lower_chest
import com.emanuel5014.trainable.sharedui.resources.muscle_middle_chest
import com.emanuel5014.trainable.sharedui.resources.muscle_obliques
import com.emanuel5014.trainable.sharedui.resources.muscle_quads
import com.emanuel5014.trainable.sharedui.resources.muscle_rear_delts
import com.emanuel5014.trainable.sharedui.resources.muscle_side_delts
import com.emanuel5014.trainable.sharedui.resources.muscle_traps
import com.emanuel5014.trainable.sharedui.resources.muscle_triceps
import com.emanuel5014.trainable.sharedui.resources.muscle_upper_back
import com.emanuel5014.trainable.sharedui.resources.muscle_upper_chest
import org.jetbrains.compose.resources.StringResource

/** The string that names a muscle. The `when` has no `else`: a new muscle does not compile until it has a label. */
fun Muscle.labelText(): StringResource = when (this) {
    Muscle.UPPER_CHEST -> Res.string.muscle_upper_chest
    Muscle.MIDDLE_CHEST -> Res.string.muscle_middle_chest
    Muscle.LOWER_CHEST -> Res.string.muscle_lower_chest
    Muscle.LATS -> Res.string.muscle_lats
    Muscle.TRAPS -> Res.string.muscle_traps
    Muscle.UPPER_BACK -> Res.string.muscle_upper_back
    Muscle.LOWER_BACK -> Res.string.muscle_lower_back
    Muscle.FRONT_DELTS -> Res.string.muscle_front_delts
    Muscle.SIDE_DELTS -> Res.string.muscle_side_delts
    Muscle.REAR_DELTS -> Res.string.muscle_rear_delts
    Muscle.BICEPS -> Res.string.muscle_biceps
    Muscle.TRICEPS -> Res.string.muscle_triceps
    Muscle.FOREARMS -> Res.string.muscle_forearms
    Muscle.ABS -> Res.string.muscle_abs
    Muscle.OBLIQUES -> Res.string.muscle_obliques
    Muscle.QUADS -> Res.string.muscle_quads
    Muscle.HAMSTRINGS -> Res.string.muscle_hamstrings
    Muscle.GLUTES -> Res.string.muscle_glutes
    Muscle.CALVES -> Res.string.muscle_calves
    Muscle.ADDUCTORS -> Res.string.muscle_adductors
    Muscle.ABDUCTORS -> Res.string.muscle_abductors
}

/** The string that names a piece of equipment. The `when` has no `else`: a new one does not compile until it has a label. */
fun Equipment.labelText(): StringResource = when (this) {
    Equipment.BARBELL -> Res.string.equipment_barbell
    Equipment.DUMBBELL -> Res.string.equipment_dumbbell
    Equipment.CABLE -> Res.string.equipment_cable
    Equipment.MACHINE -> Res.string.equipment_machine
    Equipment.SMITH_MACHINE -> Res.string.equipment_smith_machine
    Equipment.BODYWEIGHT -> Res.string.equipment_bodyweight
    Equipment.LANDMINE -> Res.string.equipment_landmine
    Equipment.OTHER -> Res.string.equipment_other
}
