package org.openkis.android.ui.caves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.openkis.android.data.local.entity.ArtificialEntity
import org.openkis.android.data.local.entity.CaveEntity
import org.openkis.android.data.local.entity.SpringEntity
import org.openkis.android.data.repository.CaveRepository
import org.openkis.android.data.repository.SyncManager
import javax.inject.Inject

enum class ItemType(val label: String) {
    CAVES("Caves"),
    SPRINGS("Springs"),
    ARTIFICIALS("Artificials")
}

data class CaveListItem(
    val code: String,
    val name: String,
    val type: ItemType,
    val elevation: String = "",
    val depth: String = "",
    val length: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

private data class CavesFilterState(
    val query: String,
    val type: ItemType,
    val enabled: Set<ItemType>,
    val visibleUrls: Set<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CavesViewModel @Inject constructor(
    private val repository: CaveRepository,
    syncManager: SyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow(ItemType.CAVES)
    val selectedType: StateFlow<ItemType> = _selectedType.asStateFlow()

    val enabledTypes: StateFlow<Set<ItemType>> = combine(
        syncManager.showCaves,
        syncManager.showSprings,
        syncManager.showArtificials
    ) { caves, springs, artificials ->
        buildSet {
            if (caves) add(ItemType.CAVES)
            if (springs) add(ItemType.SPRINGS)
            if (artificials) add(ItemType.ARTIFICIALS)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ItemType.entries.toSet())

    val items: StateFlow<List<CaveListItem>> = combine(
        _searchQuery, _selectedType, enabledTypes, syncManager.visibleServerUrls
    ) { query, type, enabled, visibleUrls ->
        CavesFilterState(query, type, enabled, visibleUrls)
    }.flatMapLatest { (query, type, enabled, visibleUrls) ->
        if (type !in enabled) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            when (type) {
                ItemType.CAVES -> {
                    val flow = if (query.isBlank()) repository.getAllCaves() else repository.searchCaves(query)
                    flow.flatMapLatest { caves ->
                        kotlinx.coroutines.flow.flowOf(
                            caves.filter { it.serverUrl in visibleUrls }.map { it.toListItem() }
                        )
                    }
                }
                ItemType.SPRINGS -> {
                    val flow = if (query.isBlank()) repository.getAllSprings() else repository.searchSprings(query)
                    flow.flatMapLatest { springs ->
                        kotlinx.coroutines.flow.flowOf(
                            springs.filter { it.serverUrl in visibleUrls }.map { it.toListItem() }
                        )
                    }
                }
                ItemType.ARTIFICIALS -> {
                    val flow = if (query.isBlank()) repository.getAllArtificials() else repository.searchArtificials(query)
                    flow.flatMapLatest { arts ->
                        kotlinx.coroutines.flow.flowOf(
                            arts.filter { it.serverUrl in visibleUrls }.map { it.toListItem() }
                        )
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedType(type: ItemType) {
        _selectedType.value = type
    }
}

private fun CaveEntity.toListItem() = CaveListItem(
    code = code, name = name, type = ItemType.CAVES,
    elevation = elevation, depth = depthTotal, length = lengthTotal,
    latitude = latitude, longitude = longitude
)

private fun SpringEntity.toListItem() = CaveListItem(
    code = code, name = name, type = ItemType.SPRINGS,
    elevation = elevation, latitude = latitude, longitude = longitude
)

private fun ArtificialEntity.toListItem() = CaveListItem(
    code = code, name = name, type = ItemType.ARTIFICIALS,
    elevation = elevation, depth = depthTotal, length = lengthTotal,
    latitude = latitude, longitude = longitude
)
