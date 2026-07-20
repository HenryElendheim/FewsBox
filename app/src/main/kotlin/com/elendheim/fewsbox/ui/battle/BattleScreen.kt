package com.elendheim.fewsbox.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elendheim.fewsbox.data.Statuses
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.model.TurnPhase
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.GameText
import com.elendheim.fewsbox.ui.InfoContent
import com.elendheim.fewsbox.ui.InfoOverlay
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

@Composable
fun BattleScreen(
    vm: BattleViewModel,
    onVictory: (survivors: Int) -> Unit,
    onDefeat: () -> Unit
) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val battle = snapshot.battle ?: return

    var selectedAbilityId by remember { mutableStateOf<String?>(null) }
    var activeActorId by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<InfoContent?>(null) }

    // Event-driven feedback: floating numbers, status announcements and
    // screen shake. Later the same collection point drives real animations.
    val floaties = remember { mutableStateMapOf<String, List<Floaty>>() }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(vm) {
        var floatyKey = 0L
        fun addFloaty(unitId: String, text: String, color: Color, label: Boolean = false) {
            val key = floatyKey++
            floaties[unitId] = (floaties[unitId] ?: emptyList()) + Floaty(key, text, color, label)
            launch {
                delay(if (label) 1000L else 850L)
                floaties[unitId] = (floaties[unitId] ?: emptyList()).filterNot { it.key == key }
            }
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
                is CombatEvent.DamageDealt -> {
                    addFloaty(event.targetId, "-${event.amount}", if (event.isCrit) EnergyGold else Accent)
                    // Big hits and crits rattle the whole screen.
                    if (event.isCrit || event.amount >= 12) shakeScreen()
                }
                is CombatEvent.StatusTicked ->
                    addFloaty(event.targetId, "-${event.amount}", statusColor(event.statusId))
                is CombatEvent.Healed -> addFloaty(event.targetId, "+${event.amount}", HpGreen)
                is CombatEvent.ShieldGained -> addFloaty(event.targetId, "+${event.amount}", ShieldBlue)
                // The status announcement: what just landed on whom, in the
                // status's own color. Placeholder for real effect animations.
                is CombatEvent.StatusApplied ->
                    addFloaty(event.targetId, GameText.name(event.statusId).uppercase(),
                        statusColor(event.statusId), label = true)
                is CombatEvent.StatusConsumed ->
                    addFloaty(event.targetId, "${GameText.name(event.statusId).uppercase()} BURST",
                        statusColor(event.statusId), label = true)
                is CombatEvent.TurnSkipped ->
                    addFloaty(event.unitId, "STUNNED", statusColor("stun"), label = true)
                is CombatEvent.ChargeFired ->
                    addFloaty(event.unitId, GameText.name(event.abilityId).uppercase(),
                        Accent, label = true)
                else -> {}
            }
        }
    }

    // Any living hero can be the active actor: heroes who already acted can
    // still spend the party's ultimate.
    val pending = battle.pendingPlayers
    val activeActor = battle.players.firstOrNull { it.id == activeActorId } ?: pending.firstOrNull()
    if (activeActor?.id != activeActorId) {
        activeActorId = activeActor?.id
        selectedAbilityId = null
    }
    val selectedAbility = activeActor?.abilities?.firstOrNull { it.id == selectedAbilityId }

    val inputLocked = snapshot.enemyTurnRunning || battle.phase != TurnPhase.PLAYER_INPUT

    fun useAbilityOn(targetId: String?) {
        val actor = activeActor ?: return
        val ability = selectedAbility ?: return
        vm.playerAction(actor.id, ability.id, listOfNotNull(targetId))
        selectedAbilityId = null
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
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
                text = if (snapshot.enemyTurnRunning) "ROUND ${battle.round}" else "ROUND ${battle.round} - ${pending.size} TO ACT",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(24.dp))

            // Enemy line
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (enemy in battle.units.filter { it.team == Team.ENEMY }) {
                    val targetable = !inputLocked && selectedAbility != null &&
                        enemy.isAlive && selectedAbility.targeting.targetsEnemy()
                    UnitCard(
                        unit = enemy,
                        isTargetable = targetable,
                        isActiveActor = false,
                        floaties = floaties[enemy.id] ?: emptyList(),
                        onClick = { if (targetable) useAbilityOn(enemy.id) },
                        onLongClick = { info = GameText.unitInfo(enemy) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Player line
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (player in battle.units.filter { it.team == Team.PLAYER }) {
                    val allyTargetable = !inputLocked && selectedAbility != null &&
                        player.isAlive && selectedAbility.targeting == Targeting.SINGLE_ALLY
                    UnitCard(
                        unit = player,
                        isTargetable = allyTargetable,
                        isActiveActor = player.id == activeActor?.id,
                        floaties = floaties[player.id] ?: emptyList(),
                        onLongClick = { info = GameText.unitInfo(player) },
                        onClick = {
                            when {
                                allyTargetable -> useAbilityOn(player.id)
                                !player.isAlive -> {}
                                else -> {
                                    activeActorId = player.id
                                    selectedAbilityId = null
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Ability bar for whichever hero is up
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeActor != null) {
                    val actorActed = activeActor.id in battle.actedThisRound
                    for (ability in activeActor.abilities) {
                        val isUltimate = ability.id == activeActor.ultimateId
                        val usable = activeActor.cooldownLeft(ability.id) == 0 &&
                            if (isUltimate) battle.partyUltReady else !actorActed
                        AbilityButton(
                            ability = ability,
                            selected = ability.id == selectedAbilityId,
                            enabled = !inputLocked && usable,
                            cooldownLeft = activeActor.cooldownLeft(ability.id),
                            onLongClick = { info = GameText.abilityInfo(ability, activeActor.baseAttack, isUltimate) },
                            onClick = {
                                if (ability.targeting.needsChosenTarget()) {
                                    selectedAbilityId =
                                        if (selectedAbilityId == ability.id) null else ability.id
                                } else {
                                    // Self / whole-line abilities fire on tap.
                                    vm.playerAction(activeActor.id, ability.id, emptyList())
                                    selectedAbilityId = null
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            UltMeterBar(
                percent = battle.partyUltCharge,
                modifier = Modifier.fillMaxWidth(0.72f)
            )
            Spacer(Modifier.height(10.dp))
        }

        InfoOverlay(content = info, onDismiss = { info = null })

        // Battle end overlay
        if (battle.phase == TurnPhase.BATTLE_OVER) {
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
                        onClick = { if (won) onVictory(battle.players.size) else onDefeat() },
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

private fun Targeting.targetsEnemy(): Boolean = when (this) {
    Targeting.SINGLE_ENEMY, Targeting.ADJACENT_ENEMIES -> true
    else -> false
}

private fun Targeting.needsChosenTarget(): Boolean = when (this) {
    Targeting.SINGLE_ENEMY, Targeting.SINGLE_ALLY, Targeting.ADJACENT_ENEMIES -> true
    else -> false
}
