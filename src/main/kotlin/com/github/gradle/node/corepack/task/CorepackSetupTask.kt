package com.github.gradle.node.corepack.task

import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmSetupTask
import com.github.gradle.node.variant.VariantComputer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

/**
 * corepack install that only gets executed if gradle decides so.
 */
abstract class CorepackSetupTask : NpmSetupTask() {

    init {
        group = NodePlugin.COREPACK_GROUP
        description = "Setup a specific version of corepack to be used by the build."
    }

    @Input
    override fun getVersion(): Provider<String> {
        return nodeExtension.corepackVersion
    }

    @get:OutputDirectory
    val corepackDir by lazy {
        val variantComputer = VariantComputer()
        variantComputer.computeCorepackDir(nodeExtension)
    }

    override fun computeCommand(): List<String> {
        val version = nodeExtension.corepackVersion.get()
        val corepackDir = corepackDir.get()
        val corepackPackage = if (version.isNotBlank()) "corepack@$version" else "corepack"
        return listOf(
            "install",
            "--global",
            "--no-save",
            "--prefix",
            corepackDir.asFile.absolutePath,
            corepackPackage
        ) + args.get()
    }

    override fun isTaskEnabled(): Boolean {
        return true
    }

    companion object {
        const val NAME = "corepackSetup"
    }

}
