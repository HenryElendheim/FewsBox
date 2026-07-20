package com.elendheim.fewsbox.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.data.Progression
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/** Everything the win screen needs about one hero's payday. */
data class HeroResult(
    val heroId: String,
    val name: String,
    val iconId: String,
    val xpGained: Int,
    val xpAfter: Int,
    val levelBefore: Int,
    val levelAfter: Int
)

/**
 * The victory screen: stars earned, XP paid out, level-ups celebrated,
 * and a note when a beaten boss defects to the roster.
 */
@Composable
fun ResultsScreen(
    stars: Int,
    heroResults: List<HeroResult>,
    unlockedHeroName: String?,
    onContinue: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "VICTORY",
                color = HpGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(18.dp))

            // The stars this run earned.
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(if (i == 1) 44.dp else 36.dp)
                            .clip(CircleShape)
                            .background(if (i < stars) EnergyGold else PanelRaised)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            for (result in heroResults) {
                HeroResultRow(result)
                Spacer(Modifier.height(10.dp))
            }

            if (unlockedHeroName != null) {
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${unlockedHeroName.uppercase()} JOINS YOUR SIDE",
                        color = TextBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "A grayscale boss, fighting for the colors now",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("CONTINUE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp)
            }
        }
    }
}

@Composable
private fun HeroResultRow(result: HeroResult) {
    val leveled = result.levelAfter > result.levelBefore
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GameIcons.heroColor(result.iconId) ?: Accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                if (leveled) {
                    Text(
                        "LEVEL UP  ${result.levelBefore} > ${result.levelAfter}",
                        color = EnergyGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        "LV ${result.levelAfter}",
                        color = HpGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            XpBar(result.xpAfter)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "+${result.xpGained} XP",
            color = EnergyGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/** Progress toward the next hero level, full gold at cap. */
@Composable
private fun XpBar(xp: Int) {
    val level = Progression.levelFor(xp)
    val fraction = if (level >= Progression.MAX_LEVEL) {
        1f
    } else {
        val floor = Progression.LEVEL_XP[level - 1]
        val ceil = Progression.LEVEL_XP[level]
        (xp - floor).toFloat() / (ceil - floor)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Ink)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(EnergyGold)
        )
    }
}
