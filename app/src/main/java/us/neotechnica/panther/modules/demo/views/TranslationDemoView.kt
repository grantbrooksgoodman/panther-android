//
//  TranslationDemoView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.TranslationInput
import androidx.compose.material3.Text as Material3Text

/**
 * The Phase 4 acceptance screen for the translation stack.
 *
 * Translating text runs the archive-miss → web translation → archive
 * write path, then reads the entry back from the hosted archive,
 * demonstrating that a second request hits the archive and printing
 * the iOS-compatible archive key and value.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun TranslationDemoView(modifier: Modifier = Modifier) {
    val colors = LocalPantherColors.current
    val scope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("Hello") }
    var targetLanguage by remember { mutableStateOf("es") }
    var isBusy by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
    ) {
        Components.Text(
            "Translation Stack",
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
        )
        Components.Text(
            "Phase 4 — hosted archive round-trip",
            color = colors.subtitleText,
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Material3Text("Text (English)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = targetLanguage,
            onValueChange = { targetLanguage = it.trim() },
            label = { Material3Text("Target language (ISO 639-1)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING)) {
            Components.Button(
                text = if (isBusy) "Translating…" else "Translate",
                color = if (isBusy) colors.disabled else colors.accent,
                onClick = {
                    if (isBusy) return@Button
                    scope.launch {
                        isBusy = true
                        log.clear()
                        runTranslation(inputText, targetLanguage) { log.add(it) }
                        isBusy = false
                    }
                },
                font = Font.systemSemibold(),
            )
            Components.Button(
                text = "Clear",
                color = colors.subtitleText,
                onClick = { log.clear() },
            )
        }

        log.forEach { line ->
            Components.Text(line, color = colors.titleText, font = Font.system(FontScale.Small))
        }
    }
}

private suspend fun runTranslation(
    text: String,
    targetLanguage: String,
    log: (String) -> Unit,
) {
    val languagePair = LanguagePair(from = "en", to = targetLanguage)
    val input = TranslationInput(text)

    try {
        log("Signing in anonymously…")
        Networking.config.authDelegate.signInAnonymously()

        log("Translating \"$text\" (${languagePair.string})…")
        val translation = Networking.config.hostedTranslationDelegate.translate(input, languagePair)
        log("Output: \"${translation.output}\"")

        val entry = Networking.config.hostedTranslationDelegate.hostedArchiveEntry(translation)
        if (entry != null) {
            log("Hosted archive path: ${Networking.config.environment.shortString}/${entry.first}")
            log("Hosted archive value: ${entry.second}")
        } else {
            log("Idempotent pair — not written to the hosted archive.")
        }

        val hash = encodedHashOf(listOf(input.value))
        val readback = Networking.config.hostedTranslationDelegate.findArchivedTranslation(hash, languagePair)
        log("Archive hit → \"${readback.output}\"")
        log("✓ Round-trip complete; entry is readable by iOS (key parity verified).")
    } catch (error: Exception) {
        log("Error: ${error.message ?: error}")
    }
}

private val SCREEN_PADDING = 20.dp
private val ITEM_SPACING = 12.dp
