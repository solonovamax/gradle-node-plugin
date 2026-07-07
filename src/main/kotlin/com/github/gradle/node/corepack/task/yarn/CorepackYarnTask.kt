package com.github.gradle.node.corepack.task.yarn

import com.github.gradle.node.corepack.task.CorepackBaseTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty

abstract class CorepackYarnTask : CorepackBaseTask() {
    @get:Optional
    @get:Input
    val yarnCommand = objects.listProperty<String>()

    @Deprecated("Avoid using this, prefer yarnCommand", replaceWith = ReplaceWith("yarnCommand"))
    override val corepackCommand: ListProperty<String>
        get() = yarnCommand

    init {
        command.set("yarn")
        command.finalizeValue()
        super.corepackCommand.set(yarnCommand)
        // cannot finalize corepackCommand, because then yarnCommand will no longer be tracked
        // corepackCommand.finalizeValue()
    }
}
