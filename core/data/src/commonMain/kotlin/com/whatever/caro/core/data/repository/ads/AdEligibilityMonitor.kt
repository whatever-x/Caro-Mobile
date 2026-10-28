package com.whatever.caro.core.data.repository.ads

import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdEligibilityState(
    val generation: Long = 0,
    val allowed: Boolean = false,
    val transitions: Int = 0,
    val recheckRevision: Long = 0,
)

class AdEligibilityGate {
    private val mutableState = MutableStateFlow(AdEligibilityState())
    val state: StateFlow<AdEligibilityState> = mutableState.asStateFlow()

    fun revoke() {
        mutableState.update { it.copy(generation = it.generation + 1, allowed = false) }
    }

    fun beginTransition() {
        mutableState.update { it.copy(generation = it.generation + 1, allowed = false, transitions = it.transitions + 1) }
    }

    fun endTransition() {
        mutableState.update {
            it.copy(
                generation = it.generation + 1,
                allowed = false,
                transitions = it.transitions - 1,
                recheckRevision =
                    it.recheckRevision + 1,
            )
        }
    }

    internal fun allowIfCurrent(generation: Long) {
        mutableState.update { current ->
            if (current.generation == generation && current.transitions == 0) current.copy(allowed = true) else current
        }
    }
}

class AdEligibilityMonitor(
    private val auth: LocalAuthDataSource,
    private val profile: ProfileRepository,
    private val gate: AdEligibilityGate,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    val state: StateFlow<AdEligibilityState> = gate.state
    private val checks = MutableStateFlow(0L)

    init {
        scope.launch {
            val budget = MutableStateFlow(AdEligibilityBudget())
            combine(
                auth.observeRegisteredSession(),
                gate.state.map { it.transitions to it.recheckRevision }.distinctUntilChanged(),
                checks,
            ) { session, transition, check -> Triple(session, transition.first, check) }
                .collect { (session, transitions, check) ->
                    if (state.value.transitions != transitions) return@collect
                    if (session != null) {
                        val current =
                            try {
                                auth.fetchTokens()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                gate.revoke()
                                return@collect
                            }
                        if (current != session) return@collect
                    }
                    budget.update { current ->
                        if (current.sessionGeneration != session?.generation || current.check != check) {
                            AdEligibilityBudget(sessionGeneration = session?.generation, check = check)
                        } else {
                            current
                        }
                    }
                    gate.revoke()
                    if (session != null && transitions == 0 && budget.value.attempts < 2) {
                        budget.update { it.copy(attempts = it.attempts + 1) }
                        val generation = state.value.generation
                        launch {
                            try {
                                val adult = profile.getMyInfo().isAdult == true
                                if (auth.fetchTokens() == session && state.value.generation == generation && state.value.transitions == 0) {
                                    budget.update { current ->
                                        if (current.sessionGeneration == session.generation && current.check == check) {
                                            current.copy(attempts = 0)
                                        } else {
                                            current
                                        }
                                    }
                                    if (adult) gate.allowIfCurrent(generation)
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                // A failed age lookup must never start advertising.
                            }
                        }
                    }
                }
        }
    }

    fun recheck() {
        if (!state.value.allowed && state.value.transitions == 0) checks.update { it + 1 }
    }

    fun revoke() = gate.revoke()
}

private data class AdEligibilityBudget(
    val sessionGeneration: Long? = null,
    val check: Long = -1,
    val attempts: Int = 0,
)
