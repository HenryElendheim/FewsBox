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
import androidx.compose.foundation.layout.width
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
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Offhands
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Progression
import com.elendheim.fewsbox.data.Ultimates
import com.elendheim.fewsbox.data.Weapons
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.GameText
import com.elendheim.fewsbox.ui.IconChip
import com.elendheim.fewsbox.ui.InfoContent
import com.elendheim.fewsbox.ui.InfoOverlay
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/**
 * The campaign hub: pick a level from the grid (stars show your best),
 * pick who fights, and pick each hero's kit from whatever their level has
 * unlocked. Long-press anything to read what it does.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoadoutScreen(
    roster: List<Loadout>,
    selectedIds: Set<String>,
    heroLevels: Map<String, Int>,
    heroXp: Map<String, Int>,
    lockedCount: Int,
    maxUnlocked: Int,
    selectedLevel: Int,
    bestStars: Map<Int, Int>,
    onSelectLevel: (Int) -> Unit,
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
            Spacer(Modifier.height(4.dp))
            Text(
                "${bestStars.values.sum()}/${Battles.count * 3} STARS",
                color = EnergyGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(14.dp))

            // The level grid: 25 levels, five per row. Tap to pick.
            LevelGrid(
                maxUnlocked = maxUnlocked,
                selectedLevel = selectedLevel,
                bestStars = bestStars,
                onSelectLevel = onSelectLevel
            )

            Spacer(Modifier.height(18.dp))

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
                                onLongClick = {
                                    info = heroInfo(member, heroLevels[member.hero.id] ?: 1)
                                }
                            )
                    )
                }
                repeat(lockedCount) {
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
                                        lines = listOf("Something gray waits deeper in the campaign.")
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            for (member in roster.filter { it.hero.id in selectedIds }) {
                HeroLoadoutCard(
                    member = member,
                    level = heroLevels[member.hero.id] ?: 1,
                    xp = heroXp[member.hero.id] ?: 0,
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
                Text(
                    "FIGHT LEVEL ${selectedLevel + 1}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp
                )
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
private fun LevelGrid(
    maxUnlocked: Int,
    selectedLevel: Int,
    bestStars: Map<Int, Int>,
    onSelectLevel: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in (0 until Battles.count).chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (level in row) {
                    val unlocked = level <= maxUnlocked
                    val selected = level == selectedLevel
                    val isBoss = Battles.unlocks.containsKey(level)
                    val stars = bestStars[level] ?: 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                2.dp,
                                if (selected) Accent else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .background(if (unlocked) Panel else Ink)
                            .alpha(if (unlocked) 1f else 0.35f)
                            .combinedClickable(
                                enabled = unlocked,
                                onClick = { onSelectLevel(level) },
                                onLongClick = {}
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            "${level + 1}",
                            color = when {
                                isBoss -> Color(0xFFF7F7F7)
                                unlocked -> TextBright
                                else -> TextMuted
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { i ->
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (i < stars) EnergyGold else PanelRaised)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun heroInfo(member: Loadout, level: Int): InfoContent {
    val attack = member.hero.baseAttack + Progression.bonusAttack(level) + member.weapon.attackBonus
    val hp = member.hero.maxHp + Progression.bonusHp(level)
    val ultimate = Ultimates.REGISTRY.getValue(member.hero.ultimateId)
    return InfoContent(
        title = "${member.hero.name}  LV $level",
        subtitle = "HP $hp · ATK $attack with ${GameText.name(member.weapon.id)}",
        lines = buildList {
            add(GameText.name(member.weapon.id).uppercase())
            add("  " + GameText.weaponBlurb(member.weapon.id))
            addAll(GameText.describeAbility(member.weapon.grantedAbility, attack).map { "  $it" })
            add(GameText.name(member.offhand.id).uppercase())
            add("  " + GameText.offhandBlurb(member.offhand.id))
            addAll(GameText.describeAbility(member.offhand.grantedAbility, attack).map { "  $it" })
            add("ULTIMATE: " + GameText.name(ultimate.id).uppercase())
            addAll(GameText.describeAbility(ultimate, attack, isUltimate = true).map { "  $it" })
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroLoadoutCard(
    member: Loadout,
    level: Int,
    xp: Int,
    onLoadoutChange: (Loadout) -> Unit,
    onInfo: (InfoContent) -> Unit
) {
    val attack = member.hero.baseAttack + Progression.bonusAttack(level) + member.weapon.attackBonus
    val hp = member.hero.maxHp + Progression.bonusHp(level)
    val toNext = Progression.xpToNext(xp)

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
                onLongClick = { onInfo(heroInfo(member, level)) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.hero.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "LV $level",
                        color = HpGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPip("HP $hp")
                    StatPip("ATK $attack")
                    StatPip(if (toNext != null) "$toNext XP TO LV ${level + 1}" else "MAX LEVEL")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GatedPickerRow(
            allIds = member.hero.weaponIds,
            unlockedIds = Progression.unlockedWeapons(member.hero, level),
            iconFor = { Weapons.REGISTRY.getValue(it).iconId },
            selectedId = member.weapon.id,
            unlockLevelFor = { Progression.weaponUnlockLevel(it) },
            onPick = { id -> onLoadoutChange(member.copy(weapon = Weapons.REGISTRY.getValue(id))) },
            onHold = { id, unlocked, unlockLevel ->
                val weapon = Weapons.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = if (unlocked) {
                            "Weapon" + if (weapon.attackBonus > 0) " · +${weapon.attackBonus} ATK" else ""
                        } else "Weapon · unlocks at level $unlockLevel",
                        lines = listOf(GameText.weaponBlurb(id)) +
                            GameText.describeAbility(
                                weapon.grantedAbility,
                                member.hero.baseAttack + Progression.bonusAttack(level) + weapon.attackBonus
                            )
                    )
                )
            }
        )
        Spacer(Modifier.height(8.dp))
        GatedPickerRow(
            allIds = member.hero.offhandIds,
            unlockedIds = Progression.unlockedOffhands(member.hero, level),
            iconFor = { Offhands.REGISTRY.getValue(it).iconId },
            selectedId = member.offhand.id,
            unlockLevelFor = { Progression.offhandUnlockLevel(it) },
            onPick = { id -> onLoadoutChange(member.copy(offhand = Offhands.REGISTRY.getValue(id))) },
            onHold = { id, unlocked, unlockLevel ->
                val offhand = Offhands.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = if (unlocked) "Offhand" else "Offhand · unlocks at level $unlockLevel",
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
private fun GatedPickerRow(
    allIds: List<String>,
    unlockedIds: List<String>,
    iconFor: (String) -> String,
    selectedId: String,
    unlockLevelFor: (Int) -> Int,
    onPick: (String) -> Unit,
    onHold: (id: String, unlocked: Boolean, unlockLevel: Int) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((index, id) in allIds.withIndex()) {
            val unlocked = id in unlockedIds
            val selected = id == selectedId
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        if (selected) EnergyGold else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .alpha(if (unlocked) 1f else 0.3f)
                    .combinedClickable(
                        onClick = { if (unlocked) onPick(id) },
                        onLongClick = { onHold(id, unlocked, unlockLevelFor(index)) }
                    )
                    .padding(3.dp)
            ) {
                IconChip(iconFor(id), size = 44)
            }
        }
    }
}
