package com.emanuel5014.trainable.data

object ExerciseTranslations {
    val it = mapOf(
        // Chest
        "Flat Bench Press" to "Panca Piana",
        "45-Degree Incline Dumbbell Press" to "Panca Inclinata 45° con Manubri",
        "Floor Flyes" to "Croci a terra",
        "Incline Multipower/Smith Machine Press" to "Panca Inclinata Multipower/Smith",
        "Flat Bench Flyes" to "Croci panca",
        "Low Cable Flyes" to "Croci al cavo basso",
        "Pec Deck Fly" to "Croci alla Pec Deck",
        
        // Back
        "Pull-ups" to "Trazioni",
        "Reverse Grip Seated Cable Row" to "Pulley inversa",
        "Machine Row" to "Rowing",
        "Dumbbell Pullover" to "Pullover con Manubrio",
        "Cable Pullover" to "Pullover con Cavo",
        "Max Stretch Pullover" to "Pullover Stretch Max",
        "Back Extensions" to "Iperestensioni",
        "Reverse Hyperextensions" to "Iperestensioni Inverse",
        "Seal Row" to "Rematore Seal",
        "Reverse Grip Lat Pulldown" to "Lat Inversa",
        
        // Legs
        "Barbell Squat" to "Squat con Bilanciere",
        "Traditional Deadlift" to "Stacco Regular",
        "Vertical Leg Press" to "Leg Press Verticale",
        "Leg Press" to "Leg Press",
        "Leg Extension" to "Leg extension",
        "Walking Lunges" to "Affondi Camminati",
        "Leg Curl" to "Leg Curl",
        "Single-Leg Curl" to "Leg Curl Singolo",
        "Belt Squat" to "Belt Squat",
        "Romanian Deadlift (RDL)" to "Stacco Rumeno",
        "Sissy Squat" to "Sissy Squat",
        "Calf Raises" to "Polpacci",
        "Pendulum Squat" to "Squat Pendulum",
        
        // Shoulders
        "Military Press" to "Military Press",
        "Seated/Standing Lateral Raises" to "Alzate Laterali Seduto/In Piedi",
        "Upright Rows" to "Alzate al mento",
        "Face Pulls" to "Face Pulls",
        
        // Arms
        "Barbell Bicep Curl" to "Curl Bicipiti con Bilanciere",
        "45-Degree Incline Dumbbell Curl" to "Curl Inclinata 45° con Manubri",
        "Incline Bench Dumbbell Curl" to "Curl su Panca Inclinata",
        "Single-Arm Cable Curl" to "Curl Cavo Mono",
        "Dips" to "Dips",
        "Close-Grip Bench Press" to "Panca Piana Presa Stretta",
        "Cable Triceps Extensions" to "Estensioni Tricipiti con Cavo",
        "Triceps Rope Pushdown" to "Pushdown Corda Tricipiti",
        
        // Core
        "Crunch" to "Crunch"
    )

    fun translate(name: String, targetLang: String): String {
        return when (targetLang) {
            "it" -> it[name] ?: name
            else -> name
        }
    }
}