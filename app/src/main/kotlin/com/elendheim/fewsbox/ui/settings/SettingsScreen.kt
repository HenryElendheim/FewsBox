package com.elendheim.fewsbox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.fewsbox.ui.Prefs
import com.elendheim.fewsbox.ui.SaveStore
import com.elendheim.fewsbox.ui.theme.Accent
import com.elendheim.fewsbox.ui.theme.EnergyGold
import com.elendheim.fewsbox.ui.theme.HpGreen
import com.elendheim.fewsbox.ui.theme.Ink
import com.elendheim.fewsbox.ui.theme.Panel
import com.elendheim.fewsbox.ui.theme.PanelRaised
import com.elendheim.fewsbox.ui.theme.TextBright
import com.elendheim.fewsbox.ui.theme.TextMuted

/**
 * Accessibility switches and the save vault: everything persists the
 * moment it changes, and the whole save can leave as a code and come
 * back on another phone.
 */
@Composable
fun SettingsScreen(
    onImported: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var exportCode by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<Boolean?>(null) }

    fun toggleSaved(block: () -> Unit) {
        block()
        Prefs.save(context)
    }

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
            Text("SETTINGS", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            Spacer(Modifier.height(16.dp))

            SettingToggle(
                title = "Reduce motion",
                blurb = "No screen shake, no lunging cards - numbers and washes only",
                checked = Prefs.reduceMotion,
                onToggle = { toggleSaved { Prefs.reduceMotion = !Prefs.reduceMotion } }
            )
            SettingToggle(
                title = "Big combat numbers",
                blurb = "Damage and healing numbers render larger in battle",
                checked = Prefs.bigNumbers,
                onToggle = { toggleSaved { Prefs.bigNumbers = !Prefs.bigNumbers } }
            )
            SettingToggle(
                title = "High contrast text",
                blurb = "Brighter secondary text on dark panels",
                checked = Prefs.highContrast,
                onToggle = { toggleSaved { Prefs.highContrast = !Prefs.highContrast } }
            )
            SettingToggle(
                title = "Slower enemy turns",
                blurb = "A longer beat between enemy actions so rounds are easier to follow",
                checked = Prefs.slowEnemies,
                onToggle = { toggleSaved { Prefs.slowEnemies = !Prefs.slowEnemies } }
            )
            SettingToggle(
                title = "Replay the tutorial",
                blurb = "Show the level 1 teaching hints again",
                checked = !Prefs.tutorialDone,
                onToggle = { toggleSaved { Prefs.tutorialDone = !Prefs.tutorialDone } }
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "SAVE VAULT",
                color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Panel)
                    .padding(14.dp)
            ) {
                Text(
                    "Export copies your whole save as a code. Keep it somewhere safe and " +
                        "paste it back in on any phone to pick up where you left off.",
                    color = TextMuted, fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val code = SaveStore.exportCode(context)
                        exportCode = code
                        clipboard.setText(AnnotatedString(code))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = EnergyGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EXPORT SAVE - COPY CODE", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                if (exportCode != null) {
                    Text(
                        "Copied to clipboard (${exportCode!!.length} characters)",
                        color = HpGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it; importResult = null },
                    label = { Text("Paste a save code to import", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val ok = importText.isNotBlank() && SaveStore.importCode(context, importText)
                        importResult = ok
                        if (ok) {
                            Prefs.load(context)
                            onImported()
                        }
                    },
                    enabled = importText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("IMPORT SAVE", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                when (importResult) {
                    true -> Text(
                        "Save imported", color = HpGreen, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)
                    )
                    false -> Text(
                        "That code didn't read as a save - check it copied fully",
                        color = Accent, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)
                    )
                    null -> {}
                }
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
    }
}

@Composable
private fun SettingToggle(
    title: String,
    blurb: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .clickable { onToggle() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(blurb, color = TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
        // A plain pill switch: filled gold when on, hollow when off.
        Box(
            Modifier
                .width(44.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (checked) EnergyGold else PanelRaised),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Ink)
            )
        }
    }
}
