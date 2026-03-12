package com.ramble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramble.app.RambleApp
import com.ramble.app.settings.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onClearApiKey: () -> Unit
) {
    val settingsManager = RambleApp.instance.settingsManager
    val settings by settingsManager.settings.collectAsState()
    val apiKey by RambleApp.instance.apiKeyManager.apiKey.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var wordContext by remember { mutableStateOf(settings.wordContext) }

    LaunchedEffect(settings.wordContext) {
        wordContext = settings.wordContext
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // API Key section
        Text(
            text = "API Key",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Soniox API Key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (apiKey != null) "${"*".repeat(8)}${apiKey!!.takeLast(4)}" else "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Voice settings section
        Text(
            text = "Voice Settings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Language Hints",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select languages to improve recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val selectedNames = settings.languageHints.mapNotNull { code ->
                        SettingsManager.SUPPORTED_LANGUAGES.find { it.first == code }?.second
                    }
                    Text(
                        text = if (selectedNames.size <= 2) selectedNames.joinToString(", ")
                               else "${selectedNames.take(2).joinToString(", ")}+${selectedNames.size - 2}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Word Context",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Specific words, names, or terms to recognize (optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = wordContext,
                        onValueChange = { wordContext = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "e.g., Ramble, Soniox, specific names...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { settingsManager.updateWordContext(wordContext) },
                        enabled = wordContext != settings.wordContext,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClearApiKey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Remove API Key", modifier = Modifier.padding(vertical = 4.dp))
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Languages") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select one or more languages for better recognition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SettingsManager.SUPPORTED_LANGUAGES.forEach { (code, name) ->
                        val isSelected = settings.languageHints.contains(code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settingsManager.toggleLanguageHint(code)
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name)
                            if (isSelected) {
                                Text(
                                    "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
