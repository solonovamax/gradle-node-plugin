package com.github.gradle.node.corepack.task.npm

import com.github.gradle.AbstractIntegTest
import org.gradle.testkit.runner.TaskOutcome

class CorepackNpm_integTest extends AbstractIntegTest {
    def 'install packages with npm and Node.js project in sub directory (#gv.version)'() {
        given:
        gradleVersion = gv

        copyResources("fixtures/npm-corepack-in-subdirectory/")
        copyResources("fixtures/javascript-project/", "javascript-project")

        when:
        def result1 = build("build")

        then:
        result1.task(":corepackNpmInstall").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackBuildNpm").outcome == TaskOutcome.SUCCESS
        createFile("javascript-project/package-lock.json").isFile()
        createFile("javascript-project/node_modules").isDirectory()
        !createFile("package-lock.json").exists()
        !createFile("node_modules").exists()
        createFile("javascript-project/output-npm/index.js").isFile()

        when:
        def result2 = build("build")

        then:
        // Not up-to-date because the package-lock.json now exists
        result2.task(":corepackNpmInstall").outcome == TaskOutcome.SUCCESS
        result2.task(":corepackBuildNpm").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result3 = build("build")

        then:
        result3.task(":corepackNpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackBuildNpm").outcome == TaskOutcome.UP_TO_DATE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
