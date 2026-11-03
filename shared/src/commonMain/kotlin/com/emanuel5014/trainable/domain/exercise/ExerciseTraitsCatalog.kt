package com.emanuel5014.trainable.domain.exercise

/** The muscles and equipment the app ships for each preset, by its id. Custom exercises have no entry. */
object ExerciseTraitsCatalog {

    val entries: Map<Int, ExerciseTraits> = mapOf(
        // Chest
        1 to traits(Equipment.BARBELL, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Flat Bench Press
        2 to traits(Equipment.DUMBBELL, Muscle.UPPER_CHEST, Muscle.TRICEPS),  // 45-Degree Incline Dumbbell Press
        3 to traits(Equipment.DUMBBELL, Muscle.MIDDLE_CHEST),  // Floor Flyes
        4 to traits(Equipment.SMITH_MACHINE, Muscle.UPPER_CHEST, Muscle.TRICEPS),  // Incline Multipower/Smith Machine Press
        5 to traits(Equipment.DUMBBELL, Muscle.MIDDLE_CHEST),  // Flat Bench Flyes
        6 to traits(Equipment.CABLE, Muscle.UPPER_CHEST),  // Low Cable Flyes
        7 to traits(Equipment.MACHINE, Muscle.MIDDLE_CHEST),  // Pec Deck Fly
        56 to traits(Equipment.MACHINE, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Chest Press Machine
        57 to traits(Equipment.CABLE, Muscle.LOWER_CHEST),  // Cable Crossover
        64 to traits(Equipment.MACHINE, Muscle.UPPER_CHEST),  // Incline Pec Deck Fly
        76 to traits(Equipment.BARBELL, Muscle.LOWER_CHEST, Muscle.TRICEPS),  // Decline Bench Press
        91 to traits(Equipment.DUMBBELL, Muscle.MIDDLE_CHEST, Muscle.LOWER_CHEST, Muscle.LATS),  // Dumbbell Pullover (Chest Focus)
        92 to traits(Equipment.BODYWEIGHT, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Weighted Push-ups
        117 to traits(Equipment.DUMBBELL, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Dumbbell Bench Press
        118 to traits(Equipment.DUMBBELL, Muscle.UPPER_CHEST, Muscle.TRICEPS),  // Incline Dumbbell Bench Press
        119 to traits(Equipment.DUMBBELL, Muscle.LOWER_CHEST, Muscle.TRICEPS),  // Decline Dumbbell Press
        120 to traits(Equipment.MACHINE, Muscle.MIDDLE_CHEST),  // Machine Chest Fly
        121 to traits(Equipment.BARBELL, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Floor Press
        122 to traits(Equipment.OTHER, Muscle.MIDDLE_CHEST),  // Svend Press
        123 to traits(Equipment.LANDMINE, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Landmine Chest Press
        124 to traits(Equipment.BODYWEIGHT, Muscle.MIDDLE_CHEST, Muscle.TRICEPS),  // Wide Grip Push-ups
        140 to traits(Equipment.BARBELL, Muscle.MIDDLE_CHEST),  // Guillotine Press
        143 to traits(Equipment.MACHINE, Muscle.MIDDLE_CHEST),  // Multipurpose
        // Back
        8 to traits(Equipment.BODYWEIGHT, Muscle.LATS, Muscle.BICEPS),  // Pull-ups
        9 to traits(Equipment.CABLE, Muscle.LATS, Muscle.UPPER_BACK),  // Reverse Grip Seated Cable Row
        10 to traits(Equipment.MACHINE, Muscle.UPPER_BACK, Muscle.LATS),  // Machine Row
        11 to traits(Equipment.DUMBBELL, Muscle.LATS, Muscle.MIDDLE_CHEST),  // Dumbbell Pullover
        12 to traits(Equipment.CABLE, Muscle.LATS),  // Cable Pullover
        13 to traits(Equipment.DUMBBELL, Muscle.LATS),  // Max Stretch Pullover
        14 to traits(Equipment.BODYWEIGHT, Muscle.LOWER_BACK, Muscle.GLUTES),  // Back Extensions
        15 to traits(Equipment.MACHINE, Muscle.GLUTES, Muscle.LOWER_BACK),  // Reverse Hyperextensions
        16 to traits(Equipment.BARBELL, Muscle.LATS, Muscle.UPPER_BACK),  // Seal Row
        17 to traits(Equipment.CABLE, Muscle.LATS, Muscle.BICEPS),  // Reverse Grip Lat Pulldown
        54 to traits(Equipment.LANDMINE, Muscle.UPPER_BACK, Muscle.LATS),  // T-Bar Row
        55 to traits(Equipment.CABLE, Muscle.LATS),  // Lat Pulldown (Wide Grip)
        71 to traits(Equipment.CABLE, Muscle.LATS),  // Straight Arm Pulldown
        93 to traits(Equipment.DUMBBELL, Muscle.LATS, Muscle.UPPER_BACK),  // Single-Arm Dumbbell Row
        94 to traits(Equipment.LANDMINE, Muscle.LATS, Muscle.UPPER_BACK),  // Meadows Row
        125 to traits(Equipment.BARBELL, Muscle.UPPER_BACK, Muscle.LATS),  // Pendlay Row
        126 to traits(Equipment.DUMBBELL, Muscle.UPPER_BACK, Muscle.LATS),  // Chest Supported Row
        127 to traits(Equipment.BARBELL, Muscle.LOWER_BACK, Muscle.TRAPS, Muscle.GLUTES),  // Rack Pulls
        128 to traits(Equipment.BARBELL, Muscle.UPPER_BACK, Muscle.LATS),  // Yates Row
        129 to traits(Equipment.BODYWEIGHT, Muscle.UPPER_BACK, Muscle.LATS),  // Inverted Row
        130 to traits(Equipment.BODYWEIGHT, Muscle.LATS, Muscle.BICEPS),  // Neutral Grip Pull-ups
        138 to traits(Equipment.BARBELL, Muscle.HAMSTRINGS, Muscle.LOWER_BACK, Muscle.GLUTES),  // Good Mornings
        // Arms
        35 to traits(Equipment.BARBELL, Muscle.BICEPS),  // Barbell Bicep Curl
        36 to traits(Equipment.DUMBBELL, Muscle.BICEPS),  // 45-Degree Incline Dumbbell Curl
        37 to traits(Equipment.DUMBBELL, Muscle.BICEPS),  // Incline Bench Dumbbell Curl
        38 to traits(Equipment.CABLE, Muscle.BICEPS),  // Single-Arm Cable Curl
        39 to traits(Equipment.BODYWEIGHT, Muscle.TRICEPS, Muscle.MIDDLE_CHEST),  // Dips
        40 to traits(Equipment.BARBELL, Muscle.TRICEPS, Muscle.MIDDLE_CHEST),  // Close-Grip Bench Press
        41 to traits(Equipment.CABLE, Muscle.TRICEPS),  // Cable Triceps Extensions
        42 to traits(Equipment.CABLE, Muscle.TRICEPS),  // Triceps Rope Pushdown
        49 to traits(Equipment.DUMBBELL, Muscle.BICEPS, Muscle.FOREARMS),  // Hammer Curls
        50 to traits(Equipment.BARBELL, Muscle.TRICEPS),  // Skull Crushers
        51 to traits(Equipment.BARBELL, Muscle.BICEPS),  // Preacher Curls
        80 to traits(Equipment.DUMBBELL, Muscle.BICEPS),  // Concentration Curl
        95 to traits(Equipment.DUMBBELL, Muscle.BICEPS, Muscle.FOREARMS),  // Zottman Curl
        96 to traits(Equipment.MACHINE, Muscle.TRICEPS),  // Triceps Dip Machine
        131 to traits(Equipment.CABLE, Muscle.TRICEPS),  // Overhead Cable Triceps Extension
        132 to traits(Equipment.DUMBBELL, Muscle.BICEPS),  // Spider Curl
        133 to traits(Equipment.BODYWEIGHT, Muscle.TRICEPS, Muscle.MIDDLE_CHEST),  // Diamond Push-ups
        134 to traits(Equipment.BARBELL, Muscle.BICEPS),  // EZ-Bar Curl
        135 to traits(Equipment.BARBELL, Muscle.FOREARMS, Muscle.BICEPS),  // Reverse Barbell Curl
        136 to traits(Equipment.BARBELL, Muscle.TRICEPS),  // JM Press
        // Core
        43 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Crunch
        44 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Reverse Crunch
        45 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Plank
        46 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Hanging Leg Raises
        77 to traits(Equipment.CABLE, Muscle.OBLIQUES, Muscle.ABS),  // Woodchopper
        81 to traits(Equipment.BODYWEIGHT, Muscle.OBLIQUES, Muscle.ABS),  // Russian Twist
        82 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Leg Raise on Parallel Bars
        83 to traits(Equipment.CABLE, Muscle.ABS),  // Cable Crunch
        84 to traits(Equipment.OTHER, Muscle.ABS),  // Ab Wheel Rollout
        85 to traits(Equipment.BODYWEIGHT, Muscle.ABS, Muscle.LOWER_BACK, Muscle.GLUTES),  // Bird Dog
        86 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Dead Bug
        99 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Mountain Climbers
        100 to traits(Equipment.BODYWEIGHT, Muscle.OBLIQUES),  // Side Plank
        101 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Toes to Bar
        102 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // L-Sit
        103 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // V-Ups
        104 to traits(Equipment.BODYWEIGHT, Muscle.ABS, Muscle.OBLIQUES),  // Bicycle Crunch
        105 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Flutter Kicks
        106 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Dragon Flag
        107 to traits(Equipment.CABLE, Muscle.OBLIQUES, Muscle.ABS),  // Pallof Press
        108 to traits(Equipment.BODYWEIGHT, Muscle.ABS),  // Hollow Body Hold
        142 to traits(Equipment.OTHER, Muscle.OBLIQUES, Muscle.ABS),  // Russian Twist with Weight
        // Shoulders
        31 to traits(Equipment.BARBELL, Muscle.FRONT_DELTS, Muscle.TRICEPS),  // Military Press
        32 to traits(Equipment.DUMBBELL, Muscle.SIDE_DELTS),  // Seated/Standing Lateral Raises
        33 to traits(Equipment.BARBELL, Muscle.SIDE_DELTS, Muscle.TRAPS),  // Upright Rows
        34 to traits(Equipment.CABLE, Muscle.REAR_DELTS, Muscle.UPPER_BACK),  // Face Pulls
        47 to traits(Equipment.DUMBBELL, Muscle.FRONT_DELTS, Muscle.SIDE_DELTS),  // Arnold Press
        48 to traits(Equipment.DUMBBELL, Muscle.REAR_DELTS),  // Bent Over Lateral Raises
        73 to traits(Equipment.MACHINE, Muscle.REAR_DELTS),  // Rear Delt Fly (Machine/Dumbbell)
        75 to traits(Equipment.SMITH_MACHINE, Muscle.TRAPS),  // Smith Machine Shrugs
        87 to traits(Equipment.DUMBBELL, Muscle.FRONT_DELTS),  // Dumbbell Front Raise
        88 to traits(Equipment.OTHER, Muscle.FRONT_DELTS, Muscle.SIDE_DELTS),  // Bus Driver
        89 to traits(Equipment.LANDMINE, Muscle.FRONT_DELTS, Muscle.TRICEPS),  // Landmine Press
        90 to traits(Equipment.CABLE, Muscle.REAR_DELTS),  // Reverse Cable Flyes
        109 to traits(Equipment.DUMBBELL, Muscle.FRONT_DELTS, Muscle.TRICEPS),  // Dumbbell Shoulder Press
        110 to traits(Equipment.LANDMINE, Muscle.FRONT_DELTS, Muscle.TRICEPS),  // Single-Arm Landmine Press
        111 to traits(Equipment.BARBELL, Muscle.FRONT_DELTS, Muscle.SIDE_DELTS),  // Bradford Press
        113 to traits(Equipment.BARBELL, Muscle.TRAPS, Muscle.SIDE_DELTS),  // High Pull
        114 to traits(Equipment.DUMBBELL, Muscle.SIDE_DELTS, Muscle.FRONT_DELTS),  // Lu Raises
        115 to traits(Equipment.DUMBBELL, Muscle.REAR_DELTS, Muscle.TRAPS),  // Y-Raises
        116 to traits(Equipment.DUMBBELL, Muscle.REAR_DELTS, Muscle.SIDE_DELTS),  // Cuban Press
        141 to traits(Equipment.DUMBBELL, Muscle.SIDE_DELTS),  // Single-Arm Dumbbell Lateral Raise
        158 to traits(Equipment.DUMBBELL, Muscle.SIDE_DELTS),  // Seated Lateral Raise
        159 to traits(Equipment.DUMBBELL, Muscle.SIDE_DELTS),  // Standing Lateral Raise
        160 to traits(Equipment.MACHINE, Muscle.REAR_DELTS),  // Rear Delt Fly (Machine)
        161 to traits(Equipment.DUMBBELL, Muscle.REAR_DELTS),  // Rear Delt Fly (Dumbbell)
        // Legs
        18 to traits(Equipment.BARBELL, Muscle.QUADS, Muscle.GLUTES),  // Barbell Squat
        19 to traits(Equipment.BARBELL, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.LOWER_BACK),  // Traditional Deadlift
        20 to traits(Equipment.MACHINE, Muscle.QUADS, Muscle.GLUTES),  // Vertical Leg Press
        21 to traits(Equipment.MACHINE, Muscle.QUADS, Muscle.GLUTES),  // Leg Press
        22 to traits(Equipment.MACHINE, Muscle.QUADS),  // Leg Extension
        23 to traits(Equipment.DUMBBELL, Muscle.QUADS, Muscle.GLUTES),  // Walking Lunges
        24 to traits(Equipment.MACHINE, Muscle.HAMSTRINGS),  // Leg Curl
        25 to traits(Equipment.MACHINE, Muscle.HAMSTRINGS),  // Single-Leg Curl
        26 to traits(Equipment.MACHINE, Muscle.QUADS, Muscle.GLUTES),  // Belt Squat
        27 to traits(Equipment.BARBELL, Muscle.HAMSTRINGS, Muscle.GLUTES),  // Romanian Deadlift (RDL)
        28 to traits(Equipment.BODYWEIGHT, Muscle.QUADS),  // Sissy Squat
        29 to traits(Equipment.MACHINE, Muscle.CALVES),  // Calf Raises
        30 to traits(Equipment.MACHINE, Muscle.QUADS, Muscle.GLUTES),  // Pendulum Squat
        52 to traits(Equipment.DUMBBELL, Muscle.QUADS, Muscle.GLUTES),  // Bulgarian Split Squat
        53 to traits(Equipment.MACHINE, Muscle.QUADS, Muscle.GLUTES),  // Hack Squat
        58 to traits(Equipment.MACHINE, Muscle.CALVES),  // Seated Calf Raise
        59 to traits(Equipment.MACHINE, Muscle.CALVES),  // Donkey Calf Raise
        60 to traits(Equipment.MACHINE, Muscle.CALVES),  // Leg Press Calf Press
        61 to traits(Equipment.SMITH_MACHINE, Muscle.CALVES),  // Smith Machine Calf Raise
        62 to traits(Equipment.OTHER, Muscle.CALVES),  // Tibialis Raise
        65 to traits(Equipment.BODYWEIGHT, Muscle.HAMSTRINGS),  // Nordic Leg Curl
        74 to traits(Equipment.BARBELL, Muscle.GLUTES),  // Hip Thrust
        78 to traits(Equipment.SMITH_MACHINE, Muscle.CALVES),  // Standing Smith Machine Calf Raise
        79 to traits(Equipment.MACHINE, Muscle.CALVES),  // Seated Calf Raise Machine
        97 to traits(Equipment.DUMBBELL, Muscle.QUADS, Muscle.GLUTES),  // Step-ups
        98 to traits(Equipment.DUMBBELL, Muscle.QUADS, Muscle.GLUTES),  // Goblet Squat
        137 to traits(Equipment.CABLE, Muscle.GLUTES, Muscle.HAMSTRINGS),  // Cable Pull-Through
        162 to traits(Equipment.MACHINE, Muscle.ADDUCTORS),  // Hip Adduction Machine
        163 to traits(Equipment.MACHINE, Muscle.ABDUCTORS, Muscle.GLUTES),  // Hip Abduction Machine
        // Cardio
        150 to traits(Equipment.MACHINE),  // Treadmill
        151 to traits(Equipment.MACHINE),  // Stationary Bike
        152 to traits(Equipment.MACHINE),  // Elliptical
        153 to traits(Equipment.MACHINE),  // Stairmaster
        154 to traits(Equipment.MACHINE),  // Rowing Machine
        155 to traits(Equipment.MACHINE),  // Assault Bike
        156 to traits(Equipment.MACHINE),  // SkiErg
        157 to traits(Equipment.OTHER),  // Jump Rope
    )

    private fun traits(equipment: Equipment, vararg muscles: Muscle) =
        ExerciseTraits(muscles.sortedBy { it.ordinal }.toSet(), equipment)
}
