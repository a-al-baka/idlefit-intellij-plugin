package dev.idlefit

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class IdleFitSettingsConfigurable : Configurable {

    private val settings = IdleFitSettingsState.instance.state

    private val enableCheckBox = JBCheckBox("", settings.pluginEnabled)
    private val minTimeSpinner = JSpinner(SpinnerNumberModel(settings.minTimeBetweenExercises, 15, 90, 1))
    private val compilationCheckBox = JBCheckBox("Compilation", settings.compilationTriggerEnabled)
    private val processTerminationCheckBox = JBCheckBox("Process Termination", settings.processTerminationTriggerEnabled)
    private val indexingCheckBox = JBCheckBox("Indexing", settings.indexingTriggerEnabled)
    private val exerciseCheckBoxes = settings.exercises.map { (exercise, enabled) ->
        JBCheckBox(exercise, enabled)
    }

    override fun createComponent(): JComponent {
        return panel {
            row("Enable IdleFit:") {
                cell(enableCheckBox)
            }
            row("Minimum time between exercises:") {
                cell(minTimeSpinner)
                label("minutes")
            }
            row("Show popup on:") {
                cell(compilationCheckBox)
                cell(processTerminationCheckBox)
                cell(indexingCheckBox)
            }
            group("Exercises:") {
                exerciseCheckBoxes.forEach {
                    row {
                        cell(it)
                    }
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return enableCheckBox.isSelected != settings.pluginEnabled ||
                minTimeSpinner.value != settings.minTimeBetweenExercises ||
                compilationCheckBox.isSelected != settings.compilationTriggerEnabled ||
                processTerminationCheckBox.isSelected != settings.processTerminationTriggerEnabled ||
                indexingCheckBox.isSelected != settings.indexingTriggerEnabled ||
                exerciseCheckBoxes.any { it.text in settings.exercises && it.isSelected != settings.exercises[it.text] }
    }

    override fun apply() {
        settings.pluginEnabled = enableCheckBox.isSelected
        settings.minTimeBetweenExercises = minTimeSpinner.value as Int
        settings.compilationTriggerEnabled = compilationCheckBox.isSelected
        settings.processTerminationTriggerEnabled = processTerminationCheckBox.isSelected
        settings.indexingTriggerEnabled = indexingCheckBox.isSelected
        exerciseCheckBoxes.forEach { settings.exercises[it.text] = it.isSelected }
    }

    override fun reset() {
        enableCheckBox.isSelected = settings.pluginEnabled
        minTimeSpinner.value = settings.minTimeBetweenExercises
        compilationCheckBox.isSelected = settings.compilationTriggerEnabled
        processTerminationCheckBox.isSelected = settings.processTerminationTriggerEnabled
        indexingCheckBox.isSelected = settings.indexingTriggerEnabled
        exerciseCheckBoxes.forEach { it.isSelected = settings.exercises[it.text] ?: false }
    }

    override fun getDisplayName(): String {
        return "IdleFit"
    }
}