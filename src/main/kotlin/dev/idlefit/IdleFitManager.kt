package dev.idlefit

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerTopics
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import java.util.concurrent.TimeUnit

class IdleFitManager : ProjectActivity {

    private val settings = IdleFitSettingsState.instance.state
    private var lastExerciseTime = System.currentTimeMillis()
    private var notification: Notification? = null

    override suspend fun execute(project: Project) {
        val messageBusConnection = project.messageBus.connect()

        messageBusConnection.subscribe(CompilerTopics.COMPILATION_STATUS, object : CompilationStatusListener {
            override fun compilationFinished(
                isAborted: Boolean,
                errors: Int,
                warnings: Int,
                compileContext: CompileContext
            ) {
                if (settings.compilationTriggerEnabled) {
                    LOG.info("Compilation finished trigger")
                    showExercisePopup(project)
                }
            }
        })

        messageBusConnection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processTerminated(
                executorId: String,
                env: ExecutionEnvironment,
                handler: ProcessHandler,
                exitCode: Int
            ) {
                if (settings.processTerminationTriggerEnabled) {
                    LOG.info("Process terminated trigger")
                    showExercisePopup(project)
                }
            }
        })

        messageBusConnection.subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
                if (settings.indexingTriggerEnabled) {
                    LOG.info("Indexing started trigger")
                    showExercisePopup(project)
                }
            }

            override fun exitDumbMode() {}
        })
    }

    private fun showExercisePopup(project: Project) {
        if (!settings.pluginEnabled) {
            LOG.debug("IdleFit plugin is disabled.")
            return
        }

        if (notification?.isExpired == false) {
            LOG.debug("Notification already showing, aborting.")
            return
        }

        val currentTime = System.currentTimeMillis()
        val minTimeBetweenExercises = TimeUnit.MINUTES.toMillis(settings.minTimeBetweenExercises.toLong())
        val timeSinceLastExercise = currentTime - lastExerciseTime

        if (timeSinceLastExercise < minTimeBetweenExercises) {
            LOG.debug("Not enough time has passed since last exercise.")
            return
        }

        val enabledExercises = settings.exercises.filter { it.value }.keys
        if (enabledExercises.isEmpty()) {
            LOG.debug("No exercises enabled.")
            return
        }

        LOG.info("Conditions met, showing notification.")
        ApplicationManager.getApplication().invokeLater {
            val exercise = enabledExercises.random()
            notification = NotificationGroupManager.getInstance().getNotificationGroup("IdleFit")
                .createNotification(
                    "Time for a break!",
                    "Your exercise: $exercise", NotificationType.INFORMATION)

            notification?.addAction(object : AnAction("Done") {
                override fun actionPerformed(e: AnActionEvent) {
                    lastExerciseTime = System.currentTimeMillis()
                    notification?.expire()
                    LOG.info("'Done' clicked. Timer reset.")
                }
            })

            notification?.addAction(object : AnAction("Snooze") {
                override fun actionPerformed(e: AnActionEvent) {
                    lastExerciseTime = System.currentTimeMillis() - minTimeBetweenExercises + TimeUnit.MINUTES.toMillis(5)
                    notification?.expire()
                    LOG.info("'Snooze' clicked. Next notification in 5 minutes.")
                }
            })

            notification?.notify(project)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(IdleFitManager::class.java)
    }
}