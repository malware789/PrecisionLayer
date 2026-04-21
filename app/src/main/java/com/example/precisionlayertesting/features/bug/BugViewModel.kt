package com.example.precisionlayertesting.features.bug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.base.BaseViewModel
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.core.models.bugModel.BugReport
import com.example.precisionlayertesting.core.repository.BugRepository
import kotlinx.coroutines.launch

class BugViewModel(
    private val bugRepository: BugRepository,
    private val sessionId: String,
    private val workspaceId: String
) : BaseViewModel() {

    private val _bugs = MutableLiveData<Result<List<BugReport>>>()
    val bugs: LiveData<Result<List<BugReport>>> = _bugs

    private var currentPage = 0
    private var allBugs = mutableListOf<BugReport>()

    init {
        loadBugs(refresh = true)
    }

    fun loadBugs(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            allBugs.clear()
        }
        
        viewModelScope.launch {
            if (refresh) _bugs.value = Result.Loading
            try {
                val data = bugRepository.getBugReports(sessionId, workspaceId, currentPage)
                if (refresh) {
                    allBugs = data.toMutableList()
                } else {
                    allBugs.addAll(data)
                }
                _bugs.value = Result.Success(allBugs)
                if (data.isNotEmpty()) currentPage++
            } catch (e: Exception) {
                _bugs.value = Result.Error(e)
            }
        }
    }
}
