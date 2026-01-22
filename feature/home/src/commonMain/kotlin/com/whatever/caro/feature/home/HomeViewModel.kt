package com.whatever.caro.feature.home

import androidx.lifecycle.viewModelScope
import com.whatever.caro.core.data.repository.demo.DemoRepository
import com.whatever.caro.core.model.User
import com.whatever.caro.core.navigator.entries.HomeEntry
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeSideEffect
import com.whatever.caro.feature.home.mvi.HomeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class HomeViewModel(
    @InjectedParam navKey: HomeEntry,
    private val demoRepository: DemoRepository,
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
    initialState = HomeState(
        test = navKey.payload.name,
    )
) {

    // 1. init 초기화
    init {
        launch {
            val userData = demoRepository.getData(id = navKey.payload.id.toLong())

            reduce {
                copy(
                    name = userData.name
                )
            }
        }
    }

    // 2. flow로 데이터 초기화
    val user: StateFlow<User> = flow {
        demoRepository.getDataFlow(id = navKey.payload.id.toLong())
            .collect { emit(it) }
    }.stateIn(
        scope = viewModelScope + coroutineExceptionHandler,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = User(id = 0L, name = "")
    )

    override suspend fun handleIntent(intent: HomeIntent) {
        TODO()
    }

}