package io.github.ahmadhussain0335.kmppermission.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import io.github.ahmadhussain0335.kmppermission.compose.rememberMultiplePermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDemoScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("KMPermission Sample") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(demoItems, key = { it.title }) { item ->
                PermissionDemoRow(item)
            }
            item {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open App Settings")
                }
            }
        }
    }
}

@Composable
private fun PermissionDemoRow(item: PermissionDemoItem, modifier: Modifier = Modifier) {
    val state = rememberMultiplePermissions(*item.permissions.toTypedArray())
    val status = worstStatus(state.permissions.map { it.status })

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                StatusBadge(status)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { state.launchMultiplePermissionRequest() },
                enabled = status != PermissionStatus.Granted,
            ) {
                Text("Request")
            }
        }
    }
}
