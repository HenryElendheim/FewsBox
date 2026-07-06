package com.elendheim.fewsbox.ui.loadout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Weapons
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.GameText
import com.elendheim.fewsbox.ui.IconChip
import com.elendheim.fewsbox.ui.InfoContent
import com.elendheim.fewsbox.ui.InfoOverlay
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/**
 * Pick a weapon and an offhand per hero, then fight. Long-press anything to
 * read what it actually does, with damage numbers from that hero's stats.
 */
@Composable
fun LoadoutScreen(
    party: List<Loadout>,
    battleIndex: Int,
    battleCount: Int,
    onLoadoutChange: (Loadout) -> Unit,
    onFight: () -> Unit
) {
    var info by remember { mutableStateOf<InfoContent?>(null) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "FEWSBOX",
                color = Accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            Spacer(Modifier.height(6.dp))

            // Battle progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(battleCount) { index ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (index <= battleIndex) Accent else PanelRaised)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            for (member in party) {
                HeroLoadoutCard(
                    member = member,
                    onLoadoutChange = onLoadoutChange,
                    onInfo = { info = it }
                )
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onFight,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("FIGHT", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Hold anything to see what it does",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        InfoOverlay(content = info, onDismiss = { info = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroLoadoutCard(
    member: Loadout,
    onLoadoutChange: (Loadout) -> Unit,
    onInfo: (InfoContent) -> Unit
) {
    val attack = member.baseAttack + member.weapon.attackBonus

    fun heroInfo() = InfoContent(
        title = member.unitName,
        subtitle = "HP ${member.maxHp} · ATK $attack with ${GameText.name(member.weapon.id)}",
        lines = buildList {
            add(GameText.name(member.weapon.id).uppercase())
            add("  " + GameText.weaponBlurb(member.weapon.id))
            addAll(GameText.describeAbility(member.weapon.grantedAbility, attack).map { "  $it" })
            add(GameText.name(member.offhand.id).uppercase())
            add("  " + GameText.offhandBlurb(member.offhand.id))
            addAll(GameText.describeAbility(member.offhand.grantedAbility, attack).map { "  $it" })
        }
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { onInfo(heroInfo()) })
        ) {
            // Heroes are solid color blocks, matching how they look in battle.
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GameIcons.heroColor(member.iconId) ?: Accent)
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(member.unitName, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPip("HP ${member.maxHp}")
                    StatPip("ATK $attack")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        PickerRow(
            options = Weapons.ALL.map { it.id to it.iconId },
            selectedId = member.weapon.id,
            onPick = { id -> onLoadoutChange(member.copy(weapon = Weapons.REGISTRY.getValue(id))) },
            onHold = { id ->
                val weapon = Weapons.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = "Weapon" + if (weapon.attackBonus > 0) " · +${weapon.attackBonus} ATK" else "",
                        lines = listOf(GameText.weaponBlurb(id)) +
                            GameText.describeAbility(weapon.grantedAbility, member.baseAttack + weapon.attackBonus)
                    )
                )
            }
        )
        Spacer(Modifier.height(8.dp))
        PickerRow(
            options = Offhands.ALL.map { it.id to it.iconId },
            selectedId = member.offhand.id,
            onPick = { id -> onLoadoutChange(member.copy(offhand = Offhands.REGISTRY.getValue(id))) },
            onHold = { id ->
                val offhand = Offhands.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = "Offhand",
                        lines = listOf(GameText.offhandBlurb(id)) +
                            GameText.describeAbility(offhand.grantedAbility, attack)
                    )
                )
            }
        )
    }
}

@Composable
private fun StatPip(text: String) {
    Text(text, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerRow(
    options: List<Pair<String, String>>,
    selectedId: String,
    onPick: (String) -> Unit,
    onHold: (String) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((id, iconId) in options) {
            val selected = id == selectedId
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        if (selected) EnergyGold else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(onClick = { onPick(id) }, onLongClick = { onHold(id) })
                    .padding(3.dp)
            ) {
                IconChip(iconId, size = 44)
            }
        }
    }
}
