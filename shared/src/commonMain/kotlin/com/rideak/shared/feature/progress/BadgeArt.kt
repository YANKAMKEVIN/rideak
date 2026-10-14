package com.rideak.shared.feature.progress

import com.rideak.exploration.Badge
import com.rideak.shared.resources.Res
import com.rideak.shared.resources.badge_100km
import com.rideak.shared.resources.badge_10km
import com.rideak.shared.resources.badge_5_cities
import com.rideak.shared.resources.badge_explorer
import com.rideak.shared.resources.badge_first_park
import com.rideak.shared.resources.badge_first_ride
import org.jetbrains.compose.resources.DrawableResource

/** Illustration of each badge (design/generated, prompts in docs/assets/badges.prompt.txt). */
val Badge.art: DrawableResource
    get() = when (this) {
        Badge.FIRST_RIDE -> Res.drawable.badge_first_ride
        Badge.TEN_KM -> Res.drawable.badge_10km
        Badge.FIRST_PARK -> Res.drawable.badge_first_park
        Badge.FIVE_CITIES -> Res.drawable.badge_5_cities
        Badge.HUNDRED_KM -> Res.drawable.badge_100km
        Badge.EXPLORER -> Res.drawable.badge_explorer
    }
