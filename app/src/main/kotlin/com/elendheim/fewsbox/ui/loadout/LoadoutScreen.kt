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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.elendheim.fewsbox.engine.ability.Effect
import kotlin.math.roundToInt
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
import com.elendheim.fewsbox.ui.Prefs
import com.elendheim.fewsbox.ui.shop.Shop
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
    onFight: () -> Unit,
    endlessBest: Int = 0,
    endlessUnlocked: Boolean = false,
    onPlayEndless: () -> Unit = {},
    fews: Int = 0,
    ownedGear: Set<String> = emptySet(),
    onOpenShop: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenEquipment: () -> Unit = {}
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
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "${bestStars.values.sum()}/${Battles.count * 3} STARS",
                    color = EnergyGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "$fews FEWS",
                    color = TextBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.combinedClickable(onClick = onOpenShop, onLongClick = {})
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenShop,
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextBright),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("SHOP", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 3.sp)
                }
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextBright),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
                }
            }
            if (!Prefs.shopHintSeen && fews >= 80) {
                Text(
                    "You can afford something new - check the shop",
                    color = EnergyGold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // The level grid: 100 levels, five per row, in its own scroll
            // window that opens on wherever you're up to.
            LevelGrid(
                maxUnlocked = maxUnlocked,
                selectedLevel = selectedLevel,
                bestStars = bestStars,
                onSelectLevel = onSelectLevel
            )

            if (endlessUnlocked) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPlayEndless,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanelRaised,
                        contentColor = EnergyGold
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        if (endlessBest > 0) "ENDLESS - LEVEL ${endlessBest + 1} - BEST $endlessBest"
                        else "ENDLESS - THE CAMPAIGN IS BEATEN",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "PARTY ${selectedIds.size}/${Party.MAX_SIZE}",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

            // The hub stays clean: name, level, health, and what one swing
            // of the equipped weapon actually does. Gear lives on its own page.
            for (member in roster.filter { it.hero.id in selectedIds }) {
                val level = heroLevels[member.hero.id] ?: 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .combinedClickable(
                            onClick = onOpenEquipment,
                            onLongClick = { info = heroInfo(member, level) }
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GameIcons.heroColor(member.hero.iconId) ?: Accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.hero.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("LV $level", color = HpGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            "HP ${member.hero.maxHp + Progression.bonusHp(level)} · " +
                                "${GameText.name(member.weapon.id).uppercase()} HITS ${weaponDamage(member, level)}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onOpenEquipment,
                colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextBright),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("EQUIPMENT", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(8.dp))
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
    // The board shows 25 levels at a time; swipe sideways for the next
    // page. It opens on whichever page holds the level you're up to.
    val pageCount = (Battles.count + 24) / 25
    val pager = rememberPagerState(
        initialPage = (selectedLevel / 25).coerceIn(0, pageCount - 1)
    ) { pageCount }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pager) { page ->
            LevelPage(
                levels = (page * 25 until minOf((page + 1) * 25, Battles.count)).toList(),
                maxUnlocked = maxUnlocked,
                selectedLevel = selectedLevel,
                bestStars = bestStars,
                onSelectLevel = onSelectLevel
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pageCount) { i ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) Accent else PanelRaised)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LevelPage(
    levels: List<Int>,
    maxUnlocked: Int,
    selectedLevel: Int,
    bestStars: Map<Int, Int>,
    onSelectLevel: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in levels.chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (level in row) {
                    val unlocked = level <= maxUnlocked
                    val selected = level == selectedLevel
                    val isBoss = Battles.isBossLevel(level)
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

/** One swing of the equipped weapon, as the number it actually lands. */
private fun weaponDamage(member: Loadout, level: Int): String {
    val attack = member.hero.baseAttack + Progression.bonusAttack(level) + member.weapon.attackBonus
    for (effect in member.weapon.grantedAbility.effects) {
        when (effect) {
            is Effect.DealDamage -> {
                val per = (attack * effect.multiplier).roundToInt()
                return if (effect.hits > 1) "${effect.hits} x $per" else "$per"
            }
            is Effect.ScalingStrike -> return "${(attack * effect.multiplier).roundToInt()}+"
            is Effect.CascadeDamage -> return "${(attack * effect.multiplier).roundToInt()}"
            else -> {}
        }
    }
    return "$attack"
}

internal fun heroInfo(member: Loadout, level: Int): InfoContent {
    val attack = member.hero.baseAttack + Progression.bonusAttack(level) + member.weapon.attackBonus
    val hp = member.hero.maxHp + Progression.bonusHp(level)
    val ultimate = Ultimates.forLevel(member.hero.ultimateId, level)
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
