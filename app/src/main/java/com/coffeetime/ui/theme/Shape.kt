package com.coffeetime.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CoffeeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // radius-xs
    small = RoundedCornerShape(8.dp),   // radius-sm
    medium = RoundedCornerShape(12.dp),   // radius-md
    large = RoundedCornerShape(16.dp),   // radius-lg
    extraLarge = RoundedCornerShape(28.dp),   // radius-xl
)
// Botones y chips de filtro: CircleShape (radius-full).
