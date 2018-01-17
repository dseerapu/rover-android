package io.rover.rover.ui.types

import io.rover.rover.core.domain.Color

fun Color.asAndroidColor(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        red,
        green,
        blue
    )
}