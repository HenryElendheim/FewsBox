package com.elendheim.fewsbox.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.CombatUnit
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.model.TurnPhase
import com.elendheim.fewsbox.ui.GameText
import com.elendheim.fewsbox.ui.InfoContent
import com.elendheim.fewsbox.ui.InfoOverlay
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.DangerRed
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.ShieldBlue
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The battle. The drag is the whole language: drag a hero onto an enemy to
 * swing the weapon at them, drag a hero onto a teammate to use the offhand
 * on them, tap a hero to use the offhand on themselves, and drag the full
 * ult bar onto a hero to fire their ultimate. No buttons. Long-press
 * anything for exact numbers.
 */
@Composable
fun BattleScreen(
    vm: BattleViewModel,
    onVictory: () -> Unit,
    onDefeat: () -> Unit
) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val battle = snapshot.battle ?: return

    var actingUnitId by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<InfoContent?>(null) }
    var ultFlashText by remember { mutableStateOf<String?>(null) }

    // Event-driven feedback: floating numbers, card reactions and screen
    // shake, all keyed off the engine's combat events.
    val floaties = remember { mutableStateMapOf<String, List<Floaty>>() }
    val flashes = remember { mutableStateMapOf<String, UnitFlash>() }
    val shake = remember { Animatable(0f) }

    // Geometry for the two drags (hero commands and the ult bar): where
    // every card sits on screen and where the finger is, in root coords.
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var barTopLeft by remember { mutableStateOf(Offset.Zero) }
    val playerRects = remember { mutableStateMapOf<String, Rect>() }
    val enemyRects = remember { mutableStateMapOf<String, Rect>() }
    var heroDragSourceId by remember { mutableStateOf<String?>(null) }
    var heroDragPos by remember { mutableStateOf<Offset?>(null) }
    var ultDragPos by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(vm) {
        var floatyKey = 0L
        var flashKey = 0L
        fun addFloaty(unitId: String, text: String, color: Color, label: Boolean = false) {
            val key = floatyKey++
            floaties[unitId] = (floaties[unitId] ?: emptyList()) + Floaty(key, text, color, label)
            launch {
                delay(if (label) 1000L else 850L)
                floaties[unitId] = (floaties[unitId] ?: emptyList()).filterNot { it.key == key }
            }
        }
        fun flashUnit(unitId: String, kind: FlashKind) {
            flashes[unitId] = UnitFlash(flashKey++, kind)
        }
        fun statusColor(statusId: String): Color =
            Statuses.REGISTRY[statusId]?.let { GameIcons[it.iconId].tint } ?: TextMuted
        fun shakeScreen() {
            launch {
                shake.snapTo(0f)
                shake.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 260
                        14f at 30
                        -10f at 90
                        6f at 150
                        -3f at 210
                    }
                )
            }
        }
        vm.events.collect { event ->
            when (event) {
                // Spotlight whoever is acting so enemy turns read clearly,
                // and go full gold when the move is an ultimate.
                is CombatEvent.AbilityUsed -> {
                    actingUnitId = event.actorId
                    launch {
                        delay(650)
                        if (actingUnitId == event.actorId) actingUnitId = null
                    }
                    val actor = vm.snapshot.value.battle?.unitOrNull(event.actorId)
                    if (actor != null && actor.ultimateId == event.abilityId) {
                        ultFlashText = GameText.name(event.abilityId).uppercase()
                        launch {
                            delay(1000)
                            ultFlashText = null
                        }
                    }
                }
                is CombatEvent.DamageDealt -> {
                    addFloaty(event.targetId, "-${event.amount}", if (event.isCrit) EnergyGold else Accent)
                    flashUnit(event.targetId, FlashKind.HIT)
                    // Big hits and crits rattle the whole screen.
                    if (event.isCrit || event.amount >= 12) shakeScreen()
                }
                is CombatEvent.StatusTicked -> {
                    addFloaty(event.targetId, "-${event.amount}", statusColor(event.statusId))
                    flashUnit(event.targetId, FlashKind.HIT)
                }
                is CombatEvent.Healed -> {
                    addFloaty(event.targetId, "+${event.amount}", HpGreen)
                    flashUnit(event.targetId, FlashKind.HEAL)
                }
                is CombatEvent.ShieldGained -> {
                    addFloaty(event.targetId, "+${event.amount}", ShieldBlue)
                    flashUnit(event.targetId, FlashKind.SHIELD)
                }
                // The status announcement: what just landed on whom, in the
                // status's own color.
                is CombatEvent.StatusApplied ->
                    addFloaty(event.targetId, GameText.name(event.statusId).uppercase(),
                        statusColor(event.statusId), label = true)
                is CombatEvent.StatusConsumed ->
                    addFloaty(event.targetId, "${GameText.name(event.statusId).uppercase()} BURST",
                        statusColor(event.statusId), label = true)
                is CombatEvent.ExtraActionsGranted ->
                    addFloaty(event.unitId, "+${event.count} TURNS", EnergyGold, label = true)
                is CombatEvent.TurnSkipped ->
                    addFloaty(event.unitId, "STUNNED", statusColor("stun"), label = true)
                is CombatEvent.ChargeFired ->
                    addFloaty(event.unitId, GameText.name(event.abilityId).uppercase(),
                        Accent, label = true)
                else -> {}
            }
        }
    }

    val inputLocked = snapshot.enemyTurnRunning || snapshot.stageClearing ||
        battle.phase != TurnPhase.PLAYER_INPUT

    fun canAct(actor: CombatUnit, abilityIndex: Int): Boolean {
        val ability = actor.abilities.getOrNull(abilityIndex) ?: return false
        return battle.actionsLeft(actor) > 0 && actor.cooldownLeft(ability.id) == 0
    }

    // Tap a hero: their offhand lands on themselves.
    fun tapHero(hero: CombatUnit) {
        if (inputLocked || !hero.isAlive) return
        val offhand = hero.abilities.getOrNull(1) ?: return
        if (!canAct(hero, 1)) return
        val targets = if (offhand.targeting == Targeting.SELF) emptyList() else listOf(hero.id)
        vm.playerAction(hero.id, offhand.id, targets)
    }

    // Where a released hero-drag lands: enemy = weapon, teammate = offhand.
    fun dropHeroDrag(sourceId: String, pos: Offset) {
        val b = vm.snapshot.value.battle ?: return
        val source = b.unitOrNull(sourceId) ?: return
        if (!source.isAlive) return
        val enemyId = enemyRects.entries.firstOrNull { it.value.contains(pos) }?.key
        if (enemyId != null && b.unitOrNull(enemyId)?.isAlive == true) {
            val weapon = source.abilities.getOrNull(0) ?: return
            vm.playerAction(source.id, weapon.id, listOf(enemyId))
            return
        }
        val allyId = playerRects.entries.firstOrNull { it.value.contains(pos) }?.key
        if (allyId != null && b.unitOrNull(allyId)?.isAlive == true) {
            val offhand = source.abilities.getOrNull(1) ?: return
            val targets = if (offhand.targeting == Targeting.SELF) emptyList() else listOf(allyId)
            // A self-only offhand dragged onto someone else just fizzles.
            if (offhand.targeting == Targeting.SELF && allyId != source.id) return
            vm.playerAction(source.id, offhand.id, targets)
        }
    }

    val dragSource = heroDragSourceId?.let { battle.unitOrNull(it) }
    val heroDragTargetId = heroDragPos?.let { pos ->
        (enemyRects.entries + playerRects.entries).firstOrNull { it.value.contains(pos) }?.key
    }
    val ultDragTargetId = ultDragPos?.let { pos ->
        playerRects.entries.firstOrNull { it.value.contains(pos) }?.key
    }

    val enemiesTargetable = dragSource != null && canAct(dragSource, 0)
    val alliesTargetable = dragSource != null && canAct(dragSource, 1)

    val levelLabel = if (snapshot.levelIndex >= Battles.CAMPAIGN_LEVELS) {
        "ENDLESS ${snapshot.levelIndex - Battles.CAMPAIGN_LEVELS + 1}"
    } else {
        "LEVEL ${snapshot.levelIndex + 1}"
    }
    val stageLabel = if (snapshot.stageCount > 1) " - STAGE ${snapshot.stage + 1}/${snapshot.stageCount}" else ""

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .onGloballyPositioned { rootOrigin = it.positionInRoot() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = shake.value }
                .systemBarsPadding()   // keep the battle clear of the status bar
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$levelLabel$stageLabel",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(20.dp))

            // Enemy line
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (enemy in battle.units.filter { it.team == Team.ENEMY }) {
                    UnitCard(
                        unit = enemy,
                        isTargetable = enemiesTargetable && enemy.isAlive,
                        isActiveActor = false,
                        isActing = enemy.id == actingUnitId,
                        flash = flashes[enemy.id],
                        glowColor = if (enemy.id == heroDragTargetId && enemiesTargetable) DangerRed else null,
                        floaties = floaties[enemy.id] ?: emptyList(),
                        onClick = {},
                        onLongClick = { info = GameText.unitInfo(enemy) },
                        modifier = Modifier.onGloballyPositioned {
                            enemyRects[enemy.id] = it.boundsInRoot()
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Player line: every card is a drag source and a drop target.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (player in battle.units.filter { it.team == Team.PLAYER }) {
                    Box(
                        Modifier.pointerInput(player.id) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val snap = vm.snapshot.value
                                    val b = snap.battle
                                    val self = b?.unitOrNull(player.id)
                                    if (b != null && self != null && self.isAlive &&
                                        !snap.enemyTurnRunning && !snap.stageClearing &&
                                        b.phase == TurnPhase.PLAYER_INPUT &&
                                        b.actionsLeft(self) > 0
                                    ) {
                                        heroDragSourceId = player.id
                                        heroDragPos = (playerRects[player.id]?.topLeft ?: Offset.Zero) + offset
                                    }
                                },
                                onDrag = { change, amount ->
                                    if (heroDragPos != null) {
                                        change.consume()
                                        heroDragPos = heroDragPos!! + amount
                                    }
                                },
                                onDragEnd = {
                                    val source = heroDragSourceId
                                    val pos = heroDragPos
                                    heroDragSourceId = null
                                    heroDragPos = null
                                    if (source != null && pos != null) dropHeroDrag(source, pos)
                                },
                                onDragCancel = {
                                    heroDragSourceId = null
                                    heroDragPos = null
                                }
                            )
                        }
                    ) {
                        UnitCard(
                            unit = player,
                            isTargetable = alliesTargetable && player.isAlive &&
                                player.id != heroDragSourceId,
                            isActiveActor = player.id == heroDragSourceId,
                            isActing = player.id == actingUnitId,
                            flash = flashes[player.id],
                            glowColor = when {
                                player.id == ultDragTargetId -> EnergyGold
                                player.id == heroDragTargetId && alliesTargetable -> HpGreen
                                else -> null
                            },
                            turnsLeft = if (player.isAlive && battle.phase == TurnPhase.PLAYER_INPUT &&
                                !snapshot.enemyTurnRunning
                            ) battle.actionsLeft(player).coerceAtLeast(0) else 0,
                            floaties = floaties[player.id] ?: emptyList(),
                            onClick = { tapHero(player) },
                            onLongClick = { info = GameText.unitInfo(player) },
                            modifier = Modifier.onGloballyPositioned {
                                playerRects[player.id] = it.boundsInRoot()
                            }
                        )
                    }
                }
            }

            // Workshop tray: what you brought, one use of each per round.
            val slots = snapshot.version.let { vm.consumableSlots() }
            if (slots.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (slot in slots) {
                        val usable = slot.remaining > 0 && !slot.usedThisRound && !inputLocked
                        val label = when (slot.id) {
                            "con_spark" -> "SPARK"
                            "con_bandage" -> "BANDAGE"
                            "con_ironskin" -> "IRON"
                            else -> slot.id.uppercase()
                        }
                        Text(
                            "$label x${slot.remaining}",
                            color = if (usable) EnergyGold else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF26242F))
                                .clickable(enabled = usable) { vm.useConsumable(slot.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .onGloballyPositioned { barTopLeft = it.positionInRoot() }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val snap = vm.snapshot.value
                                val b = snap.battle
                                if (b != null && b.partyUltReady && !snap.enemyTurnRunning &&
                                    !snap.stageClearing && b.phase == TurnPhase.PLAYER_INPUT
                                ) {
                                    ultDragPos = barTopLeft + offset
                                }
                            },
                            onDrag = { change, amount ->
                                if (ultDragPos != null) {
                                    change.consume()
                                    ultDragPos = ultDragPos!! + amount
                                }
                            },
                            onDragEnd = {
                                val pos = ultDragPos
                                ultDragPos = null
                                if (pos != null) {
                                    val targetId = playerRects.entries
                                        .firstOrNull { it.value.contains(pos) }?.key
                                    val b = vm.snapshot.value.battle
                                    val hero = targetId?.let { b?.unitOrNull(it) }
                                    val ultId = hero?.ultimateId
                                    // No hero under the finger: the charge
                                    // just slides back into its slot.
                                    if (hero != null && hero.isAlive && ultId != null) {
                                        vm.playerAction(hero.id, ultId, emptyList())
                                    }
                                }
                            },
                            onDragCancel = { ultDragPos = null }
                        )
                    }
            ) {
                UltMeterBar(percent = battle.partyUltPercent, modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = if (battle.partyUltReady) "ULT READY - DRAG THE BAR ONTO A HERO"
                else "ULT ${battle.partyUltPercent}%",
                color = if (battle.partyUltReady) EnergyGold else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // The orb chasing the finger: gold for the ult, hero-colored for a
        // hero command.
        ultDragPos?.let { pos ->
            val local = pos - rootOrigin
            Box(
                Modifier
                    .offset { IntOffset((local.x - 18.dp.toPx()).roundToInt(), (local.y - 18.dp.toPx()).roundToInt()) }
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EnergyGold)
                    .border(3.dp, Color(0xFFFFF3C4), CircleShape)
            )
        }
        heroDragPos?.let { pos ->
            val local = pos - rootOrigin
            val color = dragSource?.let { GameIcons.heroColor(it.iconId) } ?: Accent
            Box(
                Modifier
                    .offset { IntOffset((local.x - 14.dp.toPx()).roundToInt(), (local.y - 14.dp.toPx()).roundToInt()) }
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, TextBright, CircleShape)
            )
        }

        // Full-screen wash when an ultimate goes off.
        ultFlashText?.let { text ->
            val wash = remember(text) { Animatable(0.45f) }
            LaunchedEffect(text) { wash.animateTo(0f, tween(durationMillis = 900)) }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(EnergyGold.copy(alpha = wash.value))
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = EnergyGold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )
            }
        }

        InfoOverlay(content = info, onDismiss = { info = null })

        // Between stages: the wave is down but the level isn't over.
        if (snapshot.stageClearing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x9914141A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STAGE ${snapshot.stage + 1} CLEAR",
                        color = HpGreen,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "NEXT WAVE INCOMING",
                        color = TextBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Battle end overlay: only when the whole level is decided.
        if (battle.phase == TurnPhase.BATTLE_OVER && (battle.isDefeat || snapshot.onFinalStage)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xCC14141A))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val won = battle.isVictory
                    Text(
                        text = if (won) "VICTORY" else "DEFEAT",
                        color = if (won) HpGreen else DangerRed,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { if (won) onVictory() else onDefeat() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Ink
                        )
                    ) {
                        Text(
                            text = if (won) "CONTINUE" else "RETRY",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
