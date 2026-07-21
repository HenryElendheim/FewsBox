package com.elendheim.fewsbox.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay

/** Everything the win screen needs about one hero's payday. */
data class HeroResult(
    val heroId: String,
    val name: String,
    val iconId: String,
    val xpGained: Int,
    val xpBefore: Int,
    val xpAfter: Int,
    val levelBefore: Int,
    val levelAfter: Int
)

/**
 * The victory screen, staged like a payoff: stars pop in one at a time,
 * then each hero's XP bar physically fills with what they earned. XP is
 * damage dealt plus a survivor's bonus, so the bars mean something.
 */
@Composable
fun ResultsScreen(
    stars: Int,
    heroResults: List<HeroResult>,
    unlockedHeroName: String?,
    onContinue: () -> Unit,
    fewsPayouts: List<Int> = emptyList(),
    fewsTotal: Int = 0
) {
    // 0 = nothing shown yet; counts up as stars land, then the fews chips
    // show their amounts and collapse into one total, then rows appear.
    // Pressing CONTINUE mid-reveal fast-forwards everything to done.
    var starsShown by remember { mutableIntStateOf(0) }
    var fewsShown by remember { mutableIntStateOf(0) }
    var fewsMerged by remember { mutableStateOf(false) }
    var rowsVisible by remember { mutableStateOf(false) }
    var revealDone by remember { mutableStateOf(false) }
    var skipped by remember { mutableStateOf(false) }
    val showStars = stars > 0

    LaunchedEffect(skipped) {
        if (skipped) {
            starsShown = stars
            fewsShown = fewsPayouts.size
            fewsMerged = true
            rowsVisible = true
            revealDone = true
            return@LaunchedEffect
        }
        delay(200)
        if (showStars) {
            repeat(stars) {
                delay(240)
                starsShown++
            }
        }
        repeat(fewsPayouts.size) {
            delay(200)
            fewsShown++
        }
        if (fewsPayouts.isNotEmpty()) {
            delay(350)
            fewsMerged = true
        }
        delay(200)
        rowsVisible = true
        delay(800)
        revealDone = true
    }

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

            if (showStars) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) { i ->
                        StarPop(
                            size = if (i == 1) 44.dp else 36.dp,
                            earned = i < stars,
                            landed = i < starsShown
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(10.dp))
            }

            // The payday: each source shows its cut, then they fuse into one
            // number in the center. The last beat is the satisfying one.
            if (fewsTotal > 0) {
                Box(Modifier.height(34.dp), contentAlignment = Alignment.Center) {
                    if (!fewsMerged) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fewsPayouts.forEachIndexed { i, amount ->
                                if (i < fewsShown) {
                                    Text(
                                        "+$amount",
                                        color = EnergyGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    } else {
                        val pop = remember { Animatable(1.6f) }
                        LaunchedEffect(Unit) {
                            pop.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }
                        Text(
                            "+$fewsTotal FEWS",
                            color = EnergyGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            for (result in heroResults) {
                HeroResultRow(result, animate = rowsVisible, instant = skipped)
                Spacer(Modifier.height(10.dp))
            }

            if (unlockedHeroName != null && rowsVisible) {
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
                onClick = { if (revealDone) onContinue() else skipped = true },
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("CONTINUE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp)
            }
        }
    }
}

/** One star slamming into place: overshoots big, settles, stays gold. */
@Composable
private fun StarPop(size: androidx.compose.ui.unit.Dp, earned: Boolean, landed: Boolean) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(landed) {
        if (landed) {
            scale.snapTo(1.7f)
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val s = if (landed) scale.value else 1f
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(if (landed && earned) EnergyGold else PanelRaised)
    )
}

@Composable
private fun HeroResultRow(result: HeroResult, animate: Boolean, instant: Boolean = false) {
    // The row's XP counts up from where the hero started to where they
    // ended, and the bar fills along with it — across level-ups too.
    val shownXp = remember { Animatable(result.xpBefore.toFloat()) }
    LaunchedEffect(animate, instant) {
        if (instant) {
            shownXp.snapTo(result.xpAfter.toFloat())
        } else if (animate && result.xpAfter > result.xpBefore) {
            shownXp.animateTo(
                result.xpAfter.toFloat(),
                tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
    }
    val xpNow = shownXp.value.toInt()
    val levelNow = Progression.levelFor(xpNow)
    val leveled = levelNow > result.levelBefore

    val rowAlpha = if (animate) 1f else 0.25f
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
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
                        "LEVEL UP  ${result.levelBefore} > $levelNow",
                        color = EnergyGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        "LV $levelNow",
                        color = HpGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            XpBar(xpNow)
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
    val fraction = Progression.levelProgress(xp)
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
