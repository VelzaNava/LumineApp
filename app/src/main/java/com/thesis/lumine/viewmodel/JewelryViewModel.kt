package com.thesis.lumine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JewelryViewModel : ViewModel() {

    private val repository = JewelryRepository()

    private val _jewelryList = MutableStateFlow<List<Jewelry>>(emptyList())
    val jewelryList: StateFlow<List<Jewelry>> = _jewelryList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadJewelry()
    }

    fun loadJewelry() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = repository.getAllJewelry()

                if (response.isSuccessful && response.body() != null) {
                    _jewelryList.value = response.body()!!
                } else {
                    _error.value = "Failed to load jewelry"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByType(type: String) {
        viewModelScope.launch {
            try {
                val response = repository.getAllJewelry()
                if (response.isSuccessful && response.body() != null) {
                    _jewelryList.value = response.body()!!.filter {
                        it.type.equals(type, ignoreCase = true)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
