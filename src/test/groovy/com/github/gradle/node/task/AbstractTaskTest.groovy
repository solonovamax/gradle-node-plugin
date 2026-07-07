package com.github.gradle.node.task

import com.github.gradle.AbstractProjectTest
import com.github.gradle.node.NodeExtension
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

abstract class AbstractTaskTest extends AbstractProjectTest {
    ExecResult execResult
    ExecSpec execSpec
    NodeExtension nodeExtension

    def setup() {
        execSpec = Mock(ExecSpec)
        execResult = Mock(ExecResult)

        project.apply plugin: 'com.github.node-gradle.node'
        nodeExtension = NodeExtension.get(project)
    }

    protected <T extends Task> T mockExecOperationsExec(T task) {
        T taskSpy = Spy(task) as T
        ExecOperations execOperations = Spy(project.services.get(ExecOperations)) as ExecOperations
        execOperations.exec(_ as Action<ExecSpec>) >> { Action<ExecSpec> action ->
            action.execute(execSpec)
            return execResult
        }

        taskSpy.getExecOperations() >> execOperations
        return taskSpy
    }

    protected static containsPath(final Map<String, ?> env) {
        return env['PATH'] != null || env['Path'] != null
    }

    // Workaround a strange issue on Github actions macOS and Windows hosts
    protected static List<String> fixAbsolutePaths(Iterable<String> path) {
        return path.collect { fixAbsolutePath(it) }
    }

    protected static fixAbsolutePath(String path) {
        return path.replace('/private/', '/')
                .replace('C:\\Users\\runneradmin\\', 'C:\\Users\\RUNNER~1\\')
    }
}
