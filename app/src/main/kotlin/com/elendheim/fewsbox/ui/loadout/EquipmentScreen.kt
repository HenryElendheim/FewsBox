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
import com.elendheim.fewsbox.data.Progression
import com.elendheim.fewsbox.data.Weapons
import com.elendheim.fewsbox.ui.GameIcons
import com.elendheim.fewsbox.ui.GameText
import com.elendheim.fewsbox.ui.IconChip
import com.elendheim.fewsbox.ui.InfoContent
import com.elendheim.fewsbox.ui.InfoOverlay
import com.elendheim.fewsbox.ui.shop.Shop
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/**
 * The equipment room: every hero you have, their whole arsenal, pick what
 * they carry. Locked pieces show their shop price; long-press reads
 * anything with real numbers.
 */
@Composable
fun EquipmentScreen(
    roster: List<Loadout>,
    heroLevels: Map<String, Int>,
    heroXp: Map<String, Int>,
    ownedGear: Set<String>,
    onLoadoutChange: (Loadout) -> Unit,
    onBack: () -> Unit
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
            Text("EQUIPMENT", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Spacer(Modifier.height(16.dp))

            for (member in roster) {
                HeroLoadoutCard(
                    member = member,
                    level = heroLevels[member.hero.id] ?: 1,
                    xp = heroXp[member.hero.id] ?: 0,
                    ownedGear = ownedGear,
                    onLoadoutChange = onLoadoutChange,
                    onInfo = { info = it }
                )
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("BACK", fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text("Hold anything to see what it does", color = TextMuted, fontSize = 11.sp)
        }
        InfoOverlay(content = info, onDismiss = { info = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroLoadoutCard(
    member: Loadout,
    level: Int,
    xp: Int,
    ownedGear: Set<String>,
    onLoadoutChange: (Loadout) -> Unit,
    onInfo: (InfoContent) -> Unit
) {
    fun owns(id: String) =
        id == member.hero.defaultWeaponId || id == member.hero.defaultOffhandId || id in ownedGear
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
            unlockedIds = member.hero.weaponIds.filter { owns(it) },
            iconFor = { Weapons.REGISTRY.getValue(it).iconId },
            selectedId = member.weapon.id,
            onPick = { id -> onLoadoutChange(member.copy(weapon = Weapons.REGISTRY.getValue(id))) },
            onHold = { id, unlocked ->
                val weapon = Weapons.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = if (unlocked) {
                            "Weapon" + if (weapon.attackBonus > 0) " · +${weapon.attackBonus} ATK" else ""
                        } else "Weapon · ${Shop.WEAPON_PRICE} fews in the shop",
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
            unlockedIds = member.hero.offhandIds.filter { owns(it) },
            iconFor = { Offhands.REGISTRY.getValue(it).iconId },
            selectedId = member.offhand.id,
            onPick = { id -> onLoadoutChange(member.copy(offhand = Offhands.REGISTRY.getValue(id))) },
            onHold = { id, unlocked ->
                val offhand = Offhands.REGISTRY.getValue(id)
                onInfo(
                    InfoContent(
                        title = GameText.name(id),
                        subtitle = if (unlocked) "Offhand" else "Offhand · ${Shop.OFFHAND_PRICE} fews in the shop",
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
    onPick: (String) -> Unit,
    onHold: (id: String, unlocked: Boolean) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (id in allIds) {
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
                        onLongClick = { onHold(id, unlocked) }
                    )
                    .padding(3.dp)
            ) {
                IconChip(iconFor(id), size = 44)
            }
        }
    }
}
