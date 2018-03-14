@file:JvmName("LengthExtensions")

package io.rover.rover.experiences.ui.layout

import io.rover.rover.core.data.domain.Length
import io.rover.rover.core.data.domain.UnitOfMeasure

/**
 * Returns the amount the Length's absolute value, taking into
 * account the given [against] denominator if the Length is a proportional one (percentage).
 *
 * The main goal of this function is to abstract away [Length]'s absolute
 * and proportional modalities.
 */
fun Length.measuredAgainst(against: Double): Double = when (unit) {
    UnitOfMeasure.Percentage -> value * against
    UnitOfMeasure.Points -> value
}

/**
 * Returns the amount the Length's absolute value, taking into
 * account the given [against] denominator if the Length is a proportional one (percentage).
 *
 * The goal of this function is to abstract away [Length]'s absolute
 * and proportional modalities.
 */
fun Length.measuredAgainst(against: Float): Float = when (unit) {
    UnitOfMeasure.Percentage -> value.toFloat() * against
    UnitOfMeasure.Points -> value.toFloat()
}
