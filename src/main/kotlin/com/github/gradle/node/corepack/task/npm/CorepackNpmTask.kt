package com.github.gradle.node.corepack.task.npm

import com.github.gradle.node.corepack.task.CorepackBaseTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty

abstract class CorepackNpmTask : CorepackBaseTask() {
    @get:Optional
    @get:Input
    val npmCommand = objects.listProperty<String>()

    @Deprecated("Avoid using this, prefer npmCommand", replaceWith = ReplaceWith("npmCommand"))
    override val corepackCommand: ListProperty<String>
        get() = npmCommand

    init {
        command.set("npm")
        command.finalizeValue()
        super.corepackCommand.set(npmCommand)
        // cannot finalize corepackCommand, because then yarnCommand will no longer be tracked
        // corepackCommand.finalizeValue()
    }
}
