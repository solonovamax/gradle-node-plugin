package com.github.gradle.node.corepack.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.corepack.exec.CorepackExecRunner
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.task.BaseTask
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import javax.inject.Inject

abstract class CorepackBaseTask : BaseTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    val command = objects.property<String>()

    @get:Optional
    @get:Input
    open val corepackCommand = objects.listProperty<String>()

    @get:Optional
    @get:Input
    val args = objects.listProperty<String>()

    @get:Input
    val ignoreExitValue = objects.property<Boolean>().convention(false)

    @get:Internal
    val workingDir = objects.directoryProperty()

    @get:Input
    val environment = objects.mapProperty<String, String>()

    @get:Internal
    val execOverrides = objects.property<Action<ExecSpec>>()

    @get:Internal
    val nodeExtension = NodeExtension[project]

    init {
        group = NodePlugin.NODE_GROUP
        dependsOn(CorepackSetupTask.NAME)
    }

    // For DSL
    @Suppress("unused")
    fun execOverrides(execOverrides: Action<ExecSpec>) {
        this.execOverrides.set(execOverrides)
    }

    @TaskAction
    fun exec() {
        val command = buildList {
            add(command.get())
            addAll(corepackCommand.get())
            addAll(args.get())
        }
        val nodeExecConfiguration = NodeExecConfiguration(
            command, environment.get(), workingDir.asFile.orNull,
            ignoreExitValue.get(), execOverrides.orNull
        )
        val corepackExecRunner = objects.newInstance(CorepackExecRunner::class.java)
        result = corepackExecRunner.executeCorepackCommand(execOperations, nodeExtension, nodeExecConfiguration, variantComputer)
    }
}
