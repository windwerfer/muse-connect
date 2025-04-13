package com.windwerfer.museconnect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.windwerfer.museconnect.data.ConfigManager
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    museDevices: List<String>,
    selectedDevice: String?,
    onDeviceSelected: (String) -> Unit,
    configManager: ConfigManager,
    debugMessages: List<String>
) {
    var eegChecked by remember { mutableStateOf(configManager.getEegEnabled()) }
    var accChecked by remember { mutableStateOf(configManager.getAccEnabled()) }
    var ppgChecked by remember { mutableStateOf(configManager.getPpgEnabled()) }
    var debugChecked by remember { mutableStateOf(configManager.getDebugEnabled()) }

    var eegValue by remember { mutableStateOf("N/A") }
    var accValue by remember { mutableStateOf("N/A") }
    var ppgValue by remember { mutableStateOf("N/A") }

    val scope = rememberCoroutineScope()

    // Simulate value updates every 0.5s
    LaunchedEffect(eegChecked, accChecked, ppgChecked) {
        while (true) {
            if (eegChecked) eegValue = "EEG: ${Math.random()}" // Replace with real data
            if (accChecked) accValue = "ACC: ${Math.random()}"
            if (ppgChecked) ppgValue = "PPG: ${Math.random()}"
            delay(500)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Spinner for Muse devices
        Spinner(
            items = museDevices,
            selectedItem = selectedDevice,
            onItemSelected = { device ->
                onDeviceSelected(device)
                configManager.setLastConnectedDevice(device)
            }
        )

        // Checkboxes with accordion
        CheckboxWithAccordion(
            label = "EEG",
            checked = eegChecked,
            onCheckedChange = {
                eegChecked = it
                configManager.setEegEnabled(it)
            },
            value = if (eegChecked) eegValue else null
        )
        CheckboxWithAccordion(
            label = "Accelerometer",
            checked = accChecked,
            onCheckedChange = {
                accChecked = it
                configManager.setAccEnabled(it)
            },
            value = if (accChecked) accValue else null
        )
        CheckboxWithAccordion(
            label = "PPG",
            checked = ppgChecked,
            onCheckedChange = {
                ppgChecked = it
                configManager.setPpgEnabled(it)
            },
            value = if (ppgChecked) ppgValue else null
        )
        CheckboxWithAccordion(
            label = "Debug",
            checked = debugChecked,
            onCheckedChange = {
                debugChecked = it
                configManager.setDebugEnabled(it)
            },
            value = if (debugChecked) debugMessages.joinToString("\n") else null
        )
    }
}

@Composable
fun Spinner(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedItem ?: "Select Muse Device")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CheckboxWithAccordion(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String?
) {
    Column {
        Row {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(text = label, modifier = Modifier.padding(top = 12.dp))
        }
        if (checked && value != null) {
            Text(
                text = value,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}