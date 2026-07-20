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

private enum class Screen { LOADOUT, BATTLE }

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

    val heroLevels = remember(heroXp) { heroXp.mapValues { Progression.levelFor(it.value) } }

    // Write-through save: any change persists.
    LaunchedEffect(maxUnlocked, selectedLevel, bestStars, heroXp, roster, selectedIds, unlockedIds) {
        SaveStore.save(
            context, maxUnlocked, selectedLevel, bestStars, heroXp,
            selectedIds, roster, unlockedIds
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
                vm.startBattle(selectedLevel, party, heroLevels)
                screen = Screen.BATTLE
            }
        )

        Screen.BATTLE -> BattleScreen(
            vm = vm,
            onVictory = { survivors ->
                // Stars: everyone standing is 3, each loss costs one.
                val partySize = selectedIds.size
                val stars = (3 - (partySize - survivors)).coerceIn(1, 3)
                bestStars = bestStars + (selectedLevel to maxOf(bestStars[selectedLevel] ?: 0, stars))

                // Everyone who fought gets paid.
                val reward = Progression.xpRewardFor(selectedLevel)
                heroXp = heroXp.mapValues { (id, xp) ->
                    if (id in selectedIds) xp + reward else xp
                }

                // A fallen boss defects and joins the roster.
                Battles.unlocks[selectedLevel]?.let { heroId ->
                    if (heroId !in unlockedIds) {
                        unlockedIds = unlockedIds + heroId
                        roster = roster + Party.loadoutFor(heroId)
                    }
                }

                if (selectedLevel == maxUnlocked && maxUnlocked < Battles.count - 1) {
                    maxUnlocked++
                }
                if (selectedLevel < maxUnlocked) selectedLevel = minOf(selectedLevel + 1, maxUnlocked)
                screen = Screen.LOADOUT
            },
            onDefeat = { screen = Screen.LOADOUT }
        )
    }
}
