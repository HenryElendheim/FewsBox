package com.elendheim.fewsbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.fewsbox.data.Battles
import com.elendheim.fewsbox.data.Loadout
import com.elendheim.fewsbox.data.Party
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
    var screen by remember { mutableStateOf(Screen.LOADOUT) }
    var battleIndex by remember { mutableIntStateOf(0) }
    var party by remember { mutableStateOf(Party.defaultParty()) }

    when (screen) {
        Screen.LOADOUT -> LoadoutScreen(
            party = party,
            battleIndex = battleIndex,
            battleCount = Battles.count,
            onLoadoutChange = { changed: Loadout ->
                party = party.map { if (it.unitId == changed.unitId) changed else it }
            },
            onFight = {
                vm.startBattle(battleIndex, party)
                screen = Screen.BATTLE
            }
        )

        Screen.BATTLE -> BattleScreen(
            vm = vm,
            onVictory = {
                if (battleIndex < Battles.count - 1) battleIndex++
                screen = Screen.LOADOUT
            },
            onDefeat = { screen = Screen.LOADOUT }
        )
    }
}
