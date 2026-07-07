package com.elendheim.fewsbox.ui.battle

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elendheim.fewsbox.engine.ability.Targeting
import com.elendheim.fewsbox.engine.event.CombatEvent
import com.elendheim.fewsbox.engine.model.Team
import com.elendheim.fewsbox.engine.model.TurnPhase
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
    onVictory: () -> Unit,
    onDefeat: () -> Unit
) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val battle = snapshot.battle ?: return

    var selectedAbilityId by remember { mutableStateOf<String?>(null) }
    var activeActorId by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<InfoContent?>(null) }

    // Event-driven feedback. Today: flashes and floating numbers. Later the
    // same collection point drives sprites, particles and sound.
    val flashes = remember { mutableStateMapOf<String, Int>() }
    val floaties = remember { mutableStateMapOf<String, List<Floaty>>() }

    LaunchedEffect(vm) {
        var floatyKey = 0L
        fun addFloaty(unitId: String, text: String, color: Color) {
            val key = floatyKey++
            floaties[unitId] = (floaties[unitId] ?: emptyList()) + Floaty(key, text, color)
            launch {
                delay(850)
                floaties[unitId] = (floaties[unitId] ?: emptyList()).filterNot { it.key == key }
            }
        }
        vm.events.collect { event ->
            when (event) {
                is CombatEvent.DamageDealt -> {
                    flashes[event.targetId] = (flashes[event.targetId] ?: 0) + 1
                    addFloaty(event.targetId, "-${event.amount}", if (event.isCrit) EnergyGold else Accent)
                }
                is CombatEvent.StatusTicked -> {
                    flashes[event.targetId] = (flashes[event.targetId] ?: 0) + 1
                    addFloaty(event.targetId, "-${event.amount}", TextMuted)
                }
                is CombatEvent.Healed -> addFloaty(event.targetId, "+${event.amount}", HpGreen)
                is CombatEvent.ShieldGained -> addFloaty(event.targetId, "+${event.amount}", ShieldBlue)
                else -> {}
            }
        }
    }

    // Keep the active actor pointing at a unit that can still act.
    val pending = battle.pendingPlayers
    val activeActor = pending.firstOrNull { it.id == activeActorId } ?: pending.firstOrNull()
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
                        flashCount = flashes[enemy.id] ?: 0,
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
                        flashCount = flashes[player.id] ?: 0,
                        floaties = floaties[player.id] ?: emptyList(),
                        onLongClick = { info = GameText.unitInfo(player) },
                        onClick = {
                            when {
                                allyTargetable -> useAbilityOn(player.id)
                                player.id in battle.actedThisRound || !player.isAlive -> {}
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
                    for (ability in activeActor.abilities) {
                        val affordable = battle.resources.energy >= ability.cost
                        val ready = activeActor.cooldownLeft(ability.id) == 0
                        AbilityButton(
                            ability = ability,
                            selected = ability.id == selectedAbilityId,
                            enabled = !inputLocked && affordable && ready,
                            cooldownLeft = activeActor.cooldownLeft(ability.id),
                            onLongClick = { info = GameText.abilityInfo(ability, activeActor.baseAttack) },
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
                    // Hold: this hero sits the round out and saves energy.
                    Text(
                        text = "HOLD",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !inputLocked) { vm.passUnit(activeActor.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            EnergyPips(battle.resources.energy, battle.resources.maxEnergy)
            Spacer(Modifier.height(8.dp))
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

private fun Targeting.targetsEnemy(): Boolean = when (this) {
    Targeting.SINGLE_ENEMY, Targeting.ADJACENT_ENEMIES -> true
    else -> false
}

private fun Targeting.needsChosenTarget(): Boolean = when (this) {
    Targeting.SINGLE_ENEMY, Targeting.SINGLE_ALLY, Targeting.ADJACENT_ENEMIES -> true
    else -> false
}
