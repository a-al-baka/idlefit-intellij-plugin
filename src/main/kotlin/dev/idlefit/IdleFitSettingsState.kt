package dev.idlefit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "dev.idlefit.IdleFitSettingsState",
    storages = [Storage("IdleFit.xml")]
)
class IdleFitSettingsState : PersistentStateComponent<IdleFitSettingsState.State> {

    data class State(
        var pluginEnabled: Boolean = true,
        var minTimeBetweenExercises: Int = 30,
        var compilationTriggerEnabled: Boolean = true,
        var processTerminationTriggerEnabled: Boolean = true,
        var indexingTriggerEnabled: Boolean = true,
        var exercises: MutableMap<String, Boolean> = mutableMapOf(
            "Squat" to true,
            "Push-up" to true,
            "Burpee" to true
        )
    )

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: IdleFitSettingsState
            get() = ApplicationManager.getApplication().getService(IdleFitSettingsState::class.java)
    }
}