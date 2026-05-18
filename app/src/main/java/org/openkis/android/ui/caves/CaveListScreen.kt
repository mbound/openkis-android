package org.openkis.android.ui.caves

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.openkis.android.R
import org.openkis.android.ui.theme.ArtificialMarker
import org.openkis.android.ui.theme.CaveMarker
import org.openkis.android.ui.theme.SpringMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaveListScreen(
    onItemClick: (type: String, code: String) -> Unit,
    viewModel: CavesViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val enabledTypes by viewModel.enabledTypes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_browse)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search)) },
                singleLine = true
            )

            // Type filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemType.entries.filter { it in enabledTypes }.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { viewModel.setSelectedType(type) },
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results count
            Text(
                text = stringResource(R.string.items_count, items.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // List
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.browse_sync_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items, key = { "${it.type.name}_${it.code}" }) { item ->
                        CaveListItemCard(
                            item = item,
                            onClick = {
                                val typeStr = when (item.type) {
                                    ItemType.CAVES -> "caves"
                                    ItemType.SPRINGS -> "springs"
                                    ItemType.ARTIFICIALS -> "artificials"
                                }
                                onItemClick(typeStr, item.code)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaveListItemCard(item: CaveListItem, onClick: () -> Unit) {
    val accentColor = when (item.type) {
        ItemType.CAVES -> CaveMarker
        ItemType.SPRINGS -> SpringMarker
        ItemType.ARTIFICIALS -> ArtificialMarker
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Code badge
            Card(
                colors = CardDefaults.cardColors(containerColor = accentColor)
            ) {
                Text(
                    text = item.code,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name.ifBlank { item.code },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.elevation.isNotBlank()) {
                        Text("Q.${item.elevation}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.length.isNotBlank()) {
                        Text("SV.${item.length}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.depth.isNotBlank()) {
                        Text("P.${item.depth}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
