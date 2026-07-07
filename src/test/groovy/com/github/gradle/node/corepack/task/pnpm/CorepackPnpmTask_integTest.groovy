package com.github.gradle.node.corepack.task.pnpm

import com.github.gradle.AbstractIntegTest
import com.github.gradle.node.Versions
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Ignore

class CorepackPnpmTask_integTest extends AbstractIntegTest {
    @Rule
    EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def 'execute pnpm command with a package.json file and check inputs up-to-date detection (#gv.version)'() {
        given:
        gradleVersion = gv
        copyResources('fixtures/pnpm-corepack/', '')
        copyResources('fixtures/javascript-project/', '')
        writePackageJson("""{
            "name": "hello",
            "dependencies": {
                "mocha": "8.2.0",
                "chai": "4.2.0"
            },
            "devDependencies": {
                "@babel/cli": "^7.0.0",
                "@babel/core": "^7.0.0"
            },
            "scripts": {
                "build": "babel src",
                "test": "mocha"
            },
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result1 = build(":test")

        then:
        result1.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        result1.task(":test").outcome == TaskOutcome.SUCCESS
        result1.output.contains("1 passing")

        when:
        def result2 = build(":test")

        then:
        result2.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        result2.task(":test").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result3 = build(":test", "-DchangeInputs=true")

        then:
        result3.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":test").outcome == TaskOutcome.SUCCESS

        when:
        def result4 = build(":version")

        then:
        result4.task(":version").outcome == TaskOutcome.SUCCESS
        result4.output.contains("> Task :version${System.lineSeparator()}${Versions.TEST_PNPM_DOWNLOAD_VERSION}")

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    @Ignore("https://github.com/node-gradle/gradle-node-plugin/issues/270")
    def 'execute pnpm command with custom execution configuration and check up-to-date-detection (#gv.version)'() {
        given:
        gradleVersion = gv
        copyResources('fixtures/pnpm-env/', '')
        copyResources('fixtures/env/', '')
        writePackageJson("""{
            "name": "env",
            "dependencies": {
                "@bahmutov/print-env": "2.0.2",
                "utils-eval": "1.0.1"
            },
            "scripts": {
                "print-env": "print-env PATH CUSTOM",
                "printcwd": "jseval \\"'Working directory is \\\\'' + process.env.INIT_CWD + '\\\\''\\""
            },
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result1 = build(":env")

        then:
        result1.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        result1.task(":env").outcome == TaskOutcome.SUCCESS
        environmentDumpContainsPathVariable(result1.output)

        when:
        def result2 = build(":env", "-DcustomEnv=true")

        then:
        result2.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result2.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        result2.task(":env").outcome == TaskOutcome.SUCCESS
        result2.output.contains("CUSTOM=custom value")

        when:
        environmentVariables.set("NEW_ENV_VARIABLE", "Let's make the whole environment change")
        def result3 = build(":env", "-DcustomEnv=true")

        then:
        result3.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":env").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result4 = build(":env", "-DignoreExitValue=true", "-DnotExistingCommand=true")

        then:
        result4.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result4.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result4.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result4.task(":env").outcome == TaskOutcome.SUCCESS
        result4.output.contains("Unknown command 'notExistingCommand'")

        when:
        def result5 = buildAndFail(":env", "-DnotExistingCommand=true")

        then:
        result5.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result5.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result5.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result5.task(":env").outcome == TaskOutcome.FAILED
        result5.output.contains("Unknown command 'notExistingCommand'")

        when:
        def result6 = build(":pwd")

        then:
        result6.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result6.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result6.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result6.task(":pwd").outcome == TaskOutcome.SUCCESS
        result6.output.contains("Working directory is '${projectDir}'")

        when:
        def result7 = build(":pwd", "-DcustomWorkingDir=true")

        then:
        result7.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result7.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result7.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        result7.task(":pwd").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result8 = build(":pwd", "-DcustomWorkingDir=true", "--rerun-tasks")

        then:
        result8.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result8.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result8.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        result8.task(":pwd").outcome == TaskOutcome.SUCCESS
        def expectedWorkingDirectory = "${projectDir}${File.separator}build${File.separator}customWorkingDirectory"
        result8.output.contains("Working directory is '${expectedWorkingDirectory}'")
        new File(expectedWorkingDirectory).isDirectory()

        when:
        def result9 = build(":version")

        then:
        result9.task(":version").outcome == TaskOutcome.SUCCESS
        result9.output.contains("> Task :version${System.lineSeparator()}${Versions.TEST_PNPM_DOWNLOAD_VERSION}")

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
