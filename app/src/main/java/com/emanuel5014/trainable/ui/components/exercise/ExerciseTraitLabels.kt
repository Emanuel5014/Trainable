package com.emanuel5014.trainable.ui.components

import androidx.annotation.StringRes
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.exercise.Equipment
import com.emanuel5014.trainable.domain.exercise.Muscle

/** The string that names a muscle. The `when` has no `else`: a new muscle does not compile until it has a label. */
@StringRes
fun Muscle.labelRes(): Int = when (this) {
    Muscle.UPPER_CHEST -> R.string.muscle_upper_chest
    Muscle.MIDDLE_CHEST -> R.string.muscle_middle_chest
    Muscle.LOWER_CHEST -> R.string.muscle_lower_chest
    Muscle.LATS -> R.string.muscle_lats
    Muscle.TRAPS -> R.string.muscle_traps
    Muscle.UPPER_BACK -> R.string.muscle_upper_back
    Muscle.LOWER_BACK -> R.string.muscle_lower_back
    Muscle.FRONT_DELTS -> R.string.muscle_front_delts
    Muscle.SIDE_DELTS -> R.string.muscle_side_delts
    Muscle.REAR_DELTS -> R.string.muscle_rear_delts
    Muscle.BICEPS -> R.string.muscle_biceps
    Muscle.TRICEPS -> R.string.muscle_triceps
    Muscle.FOREARMS -> R.string.muscle_forearms
    Muscle.ABS -> R.string.muscle_abs
    Muscle.OBLIQUES -> R.string.muscle_obliques
    Muscle.QUADS -> R.string.muscle_quads
    Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
    Muscle.GLUTES -> R.string.muscle_glutes
    Muscle.CALVES -> R.string.muscle_calves
    Muscle.ADDUCTORS -> R.string.muscle_adductors
    Muscle.ABDUCTORS -> R.string.muscle_abductors
}

/** The string that names a piece of equipment. The `when` has no `else`: a new one does not compile until it has a label. */
@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.LANDMINE -> R.string.equipment_landmine
    Equipment.OTHER -> R.string.equipment_other
}
