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
    var battleIndex by remember { mutableIntStateOf(saved.battleIndex) }
    var roster by remember { mutableStateOf(saved.roster) }
    var selectedIds by remember { mutableStateOf(saved.selectedIds) }
    var unlockedIds by remember { mutableStateOf(saved.unlockedIds) }

    // Write-through save: any change to progress, party, gear or unlocks persists.
    LaunchedEffect(battleIndex, roster, selectedIds, unlockedIds) {
        SaveStore.save(context, battleIndex, selectedIds, roster, unlockedIds)
    }

    when (screen) {
        Screen.LOADOUT -> LoadoutScreen(
            roster = roster,
            selectedIds = selectedIds,
            silverLocked = Party.SILVER_ID !in unlockedIds,
            battleIndex = battleIndex,
            battleCount = Battles.count,
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
                vm.startBattle(battleIndex, party)
                screen = Screen.BATTLE
            }
        )

        Screen.BATTLE -> BattleScreen(
            vm = vm,
            onVictory = {
                if (battleIndex < Battles.count - 1) {
                    battleIndex++
                } else if (Party.SILVER_ID !in unlockedIds) {
                    // Campaign beaten: Silver defects and joins the roster.
                    unlockedIds = unlockedIds + Party.SILVER_ID
                    roster = roster + Party.silverLoadout()
                }
                screen = Screen.LOADOUT
            },
            onDefeat = { screen = Screen.LOADOUT }
        )
    }
}
