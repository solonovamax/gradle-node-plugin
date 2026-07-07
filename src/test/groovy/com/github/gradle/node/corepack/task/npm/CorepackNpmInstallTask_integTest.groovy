package com.github.gradle.node.corepack.task.npm

import com.github.gradle.AbstractIntegTest
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables

class CorepackNpmInstallTask_integTest extends AbstractIntegTest {
    @Rule
    EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def 'do not fail build if package.json is missing (#gv.version)'() {
        given:
        gradleVersion = gv

        copyResources("fixtures/node-env/")

        when:
        def result1 = build(":corepackNpmInstall")

        then:
        result1.task(":corepackNpmInstall").outcome == TaskOutcome.NO_SOURCE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
