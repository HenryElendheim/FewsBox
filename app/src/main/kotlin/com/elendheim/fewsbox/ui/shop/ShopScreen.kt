package com.elendheim.fewsbox.ui.shop

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

/** Prices and catalog data for everything fews can buy. */
object Shop {
    const val WEAPON_PRICE = 100
    const val OFFHAND_PRICE = 80

    // Workshop consumables: id, name, price, what it does in battle.
    data class ConsumableDef(val id: String, val name: String, val price: Int, val blurb: String)
    val CONSUMABLES = listOf(
        ConsumableDef("con_spark", "Ult Spark", 45, "Adds 15% to the party ult meter, right now."),
        ConsumableDef("con_bandage", "Field Bandage", 35, "Heals every ally 12 on the spot."),
        ConsumableDef("con_ironskin", "Iron Skin", 35, "Every ally gains a shield of 8.")
    )
    val CONSUMABLE_BY_ID = CONSUMABLES.associateBy { it.id }

    fun priceFor(gearId: String): Int =
        if (gearId.startsWith("wpn_")) WEAPON_PRICE else OFFHAND_PRICE
}

private enum class ShopTab { ARMORY, WORKSHOP, WARDROBE, ARCHIVE }

/**
 * The shop: fews go in, options come out. The armory sells every hero's
 * non-starter gear — all sidegrades, so buying is choice, never power.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShopScreen(
    fews: Int,
    roster: List<Loadout>,
    ownedGear: Set<String>,
    consumables: Map<String, Int>,
    onBuyGear: (String) -> Unit,
    onBuyConsumable: (String) -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(ShopTab.ARMORY) }
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
            Text("SHOP", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Text(
                "$fews FEWS",
                color = EnergyGold, fontSize = 12.sp, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (t in ShopTab.entries) {
                    val active = t == tab
                    Text(
                        t.name,
                        color = if (active) Ink else TextMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) Accent else PanelRaised)
                            .combinedClickable(onClick = { tab = t }, onLongClick = {})
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            when (tab) {
                ShopTab.ARMORY -> ArmoryTab(fews, roster, ownedGear, onBuyGear) { info = it }
                ShopTab.WORKSHOP -> WorkshopTab(fews, consumables, onBuyConsumable)
                ShopTab.WARDROBE -> UnderConstruction("New shades and victory animations are being sewn.")
                ShopTab.ARCHIVE -> UnderConstruction("The lore of the grayscale is still being written.")
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("BACK", fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            }
        }
        InfoOverlay(content = info, onDismiss = { info = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArmoryTab(
    fews: Int,
    roster: List<Loadout>,
    ownedGear: Set<String>,
    onBuyGear: (String) -> Unit,
    onInfo: (InfoContent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        for (member in roster) {
            val hero = member.hero
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Panel)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GameIcons.heroColor(hero.iconId) ?: Accent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(hero.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                // Weapons on one shelf, offhands on their own below.
                for (gear in listOf(hero.weaponIds, hero.offhandIds)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (id in gear) {
                        val starter = id == hero.defaultWeaponId || id == hero.defaultOffhandId
                        val owned = starter || id in ownedGear
                        val price = Shop.priceFor(id)
                        val affordable = fews >= price
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (owned) PanelRaised else Ink)
                                .combinedClickable(
                                    onClick = { if (!owned && affordable) onBuyGear(id) },
                                    onLongClick = {
                                        val ability = Weapons.REGISTRY[id]?.grantedAbility
                                            ?: Offhands.REGISTRY.getValue(id).grantedAbility
                                        onInfo(
                                            InfoContent(
                                                title = GameText.name(id),
                                                subtitle = when {
                                                    starter -> "Starter gear"
                                                    owned -> "Owned"
                                                    else -> "$price fews"
                                                },
                                                lines = GameText.describeAbility(ability, hero.baseAttack)
                                            )
                                        )
                                    }
                                )
                                .padding(6.dp)
                        ) {
                            Box(Modifier.alpha(if (owned) 1f else 0.55f)) {
                                IconChip(
                                    Weapons.REGISTRY[id]?.iconId ?: Offhands.REGISTRY.getValue(id).iconId,
                                    size = 42
                                )
                            }
                            Text(
                                if (owned) "OWNED" else "$price",
                                color = when {
                                    owned -> HpGreen
                                    affordable -> EnergyGold
                                    else -> TextMuted
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                }
            }
        }
        Text(
            "Every piece is a sidegrade. Buying opens options, never raw power. Hold anything to read it.",
            color = TextMuted, fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkshopTab(
    fews: Int,
    consumables: Map<String, Int>,
    onBuyConsumable: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        for (def in Shop.CONSUMABLES) {
            val ownedCount = consumables[def.id] ?: 0
            val affordable = fews >= def.price
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Panel)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(def.name, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(def.blurb, color = TextMuted, fontSize = 11.sp)
                    Text(
                        "You own $ownedCount · bring up to 5 into a battle, one use per turn each",
                        color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { if (affordable) onBuyConsumable(def.id) },
                    enabled = affordable,
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = EnergyGold)
                ) {
                    Text("${def.price}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun UnderConstruction(note: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "UNDER CONSTRUCTION",
            color = TextBright, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
        )
        Text(note, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
