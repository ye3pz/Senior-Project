package com.example.shade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shade.ThreatsList
import com.example.shade.data.ThreatItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.shade.ui.theme.HistoryItem
import com.example.shade.ui.theme.ThreatLevel

class HistoryViewModel : ViewModel() {

    // Internal mutable list of threat items
    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())

    // Public read-only flow for the UI
    val items: StateFlow<List<HistoryItem>> = _items

    init {
        // Load threats when the VM is created
        loadHistory(ThreatsList.activeThreats.toList())
    }

    fun loadHistory(threats: List<ThreatItem>) {
            // Convert to new list so UI recomposes properly
        _items.value = threats.map { it.toHistoryItem() }

    }


     // TODO : Future clear of history
    fun clearHistory() {
        viewModelScope.launch {
            ThreatsList.activeThreats.clear()
            _items.value = emptyList()
        }
    }

    private fun ThreatItem.toHistoryItem(): HistoryItem {
        val threatLevel = when {
            description.contains("Dangerous", ignoreCase = true) -> ThreatLevel.HIGH
            description.contains("Moderate", ignoreCase = true)  -> ThreatLevel.MEDIUM
            else -> ThreatLevel.SAFE
        }

        val dateTime = java.text.SimpleDateFormat(
            "MM-dd-yyyy hh:mm a",
            java.util.Locale.getDefault()
        ).format(System.currentTimeMillis())

        return HistoryItem(
            name = title,
            dateTime = dateTime,
            threatLevel = threatLevel
        )
    }
    fun refreshHistory() {
        _items.value = ThreatsList.activeThreats.toList().map { it.toHistoryItem() }
    }

}
