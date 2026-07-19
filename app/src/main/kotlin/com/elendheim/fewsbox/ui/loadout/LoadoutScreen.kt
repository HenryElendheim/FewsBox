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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Ultimates
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
 * Pick who fights (up to Party.MAX_SIZE from the rainbow roster), then pick
 * each hero's kit from their own restricted pools. Long-press anything to
 * read what it does.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoadoutScreen(
    roster: List<Loadout>,
    selectedIds: Set<String>,
    lockedCount: Int,
    battleIndex: Int,
    battleCount: Int,
    onToggleHero: (String) -> Unit,
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

            Spacer(Modifier.height(18.dp))

            // The roster: tap a color to put it in (or pull it from) the party.
            Text(
                "PARTY ${selectedIds.size}/${Party.MAX_SIZE}",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (member in roster) {
                    val inParty = member.hero.id in selectedIds
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                3.dp,
                                if (inParty) TextBright else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .background(GameIcons.heroColor(member.hero.iconId) ?: Accent)
                            .alpha(if (inParty) 1f else 0.45f)
                            .combinedClickable(
                                onClick = { onToggleHero(member.hero.id) },
                                onLongClick = { info = heroInfo(member) }
                            )
                    )
                }
                repeat(lockedCount) {
                    // Empty slots. Something gray is waiting out there.
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PanelRaised)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    info = InfoContent(
                                        title = "?",
                                        subtitle = "Locked",
                                        lines = listOf("Something gray waits at the end of the campaign.")
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            for (member in roster.filter { it.hero.id in selectedIds }) {
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
                enabled = selectedIds.isNotEmpty(),
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

private fun heroInfo(member: Loadout): InfoContent {
    val attack = member.hero.baseAttack + member.weapon.attackBonus
    val ultimate = Ultimates.REGISTRY.getValue(member.hero.ultimateId)
    return InfoContent(
        title = member.hero.name,
        subtitle = "HP ${member.hero.maxHp} · ATK $attack with ${GameText.name(member.weapon.id)}",
        lines = buildList {
            add(GameText.name(member.weapon.id).uppercase())
            add("  " + GameText.weaponBlurb(member.weapon.id))
            addAll(GameText.describeAbility(member.weapon.grantedAbility, attack).map { "  $it" })
            add(GameText.name(member.offhand.id).uppercase())
            add("  " + GameText.offhandBlurb(member.offhand.id))
            addAll(GameText.describeAbility(member.offhand.grantedAbility, attack).map { "  $it" })
            add("ULTIMATE: " + GameText.name(ultimate.id).uppercase())
            addAll(GameText.describeAbility(ultimate, attack).map { "  $it" })
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroLoadoutCard(
    member: Loadout,
    onLoadoutChange: (Loadout) -> Unit,
    onInfo: (InfoContent) -> Unit
) {
    val attack = member.hero.baseAttack + member.weapon.attackBonus

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onInfo(heroInfo(member)) }
            )
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GameIcons.heroColor(member.hero.iconId) ?: Accent)
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(member.hero.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPip("HP ${member.hero.maxHp}")
                    StatPip("ATK $attack")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        PickerRow(
            options = member.hero.weaponIds.map { id ->
                id to Weapons.REGISTRY.getValue(id).iconId
            },
            selectedId = member.weapon.id,
            onPick = { id -> onLoadoutChange(member.copy(weapon = Weapons.REGISTRY.getValue(id))) },
            onHold = { id ->
                val weapon = Weapons.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = "Weapon" + if (weapon.attackBonus > 0) " · +${weapon.attackBonus} ATK" else "",
                        lines = listOf(GameText.weaponBlurb(id)) +
                            GameText.describeAbility(
                                weapon.grantedAbility,
                                member.hero.baseAttack + weapon.attackBonus
                            )
                    )
                )
            }
        )
        Spacer(Modifier.height(8.dp))
        PickerRow(
            options = member.hero.offhandIds.map { id ->
                id to Offhands.REGISTRY.getValue(id).iconId
            },
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
