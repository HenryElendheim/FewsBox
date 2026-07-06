package com.elendheim.fewsbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.ui.theme.PanelRaised

/**
 * The placeholder unit/ability visual: a glyph on a tinted chip. Swapping
 * this for real art later touches nothing else — same iconId in, art out.
 */
@Composable
fun IconChip(iconId: String, size: Int, modifier: Modifier = Modifier) {
    val spec = GameIcons[iconId]
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size / 4))
            .background(PanelRaised),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = spec.glyph,
            color = spec.tint,
            fontSize = (size * 0.34).sp,
            fontWeight = FontWeight.Black
        )
    }
}
