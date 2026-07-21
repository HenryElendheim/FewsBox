package com.elendheim.fewsbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Party
import com.elendheim.fewsbox.data.Progression
import com.elendheim.fewsbox.ui.Prefs
import com.elendheim.fewsbox.ui.SaveStore
import com.elendheim.fewsbox.ui.settings.SettingsScreen
import com.elendheim.fewsbox.ui.battle.BattleScreen
import com.elendheim.fewsbox.ui.battle.BattleViewModel
import com.elendheim.fewsbox.ui.loadout.LoadoutScreen
import com.elendheim.fewsbox.ui.results.HeroResult
import com.elendheim.fewsbox.ui.results.ResultsScreen
import com.elendheim.fewsbox.ui.shop.Shop
import com.elendheim.fewsbox.ui.shop.ShopScreen
import com.elendheim.fewsbox.ui.theme.FewsBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FewsBoxTheme {
                Surface(Modifier.fillMaxSize()) {
                    FewsBoxApp()
                }
            }
        }
    }
}

private enum class Screen { LOADOUT, BATTLE, RESULTS, SHOP, SETTINGS }

@Composable
fun FewsBoxApp(vm: BattleViewModel = viewModel()) {
    val context = LocalContext.current
    // Bumping saveVersion re-reads everything from disk — that is how an
    // imported save takes over the running app.
    var saveVersion by remember { mutableIntStateOf(0) }
    remember { Prefs.load(context); true }
    val saved = remember(saveVersion) { SaveStore.load(context) }

    var screen by remember { mutableStateOf(Screen.LOADOUT) }
    var maxUnlocked by remember(saveVersion) { mutableIntStateOf(saved.maxUnlocked) }
    var selectedLevel by remember(saveVersion) { mutableIntStateOf(saved.selectedLevel) }
    var bestStars by remember(saveVersion) { mutableStateOf(saved.bestStars) }
    var heroXp by remember(saveVersion) { mutableStateOf(saved.heroXp) }
    var roster by remember(saveVersion) { mutableStateOf(saved.roster) }
    var selectedIds by remember(saveVersion) { mutableStateOf(saved.selectedIds) }
    var unlockedIds by remember(saveVersion) { mutableStateOf(saved.unlockedIds) }
    var endlessBest by remember(saveVersion) { mutableIntStateOf(saved.endlessBest) }
    var fews by remember(saveVersion) { mutableIntStateOf(saved.fews) }
    var ownedGear by remember(saveVersion) { mutableStateOf(saved.ownedGear) }
    var consumables by remember(saveVersion) { mutableStateOf(saved.consumables) }
    var battleLevel by remember { mutableIntStateOf(0) }
    var lastStars by remember { mutableIntStateOf(0) }
    var lastResults by remember { mutableStateOf<List<HeroResult>>(emptyList()) }
    var lastUnlockName by remember { mutableStateOf<String?>(null) }
    var lastFewsPayouts by remember { mutableStateOf<List<Int>>(emptyList()) }
    var lastFewsTotal by remember { mutableIntStateOf(0) }

    val heroLevels = remember(heroXp) { heroXp.mapValues { Progression.levelFor(it.value) } }
    val campaignBeaten = (bestStars[Battles.FINAL_BOSS_INDEX] ?: 0) > 0

    fun startFight(level: Int) {
        val party = roster.filter { member -> member.hero.id in selectedIds }
        battleLevel = level
        vm.bringConsumables(consumables.mapValues { minOf(5, it.value) })
        vm.startLevel(level, party, heroLevels)
        screen = Screen.BATTLE
    }

    fun bankConsumables() {
        // Whatever wasn't used in battle goes back on the shelf.
        val remaining = vm.consumablesRemaining()
        consumables = consumables.mapValues { (id, count) ->
            val brought = minOf(5, count)
            (count - brought) + (remaining[id] ?: 0)
        }
    }

    // Write-through save: any change persists.
    LaunchedEffect(
        maxUnlocked, selectedLevel, bestStars, heroXp, roster, selectedIds,
        unlockedIds, endlessBest, fews, ownedGear, consumables
    ) {
        SaveStore.save(
            context, maxUnlocked, selectedLevel, bestStars, heroXp,
            selectedIds, roster, unlockedIds, endlessBest, fews, ownedGear, consumables
        )
    }

    when (screen) {
        Screen.LOADOUT -> LoadoutScreen(
            roster = roster,
            selectedIds = selectedIds,
            heroLevels = heroLevels,
            heroXp = heroXp,
            lockedCount = Party.UNLOCKABLE_IDS.count { it !in unlockedIds },
            maxUnlocked = maxUnlocked,
            selectedLevel = selectedLevel,
            bestStars = bestStars,
            onSelectLevel = { level -> if (level <= maxUnlocked) selectedLevel = level },
            onToggleHero = { heroId ->
                selectedIds = when {
                    heroId in selectedIds && selectedIds.size > 1 -> selectedIds - heroId
                    heroId !in selectedIds && selectedIds.size < Party.MAX_SIZE -> selectedIds + heroId
                    else -> selectedIds
                }
            },
            onLoadoutChange = { changed: Loadout ->
                roster = roster.map { if (it.hero.id == changed.hero.id) changed else it }
            },
            onFight = { startFight(selectedLevel) },
            endlessBest = endlessBest,
            endlessUnlocked = campaignBeaten,
            onPlayEndless = { startFight(Battles.CAMPAIGN_LEVELS + endlessBest) },
            fews = fews,
            ownedGear = ownedGear,
            onOpenShop = {
                if (!Prefs.shopHintSeen) {
                    Prefs.shopHintSeen = true
                    Prefs.save(context)
                }
                screen = Screen.SHOP
            },
            onOpenSettings = { screen = Screen.SETTINGS }
        )

        Screen.SETTINGS -> SettingsScreen(
            onImported = {
                saveVersion++
                screen = Screen.LOADOUT
            },
            onBack = { screen = Screen.LOADOUT }
        )

        Screen.SHOP -> ShopScreen(
            fews = fews,
            roster = roster,
            ownedGear = ownedGear,
            consumables = consumables,
            onBuyGear = { id ->
                val price = Shop.priceFor(id)
                if (fews >= price && id !in ownedGear) {
                    fews -= price
                    ownedGear = ownedGear + id
                }
            },
            onBuyConsumable = { id ->
                val price = Shop.CONSUMABLE_BY_ID[id]?.price ?: return@ShopScreen
                if (fews >= price) {
                    fews -= price
                    consumables = consumables + (id to (consumables[id] ?: 0) + 1)
                }
            },
            onBack = { screen = Screen.LOADOUT }
        )

        Screen.BATTLE -> BattleScreen(
            vm = vm,
            onVictory = {
                val isEndless = battleLevel >= Battles.CAMPAIGN_LEVELS
                val damageByHero = vm.damageByHero()
                val survivorIds = vm.survivors()

                // Stars: everyone standing is 3, each loss costs one. Endless
                // runs are about distance, not stars.
                val stars = if (isEndless) 0 else {
                    (3 - (selectedIds.size - survivorIds.size)).coerceIn(1, 3)
                }
                val firstClear = !isEndless && (bestStars[battleLevel] ?: 0) == 0
                if (!isEndless) {
                    bestStars = bestStars + (battleLevel to maxOf(bestStars[battleLevel] ?: 0, stars))
                }

                // The payday: 15 a star on a first clear with the third star
                // worth 20; replays pay a flat 15 a star; endless pays for
                // distance. Thief drops ride on top.
                val payouts = when {
                    isEndless -> listOf(30)
                    firstClear -> List(stars) { i -> if (i == 2) 20 else 15 }
                    else -> List(stars) { 15 }
                }.toMutableList()
                val stolen = vm.fewsEarned()
                if (stolen > 0) payouts.add(stolen)
                lastFewsPayouts = payouts
                lastFewsTotal = payouts.sum()
                fews += lastFewsTotal
                bankConsumables()

                // XP is earned, not granted: what you dealt this level, plus
                // a flat 5 for walking out alive.
                val oldXp = heroXp
                heroXp = heroXp.mapValues { (id, xp) ->
                    if (id in selectedIds) {
                        xp + (damageByHero[id] ?: 0) + (if (id in survivorIds) 5 else 0)
                    } else xp
                }
                lastStars = stars
                lastResults = roster
                    .filter { it.hero.id in selectedIds }
                    .map { member ->
                        val before = oldXp[member.hero.id] ?: 0
                        val after = heroXp[member.hero.id] ?: 0
                        HeroResult(
                            heroId = member.hero.id,
                            name = member.hero.name,
                            iconId = member.hero.iconId,
                            xpGained = after - before,
                            xpBefore = before,
                            xpAfter = after,
                            levelBefore = Progression.levelFor(before),
                            levelAfter = Progression.levelFor(after)
                        )
                    }

                // A fallen boss defects and joins the roster.
                lastUnlockName = null
                Battles.unlocks[battleLevel]?.let { heroId ->
                    if (heroId !in unlockedIds) {
                        unlockedIds = unlockedIds + heroId
                        val loadout = Party.loadoutFor(heroId)
                        roster = roster + loadout
                        lastUnlockName = loadout.hero.name
                    }
                }

                // Level 1 down means the teaching hints have done their job.
                if (battleLevel == 0 && !Prefs.tutorialDone) {
                    Prefs.tutorialDone = true
                    Prefs.save(context)
                }

                if (isEndless) {
                    endlessBest = maxOf(endlessBest, battleLevel - Battles.CAMPAIGN_LEVELS + 1)
                } else {
                    if (battleLevel == maxUnlocked && maxUnlocked < Battles.count - 1) {
                        maxUnlocked++
                    }
                    if (selectedLevel < maxUnlocked) selectedLevel = minOf(selectedLevel + 1, maxUnlocked)
                }
                screen = Screen.RESULTS
            },
            onDefeat = {
                bankConsumables()
                screen = Screen.LOADOUT
            }
        )

        Screen.RESULTS -> ResultsScreen(
            stars = lastStars,
            heroResults = lastResults,
            unlockedHeroName = lastUnlockName,
            onContinue = { screen = Screen.LOADOUT },
            fewsPayouts = lastFewsPayouts,
            fewsTotal = lastFewsTotal
        )
    }
}
