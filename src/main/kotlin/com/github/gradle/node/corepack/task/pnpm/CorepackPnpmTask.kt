package com.github.gradle.node.corepack.task.pnpm

import com.github.gradle.node.corepack.task.CorepackBaseTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty

abstract class CorepackPnpmTask : CorepackBaseTask() {
    @get:Optional
    @get:Input
    val pnpmCommand = objects.listProperty<String>()

    @Deprecated("Avoid using this, prefer pnpmCommand", replaceWith = ReplaceWith("pnpmCommand"))
    override val corepackCommand: ListProperty<String>
        get() = pnpmCommand

    init {
        command.set("pnpm")
        command.finalizeValue()
        super.corepackCommand.set(pnpmCommand)
        // cannot finalize corepackCommand, because then yarnCommand will no longer be tracked
        // corepackCommand.finalizeValue()
    }
}
