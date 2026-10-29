package com.whatever.caro.core.ads

import androidx.compose.runtime.Composable

@Composable
expect fun ConfigureAdHost(
    allowConsent: Boolean,
    onAvailabilityChanged: (available: Boolean, formShowing: Boolean) -> Unit,
)
