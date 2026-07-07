package com.github.gradle.node.corepack.task.yarn

import com.github.gradle.node.NodePlugin
import com.github.gradle.node.util.zip
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.property
import java.io.File

/**
 * yarn install that only gets executed if gradle decides so.
 */
abstract class CorepackYarnInstallTask : CorepackYarnTask() {
    @get:Internal
    val nodeModulesOutputFilter = objects.property<Action<ConfigurableFileTree>>()

    init {
        group = NodePlugin.YARN_GROUP
        description = "Install node packages using Yarn (via Corepack)."
        yarnCommand.set(nodeExtension.npmInstallCommand.map { listOf(it) })
    }

    @PathSensitive(RELATIVE)
    @Optional
    @InputFile
    protected fun getPackageJsonFile(): File? {
        return projectFileIfExists("package.json").orNull
    }

    @PathSensitive(RELATIVE)
    @Optional
    @InputFile
    protected fun getYarnLockFile(): File? {
        return projectFileIfExists("yarn.lock").orNull
    }

    @Optional
    @OutputFile
    protected fun getYarnLockFileAsOutput(): File? {
        return projectFileIfExists("yarn.lock").orNull
    }

    private fun projectFileIfExists(name: String): Provider<File> {
        return nodeExtension.nodeProjectDir.map { it.file(name).asFile }
            .map { if (it.exists()) it else null }
    }

    @Optional
    @OutputDirectory
    @Suppress("unused")
    protected fun getNodeModulesDirectory(): Provider<Directory> {
        val filter = nodeModulesOutputFilter.orNull
        return if (filter == null) nodeExtension.nodeProjectDir.dir("node_modules")
        else providers.provider { null }
    }

    @Optional
    @OutputFiles
    @Suppress("unused")
    protected fun getNodeModulesFiles(): Provider<FileTree> {
        val nodeModulesDirectoryProvider = nodeExtension.nodeProjectDir.dir("node_modules")
        return zip(nodeModulesDirectoryProvider, nodeModulesOutputFilter)
            .map { (nodeModulesDirectory, nodeModulesOutputFilter) ->
                val fileTree = objects.fileTree().from(nodeModulesDirectory)
                nodeModulesOutputFilter.execute(fileTree)
                fileTree
            }
    }

    // For DSL
    @Suppress("unused")
    fun nodeModulesOutputFilter(nodeModulesOutputFilter: Action<ConfigurableFileTree>) {
        this.nodeModulesOutputFilter.set(nodeModulesOutputFilter)
    }

    companion object {
        const val NAME = "corepackYarnInstall"
    }
}
