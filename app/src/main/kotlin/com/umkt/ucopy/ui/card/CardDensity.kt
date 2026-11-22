package com.umkt.ucopy.ui.card

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.ui.common.ViewDensity

internal data class DensitySpec(
    val outerHorizontal: Dp,
    val outerVertical: Dp,
    val inner: Dp,
    val previewLines: Int,
    val titleLines: Int,
    val compact: Boolean
)

internal fun densitySpec(
    density: ViewDensity,
    baseOuterHorizontal: Dp,
    baseOuterVertical: Dp,
    baseInner: Dp,
    baseLines: Int
): DensitySpec = when (density) {
    ViewDensity.COMPACT -> DensitySpec(baseOuterHorizontal * 0.6f, baseOuterVertical * 0.5f, baseInner * 0.65f, 1, 1, true)
    ViewDensity.GRID -> DensitySpec(minOf(baseOuterHorizontal, 12.dp), minOf(baseOuterVertical, 8.dp), minOf(baseInner, 12.dp), 2, 2, true)
    ViewDensity.DETAILED -> DensitySpec(baseOuterHorizontal, baseOuterVertical * 1.25f, baseInner * 1.3f, baseLines * 2 + 2, 3, false)
    ViewDensity.COMFORTABLE -> DensitySpec(baseOuterHorizontal, baseOuterVertical, baseInner, baseLines, 2, false)
}
