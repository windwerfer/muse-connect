package com.windwerfer.museconnect.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.windwerfer.museconnect.data.ConfigManager
import com.windwerfer.museconnect.utils.Logging
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    museDevices: List<String>,
    deviceMacs: List<String>,
    selectedDevice: String?,
    onDeviceSelected: (String) -> Unit,
    configManager: ConfigManager
) {
    var eegChecked by remember { mutableStateOf(configManager.getEegEnabled()) }
    var accChecked by remember { mutableStateOf(configManager.getAccEnabled()) }
    var ppgChecked by remember { mutableStateOf(configManager.getPpgEnabled()) }
    var debugChecked by remember { mutableStateOf(configManager.getDebugEnabled()) }

    var eegValue by remember { mutableStateOf("N/A") }
    var accValue by remember { mutableStateOf("N/A") }
    var ppgValue by remember { mutableStateOf("N/A") }

    val debugMessages by Logging.debugMessages.collectAsState()
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
            deviceMacs = deviceMacs,
            selectedItem = selectedDevice,
            onItemSelected = onDeviceSelected
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
            value = if (debugChecked) debugMessages.joinToString("\n") else null,
            isScrollable = debugChecked
        )
    }
}

@Composable
fun Spinner(
    items: List<String>,
    deviceMacs: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = if (selectedItem == null) {
                    "No Devices Found"
                } else {
                    val index = deviceMacs.indexOf(selectedItem)
                    if (index != -1) items[index] else "No Devices Found"
                }
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No Devices Found") },
                    onClick = { expanded = false }
                )
            } else {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onItemSelected(deviceMacs[index])
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CheckboxWithAccordion(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String?,
    isScrollable: Boolean = false
) {
    Column {
        Row {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(text = label, modifier = Modifier.padding(top = 12.dp))
        }
        if (checked && value != null) {
            if (isScrollable) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .padding(start = 32.dp, top = 4.dp)
                        .heightIn(max = 200.dp)
                ) {
                    Text(
                        text = value,
                        modifier = Modifier
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodySmall
                    )
                    VerticalScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            } else {
                Text(
                    text = value,
                    modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val maxScroll = scrollState.maxValue
    if (maxScroll > 0) {
        val scrollFraction = (scrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
        Box(
            modifier = modifier
                .width(4.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            val scrollbarHeight = 20.dp
            val containerHeight = 200.dp
            if (containerHeight > scrollbarHeight) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scrollbarHeight)
                        .offset(y = ((containerHeight - scrollbarHeight) * scrollFraction))
                        .background(Color.Gray)
                )
            }
        }
    }
}