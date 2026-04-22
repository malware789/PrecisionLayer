package com.example.precisionlayertesting.features.bug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.repository.BugRepository
import com.example.precisionlayertesting.core.utils.Result
import kotlinx.coroutines.launch

class ReportedBugDetailsViewModel(
    private val repository: BugRepository
) : ViewModel() {

    private val _viewUrl = MutableLiveData<String?>()
    val viewUrl: LiveData<String?> get() = _viewUrl

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchScreenshotUrl(bugId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            when (val result = repository.getScreenshotViewUrl(bugId)) {
                is Result.Success -> {
                    _viewUrl.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Failed to fetch screenshot"
                }

                else -> {
                    _error.value =  "Image not fetched"
                }
            }
            _loading.value = false
        }
    }
}
