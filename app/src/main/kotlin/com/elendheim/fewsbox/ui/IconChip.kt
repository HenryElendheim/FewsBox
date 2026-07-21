package com.elendheim.fewsbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.ui.theme.PanelRaised

/**
 * The unit/ability visual: real art when GameArt has it, otherwise the
 * glyph-on-a-chip placeholder. Same iconId in either way.
 */
@Composable
fun IconChip(iconId: String, size: Int, modifier: Modifier = Modifier) {
    val art = GameArt[iconId]
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size / 4))
            .background(PanelRaised),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(
                painter = painterResource(art),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding((size / 12).dp)
            )
        } else {
            val spec = GameIcons[iconId]
            Text(
                text = spec.glyph,
                color = spec.tint,
                fontSize = (size * 0.34).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
