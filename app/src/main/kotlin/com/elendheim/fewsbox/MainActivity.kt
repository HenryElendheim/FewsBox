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
import com.elendheim.fewsbox.ui.SaveStore
import com.elendheim.fewsbox.ui.battle.BattleScreen
import com.elendheim.fewsbox.ui.battle.BattleViewModel
import com.elendheim.fewsbox.ui.loadout.LoadoutScreen
import com.elendheim.fewsbox.ui.results.HeroResult
import com.elendheim.fewsbox.ui.results.ResultsScreen
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

private enum class Screen { LOADOUT, BATTLE, RESULTS }

@Composable
fun FewsBoxApp(vm: BattleViewModel = viewModel()) {
    val context = LocalContext.current
    val saved = remember { SaveStore.load(context) }

    var screen by remember { mutableStateOf(Screen.LOADOUT) }
    var maxUnlocked by remember { mutableIntStateOf(saved.maxUnlocked) }
    var selectedLevel by remember { mutableIntStateOf(saved.selectedLevel) }
    var bestStars by remember { mutableStateOf(saved.bestStars) }
    var heroXp by remember { mutableStateOf(saved.heroXp) }
    var roster by remember { mutableStateOf(saved.roster) }
    var selectedIds by remember { mutableStateOf(saved.selectedIds) }
    var unlockedIds by remember { mutableStateOf(saved.unlockedIds) }
    var endlessBest by remember { mutableIntStateOf(saved.endlessBest) }
    var battleLevel by remember { mutableIntStateOf(0) }
    var lastStars by remember { mutableIntStateOf(0) }
    var lastResults by remember { mutableStateOf<List<HeroResult>>(emptyList()) }
    var lastUnlockName by remember { mutableStateOf<String?>(null) }

    val heroLevels = remember(heroXp) { heroXp.mapValues { Progression.levelFor(it.value) } }
    val campaignBeaten = (bestStars[Battles.FINAL_BOSS_INDEX] ?: 0) > 0

    // Write-through save: any change persists.
    LaunchedEffect(maxUnlocked, selectedLevel, bestStars, heroXp, roster, selectedIds, unlockedIds, endlessBest) {
        SaveStore.save(
            context, maxUnlocked, selectedLevel, bestStars, heroXp,
            selectedIds, roster, unlockedIds, endlessBest
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
            onFight = {
                val party = roster.filter { member -> member.hero.id in selectedIds }
                battleLevel = selectedLevel
                vm.startLevel(selectedLevel, party, heroLevels)
                screen = Screen.BATTLE
            },
            endlessBest = endlessBest,
            endlessUnlocked = campaignBeaten,
            onPlayEndless = {
                val party = roster.filter { member -> member.hero.id in selectedIds }
                battleLevel = Battles.CAMPAIGN_LEVELS + endlessBest
                vm.startLevel(battleLevel, party, heroLevels)
                screen = Screen.BATTLE
            }
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
                if (!isEndless) {
                    bestStars = bestStars + (battleLevel to maxOf(bestStars[battleLevel] ?: 0, stars))
                }

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
            onDefeat = { screen = Screen.LOADOUT }
        )

        Screen.RESULTS -> ResultsScreen(
            stars = lastStars,
            heroResults = lastResults,
            unlockedHeroName = lastUnlockName,
            onContinue = { screen = Screen.LOADOUT }
        )
    }
}
