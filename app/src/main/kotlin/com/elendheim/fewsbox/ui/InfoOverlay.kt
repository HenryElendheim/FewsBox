package com.elendheim.fewsbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/**
 * The hold-to-read card. Long-pressing a unit, weapon, offhand or ability
 * fills this; tapping anywhere dismisses it.
 */
@Composable
fun InfoOverlay(content: InfoContent?, onDismiss: () -> Unit) {
    if (content == null) return
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC0A0A0E))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Panel)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                content.title,
                color = Accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                content.subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(12.dp))
            for (line in content.lines) {
                val isHeader = line.isNotEmpty() && line == line.uppercase() && !line.startsWith(" ")
                Text(
                    text = line.trim(),
                    color = if (isHeader) TextBright else TextMuted,
                    fontSize = if (isHeader) 12.sp else 13.sp,
                    fontWeight = if (isHeader) FontWeight.Black else FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = if (isHeader) 1.sp else 0.sp,
                    modifier = Modifier.padding(
                        top = if (isHeader) 10.dp else 3.dp,
                        start = if (isHeader) 0.dp else 8.dp
                    )
                )
            }
        }
    }
}
