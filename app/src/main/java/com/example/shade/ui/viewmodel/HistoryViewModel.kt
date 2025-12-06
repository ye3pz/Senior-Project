package com.example.shade.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shade.ThreatsList
import com.example.shade.utils.ThreatItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.shade.utils.HistoryItem
import com.example.shade.utils.toHistoryItem

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


    fun clearHistory() {
        viewModelScope.launch {
            ThreatsList.activeThreats.clear()
            _items.value = emptyList()
        }
    }


    fun refreshHistory() {
        _items.value = ThreatsList.activeThreats.toList().map { it.toHistoryItem() }
    }

}
