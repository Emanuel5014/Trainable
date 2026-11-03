package com.emanuel5014.trainable.data.model

/** Look of the main bottom navigation, picked by the user in Settings. */
enum class NavBarStyle(val id: Int) {
    /** Standard Material 3 navigation bar. */
    Classic(0),

    /** Blurred bar floating above the content. */
    Floating(1),

    /** Material 3 Expressive floating toolbar with the screen action next to it. */
    Expressive(2);

    companion object {
        fun fromId(id: Int?): NavBarStyle? = entries.firstOrNull { it.id == id }
    }
}
