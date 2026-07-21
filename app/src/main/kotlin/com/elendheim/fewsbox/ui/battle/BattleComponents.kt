package com.elendheim.fewsbox.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.engine.model.ActiveStatus
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Resolver
import com.elendheim.fewsbox.ui.GameArt
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.IconChip
import com.elendheim.fewsbox.ui.Prefs
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.HpLow
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.ShieldBlue
import com.elendheim.fewsbox.ui.theme.TextMuted
import kotlinx.coroutines.launch

/** One floating combat number or status label above a unit. */
data class Floaty(val key: Long, val text: String, val color: Color, val label: Boolean = false)

/** A one-shot reaction on a unit card: recoil on a hit, glow on help. */
enum class FlashKind { HIT, HEAL, SHIELD }
data class UnitFlash(val key: Long, val kind: FlashKind)

/** A weapon or offhand's signature strike animation, drawn over the target. */
enum class ImpactStyle { SLASH, SMASH, FIRE, MULTI, MAGIC, WAVE, BUFF }
data class ImpactFx(val key: Long, val style: ImpactStyle, val color: Color)

private val RedC = Color(0xFFE5484D)
private val OrangeC = Color(0xFFE8823D)
private val GoldC = Color(0xFFFFD166)
private val GreenC = Color(0xFF6FCF97)
private val BlueC = Color(0xFF4AA3FF)
private val VioletC = Color(0xFFB68CFF)
private val GrayC = Color(0xFFC9C9C9)

/** Every ability id maps to a strike look; offhands flare in their color. */
fun impactStyleFor(abilityId: String): Pair<ImpactStyle, Color>? = when (abilityId) {
    "atk_red_sword", "atk_red_axe" -> ImpactStyle.SLASH to RedC
    "atk_red_maul" -> ImpactStyle.SMASH to RedC
    "atk_red_cleaver" -> ImpactStyle.MULTI to RedC
    "atk_red_pulse" -> ImpactStyle.SMASH to GoldC
    "atk_orange_ember", "atk_orange_fan", "atk_orange_blaze" -> ImpactStyle.FIRE to OrangeC
    "atk_orange_torches", "atk_orange_whip" -> ImpactStyle.MULTI to OrangeC
    "atk_yellow_siphon" -> ImpactStyle.WAVE to GoldC
    "atk_yellow_bell" -> ImpactStyle.BUFF to GoldC
    "atk_yellow_lance", "atk_yellow_lifeline" -> ImpactStyle.SLASH to GoldC
    "atk_yellow_karma" -> ImpactStyle.SMASH to GoldC
    "atk_green_fan", "atk_green_scythe" -> ImpactStyle.SLASH to GreenC
    "atk_green_volley" -> ImpactStyle.MULTI to GreenC
    "atk_green_tangle" -> ImpactStyle.WAVE to GreenC
    "atk_green_blast" -> ImpactStyle.SMASH to GreenC
    "atk_blue_hammer", "atk_blue_anchor", "atk_blue_breakwater" -> ImpactStyle.SMASH to BlueC
    "atk_blue_pike" -> ImpactStyle.SLASH to BlueC
    "atk_blue_undertow" -> ImpactStyle.WAVE to BlueC
    "atk_violet_needle", "atk_violet_reaper" -> ImpactStyle.SLASH to VioletC
    "atk_violet_fang" -> ImpactStyle.MULTI to VioletC
    "atk_violet_gravebind", "atk_violet_nightfall" -> ImpactStyle.MAGIC to VioletC
    "atk_ash_cinder", "atk_ash_smoke", "ash_cloud", "cinder_spit" -> ImpactStyle.FIRE to GrayC
    "atk_ash_veil", "basic_slash" -> ImpactStyle.SLASH to GrayC
    "atk_silver_edge", "atk_silver_lash" -> ImpactStyle.MULTI to Color(0xFFF7F7F7)
    "atk_silver_spike", "doom_bolt", "null_wave" -> ImpactStyle.MAGIC to GrayC
    "venom_spit" -> ImpactStyle.MULTI to GreenC
    "crushing_blow" -> ImpactStyle.SMASH to Color(0xFFEDEDED)
    "silver_storm" -> ImpactStyle.WAVE to Color(0xFFF7F7F7)
    "ult_red" -> ImpactStyle.SMASH to RedC
    "ult_orange", "ult_ash" -> ImpactStyle.FIRE to OrangeC
    "ult_yellow" -> ImpactStyle.BUFF to GoldC
    "ult_green" -> ImpactStyle.BUFF to GreenC
    "ult_blue" -> ImpactStyle.BUFF to BlueC
    "ult_violet" -> ImpactStyle.MAGIC to VioletC
    "ult_silver" -> ImpactStyle.WAVE to Color(0xFFF7F7F7)
    else -> when {
        // Offhands flare in their owner color: def_red_*, def_blue_*...
        abilityId.startsWith("def_red_") -> ImpactStyle.BUFF to RedC
        abilityId.startsWith("def_orange_") -> ImpactStyle.BUFF to OrangeC
        abilityId.startsWith("def_yellow_") -> ImpactStyle.BUFF to GoldC
        abilityId.startsWith("def_green_") -> ImpactStyle.BUFF to GreenC
        abilityId.startsWith("def_blue_") -> ImpactStyle.BUFF to BlueC
        abilityId.startsWith("def_violet_") -> ImpactStyle.BUFF to VioletC
        else -> null
    }
}

/** How a status wears on the card: spikes, orbs, rings, glows, haze. */
private enum class AuraStyle { SPIKES, ORB, RING, GLOW, HAZE }

private val auras: Map<String, Pair<Color, AuraStyle>> = mapOf(
    "thorns" to (BlueC to AuraStyle.SPIKES),
    "counter" to (OrangeC to AuraStyle.SPIKES),
    "fire_shield" to (OrangeC to AuraStyle.ORB),
    "bubble" to (Color(0xFF9BD2FF) to AuraStyle.ORB),
    "anger" to (RedC to AuraStyle.ORB),
    "reflect" to (GreenC to AuraStyle.RING),
    "guard" to (BlueC to AuraStyle.RING),
    "pest_guard" to (GoldC to AuraStyle.RING),
    "ward" to (VioletC to AuraStyle.RING),
    "immunity" to (GoldC to AuraStyle.RING),
    "echo" to (GreenC to AuraStyle.RING),
    "regen" to (GreenC to AuraStyle.GLOW),
    "kiss" to (Color(0xFFFF8FB1) to AuraStyle.GLOW),
    "war_cry" to (RedC to AuraStyle.GLOW),
    "rally" to (RedC to AuraStyle.GLOW),
    "spite" to (VioletC to AuraStyle.GLOW),
    "keen" to (Color(0xFFF7F7F7) to AuraStyle.GLOW),
    "ignite" to (OrangeC to AuraStyle.GLOW),
    "omen" to (VioletC to AuraStyle.GLOW),
    "dodge" to (Color(0xFF8B8797) to AuraStyle.HAZE),
    "cloak" to (VioletC to AuraStyle.HAZE)
)

@Composable
fun HpBar(hp: Int, maxHp: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = hp.toFloat() / maxHp,
        animationSpec = tween(durationMillis = 400),
        label = "hp"
    )
    val barColor = if (fraction < 0.35f) HpLow else HpGreen
    // Lost health shows as dark red behind the bar, and the exact number
    // sits on top in white — no guessing.
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF551418)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor)
        )
        Text(
            "$hp/$maxHp",
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(shadow = Shadow(color = Color(0xCC000000), blurRadius = 4f))
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

/** The card's persistent halo for whichever visual status it carries. */
@Composable
private fun StatusAura(unit: CombatUnit) {
    val spec = unit.statuses.firstNotNullOfOrNull { auras[it.defId] } ?: return
    val (color, style) = spec
    val pulse by rememberInfiniteTransition(label = "aura").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "auraPulse"
    )
    Canvas(Modifier.size(68.dp)) {
        val r = size.minDimension / 2f
        val c = center
        when (style) {
            AuraStyle.ORB -> {
                drawCircle(color.copy(alpha = 0.10f + 0.16f * pulse), r)
                drawCircle(color.copy(alpha = 0.6f * pulse), r - 2.dp.toPx(), style = Stroke(2.5.dp.toPx()))
            }
            AuraStyle.RING ->
                drawCircle(color.copy(alpha = 0.25f + 0.5f * pulse), r - 2.dp.toPx(), style = Stroke(3.dp.toPx()))
            AuraStyle.GLOW -> drawCircle(color.copy(alpha = 0.30f * pulse), r)
            AuraStyle.HAZE -> {
                drawCircle(color.copy(alpha = 0.16f * pulse), r)
                drawCircle(color.copy(alpha = 0.12f * pulse), r * 0.7f, center = Offset(c.x + 6f, c.y - 6f))
            }
            AuraStyle.SPIKES -> repeat(8) { i ->
                rotate(i * 45f, c) {
                    drawLine(
                        color.copy(alpha = 0.45f + 0.4f * pulse),
                        start = Offset(c.x, c.y - r + 3.dp.toPx()),
                        end = Offset(c.x, c.y - r - 3.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** One weapon strike playing out over the card, styled per item. */
@Composable
private fun ImpactOverlay(impact: ImpactFx?) {
    if (impact == null) return
    val progress = remember(impact.key) { Animatable(0f) }
    LaunchedEffect(impact.key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    if (p >= 1f) return
    val color = impact.color
    Canvas(Modifier.requiredSize(64.dp)) {
        val a = 1f - p
        val c = center
        val r = size.minDimension / 2f
        when (impact.style) {
            ImpactStyle.SLASH -> {
                val len = size.width * (0.3f + 0.9f * p)
                drawLine(color.copy(alpha = a), Offset(c.x - len / 2, c.y - len / 2),
                    Offset(c.x + len / 2, c.y + len / 2), 5.dp.toPx(), StrokeCap.Round)
                drawLine(color.copy(alpha = a * 0.6f), Offset(c.x + len / 2, c.y - len / 2),
                    Offset(c.x - len / 2, c.y + len / 2), 3.dp.toPx(), StrokeCap.Round)
            }
            ImpactStyle.SMASH -> {
                drawCircle(color.copy(alpha = a), r * p, style = Stroke(2.dp.toPx() + 5.dp.toPx() * a))
                drawCircle(color.copy(alpha = a * 0.4f), r * p * 0.6f)
            }
            ImpactStyle.FIRE -> {
                drawCircle(color.copy(alpha = a * 0.55f), r * (0.4f + 0.6f * p))
                drawCircle(Color(0xFFFFF3C4).copy(alpha = a * 0.5f), r * 0.3f * a)
            }
            ImpactStyle.MULTI -> repeat(3) { i ->
                val t = (p * 3f - i).coerceIn(0f, 1f)
                if (t > 0f) drawCircle(
                    color.copy(alpha = 1f - t),
                    4.dp.toPx() + 6.dp.toPx() * t,
                    center = Offset(c.x + (i - 1) * 14.dp.toPx(), c.y + (i - 1) * 8.dp.toPx())
                )
            }
            ImpactStyle.MAGIC -> rotate(90f * p, c) {
                drawLine(color.copy(alpha = a), Offset(c.x, c.y - r * 0.8f),
                    Offset(c.x, c.y + r * 0.8f), 4.dp.toPx(), StrokeCap.Round)
                drawLine(color.copy(alpha = a), Offset(c.x - r * 0.8f, c.y),
                    Offset(c.x + r * 0.8f, c.y), 4.dp.toPx(), StrokeCap.Round)
            }
            ImpactStyle.WAVE -> drawArc(
                color.copy(alpha = a), startAngle = 150f, sweepAngle = 240f * p,
                useCenter = false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
            )
            ImpactStyle.BUFF -> {
                drawCircle(color.copy(alpha = a * 0.8f), r * (0.3f + 0.7f * p), style = Stroke(3.5.dp.toPx()))
                drawCircle(color.copy(alpha = a * 0.25f), r * p)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnitCard(
    unit: CombatUnit,
    isTargetable: Boolean,
    isActiveActor: Boolean,
    isActing: Boolean = false,   // this unit's move is playing out right now
    flash: UnitFlash? = null,    // latest hit/heal/shield reaction to play
    turnsLeft: Int = 0,          // heroes with turns left get an underline
    glowColor: Color? = null,    // halo behind the card while a drag hovers it
    impact: ImpactFx? = null,    // the strike animation currently landing here
    floaties: List<Floaty>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
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

    // The attacker steps toward the other line and springs back.
    val lunge = remember { Animatable(0f) }
    LaunchedEffect(isActing) {
        if (isActing && !Prefs.reduceMotion) {
            val toward = if (unit.team == Team.PLAYER) -26f else 26f
            lunge.animateTo(toward, tween(durationMillis = 130, easing = FastOutSlowInEasing))
            lunge.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    // Reaction to what just happened to this unit: recoil and a red wash on
    // a hit, a swell and green/blue wash on heals and shields.
    val punch = remember { Animatable(1f) }
    val washAlpha = remember { Animatable(0f) }
    LaunchedEffect(flash?.key) {
        val kind = flash?.kind ?: return@LaunchedEffect
        if (Prefs.reduceMotion) {
            washAlpha.snapTo(0.5f)
            washAlpha.animateTo(0f, tween(durationMillis = 380))
            return@LaunchedEffect
        }
        launch {
            when (kind) {
                FlashKind.HIT -> {
                    punch.snapTo(0.84f)
                    punch.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
                FlashKind.HEAL, FlashKind.SHIELD -> {
                    punch.snapTo(1.1f)
                    punch.animateTo(1f, tween(durationMillis = 320))
                }
            }
        }
        washAlpha.snapTo(0.5f)
        washAlpha.animateTo(0f, tween(durationMillis = 380))
    }
    val washColor = when (flash?.kind) {
        FlashKind.HIT -> Color(0xFFFF5A5A)
        FlashKind.HEAL -> HpGreen
        FlashKind.SHIELD -> ShieldBlue
        null -> Color.Transparent
    }

    val borderColor = when {
        isActing -> Color(0xFFF7F7F7)
        isTargetable -> Accent
        isActiveActor -> EnergyGold
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(76.dp)
            .graphicsLayer {
                scaleX = actingScale * punch.value
                scaleY = actingScale * punch.value
                translationY = lunge.value
            }
            .alpha(deadAlpha)
            .combinedClickable(enabled = unit.isAlive, onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = unit.name }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // The drop halo: lights up behind whoever the drag is hovering
            // so there is never any doubt who receives it.
            if (glowColor != null) {
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(glowColor.copy(alpha = 0.45f))
                )
            }
            StatusAura(unit)
            unit.charge?.let {
                ChargeRing(
                    progress = it.progress,
                    ready = it.isReady,
                    modifier = Modifier.size(64.dp)
                )
            }
            // A hero's weapon rides BEHIND the body: declared before the
            // block so the block overlaps the grip and nothing floats.
            val weaponArt = GameArt[unit.abilities.getOrNull(0)?.iconId]
            if (weaponArt != null && GameIcons.heroColor(unit.iconId) != null) {
                Image(
                    painter = painterResource(weaponArt),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .requiredSize(38.dp)
                        .graphicsLayer {
                            translationX = with(this) { 32.dp.toPx() }
                            translationY = with(this) { 4.dp.toPx() }
                            rotationZ = if (isActing) -14f else 0f
                        }
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
                if (washAlpha.value > 0f) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(washColor.copy(alpha = washAlpha.value))
                    )
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
            ImpactOverlay(impact)
            // Numbers rise from above the character's head, full strength,
            // never hiding behind the block.
            Box(
                Modifier.requiredSize(64.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                FloatingNumbers(floaties)
            }
        }
        HpBar(unit.hp, unit.maxHp, Modifier.width(60.dp).padding(top = 4.dp))
        StatusRow(
            unit.statuses.filterNot { it.defId == Resolver.TAUNT_STATUS_ID },
            Modifier.padding(top = 3.dp)
        )
        // The whole turn display: an underline while this hero can still
        // act, with the number of turns they have banked under it.
        if (turnsLeft > 0) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EnergyGold)
            )
            // One turn is the normal state; only banked extras get a number.
            if (turnsLeft > 1) {
                Text(
                    "$turnsLeft",
                    color = EnergyGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
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
            val big = Prefs.bigNumbers
            // Big and unmissable: reading the number IS the combat feedback.
            // A dark halo keeps it legible over any block color.
            Text(
                text = floaty.text,
                color = floaty.color,
                fontSize = when {
                    floaty.label && big -> 16.sp
                    floaty.label -> 13.sp
                    big -> 30.sp
                    else -> 25.sp
                },
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(color = Color(0xE6000000), blurRadius = 8f)
                ),
                modifier = Modifier
                    .graphicsLayer {
                        translationY = -30f - 34f * progress
                        // Full opacity for the whole ride, fading only at
                        // the very end.
                        alpha = if (progress < 0.7f) 1f
                        else 1f - (progress - 0.7f) / 0.3f
                        scaleX = 1f + 0.25f * (1f - progress)
                        scaleY = 1f + 0.25f * (1f - progress)
                    }
            )
        }
    }
}

/** The party's shared ultimate meter: one gold bar, filled slowly by
 *  everyone's damage dealt and taken. When it's full you drag the bar's
 *  glow onto a hero and they fire their ultimate. */
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
