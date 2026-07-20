package com.elendheim.fewsbox.ui.battle

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.engine.model.ActiveStatus
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Ability
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.IconChip
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.HpLow
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.ShieldBlue
import com.elendheim.fewsbox.ui.theme.TextMuted

/** One floating combat number or status label above a unit. */
data class Floaty(val key: Long, val text: String, val color: Color, val label: Boolean = false)

@Composable
fun HpBar(hp: Int, maxHp: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = hp.toFloat() / maxHp,
        animationSpec = tween(durationMillis = 400),
        label = "hp"
    )
    val barColor = if (fraction < 0.35f) HpLow else HpGreen
    Box(
        modifier = modifier
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Ink)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
        )
    }
}

@Composable
fun StatusRow(statuses: List<ActiveStatus>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (status in statuses) {
            val def = Statuses.REGISTRY[status.defId] ?: continue
            val spec = GameIcons[def.iconId]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(PanelRaised)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(spec.glyph, color = spec.tint, fontSize = 9.sp, fontWeight = FontWeight.Black)
                if (status.stacks > 1) {
                    Text(
                        "${status.stacks}",
                        color = TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

/** The elite telegraph: a ring that fills as the big attack winds up and
 *  pulses once it's ready — the next turn is going to hurt. */
@Composable
fun ChargeRing(progress: Float, ready: Boolean, modifier: Modifier = Modifier, color: Color = Accent) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "charge"
    )
    val ringColor = if (ready) {
        val pulse by rememberInfiniteTransition(label = "chargePulse").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 450), RepeatMode.Reverse),
            label = "chargePulseAlpha"
        )
        color.copy(alpha = pulse)
    } else {
        color.copy(alpha = 0.75f)
    }
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = Ink,
            startAngle = -90f, sweepAngle = 360f,
            useCenter = false, style = stroke
        )
        drawArc(
            color = ringColor,
            startAngle = -90f, sweepAngle = 360f * animated.coerceIn(0f, 1f),
            useCenter = false, style = stroke
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnitCard(
    unit: CombatUnit,
    isTargetable: Boolean,
    isActiveActor: Boolean,
    isActing: Boolean = false,   // this unit's move is playing out right now
    floaties: List<Floaty>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val actingScale by animateFloatAsState(
        targetValue = if (isActing) 1.14f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "actingScale"
    )
    val deadAlpha by animateFloatAsState(
        targetValue = if (unit.isAlive) 1f else 0.18f,
        animationSpec = tween(durationMillis = 500),
        label = "death"
    )
    val borderColor = when {
        isActing -> Color(0xFFF7F7F7)
        isTargetable -> Accent
        isActiveActor -> EnergyGold
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .graphicsLayer { scaleX = actingScale; scaleY = actingScale }
            .alpha(deadAlpha)
            .combinedClickable(enabled = unit.isAlive, onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = unit.name }
    ) {
        Box(contentAlignment = Alignment.Center) {
            unit.charge?.let {
                ChargeRing(
                    progress = it.progress,
                    ready = it.isReady,
                    modifier = Modifier.size(64.dp)
                )
            }
            val heroColor = GameIcons.heroColor(unit.iconId)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            ) {
                if (heroColor != null) {
                    // Your own units are solid color blocks.
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(heroColor)
                    )
                } else {
                    IconChip(unit.iconId, size = 52)
                }
            }
            if (unit.shield > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer { translationX = 12f; translationY = -8f }
                        .clip(RoundedCornerShape(7.dp))
                        .background(ShieldBlue)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("${unit.shield}", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            // Taunt wears its badge on the other shoulder: gold T, meaning
            // every enemy attack is coming here.
            if (unit.hasStatus(Resolver.TAUNT_STATUS_ID)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer { translationX = -12f; translationY = -8f }
                        .clip(RoundedCornerShape(7.dp))
                        .background(EnergyGold)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("T", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(
                Modifier.requiredSize(64.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingNumbers(floaties)
            }
        }
        HpBar(unit.hp, unit.maxHp, Modifier.width(60.dp).padding(top = 4.dp))
        StatusRow(
            unit.statuses.filterNot { it.defId == Resolver.TAUNT_STATUS_ID },
            Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun FloatingNumbers(floaties: List<Floaty>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentSize(unbounded = true)
    ) {
        for (floaty in floaties) {
            val progress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800),
                label = "floaty${floaty.key}"
            )
            // Big and unmissable: reading the number IS the combat feedback.
            Text(
                text = floaty.text,
                color = floaty.color,
                fontSize = if (floaty.label) 13.sp else 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = -44f * progress
                        alpha = 1f - progress * 0.8f
                        scaleX = 1f + 0.25f * (1f - progress)
                        scaleY = 1f + 0.25f * (1f - progress)
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AbilityButton(
    ability: Ability,
    selected: Boolean,
    enabled: Boolean,
    cooldownLeft: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val border = if (selected) Accent else Color.Transparent
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "abilityPress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, border, RoundedCornerShape(12.dp))
            // Long-press info works even when the button is unaffordable.
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = true,
                onClick = { if (enabled) onClick() },
                onLongClick = onLongClick
            )
            .padding(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconChip(ability.iconId, size = 46)
            if (cooldownLeft > 0) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xAA14141A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$cooldownLeft", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** The party's shared ultimate meter: one gold bar, filled by everyone's
 *  damage dealt and taken. Full means someone gets to go big. */
@Composable
fun UltMeterBar(percent: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "partyUlt"
    )
    val full = percent >= 100
    val barColor = if (full) {
        val pulse by rememberInfiniteTransition(label = "ultPulse").animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 450), RepeatMode.Reverse),
            label = "ultPulseAlpha"
        )
        EnergyGold.copy(alpha = pulse)
    } else {
        EnergyGold
    }
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(PanelRaised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(barColor)
        )
    }
}
