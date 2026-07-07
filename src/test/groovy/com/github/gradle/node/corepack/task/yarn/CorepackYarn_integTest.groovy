package com.github.gradle.node.corepack.task.yarn

import com.github.gradle.AbstractIntegTest
import org.gradle.testkit.runner.TaskOutcome

class CorepackYarn_integTest extends AbstractIntegTest {
    def 'install packages with yarn and Node.js project in sub directory (#gv.version)'() {
        given:
        gradleVersion = gv

        writeBuild('''
        plugins {
            id 'com.github.node-gradle.node\'
        }
        
        node {
            nodeProjectDir = file("${projectDir}/javascript-project")
        }
        
        task build(type: CorepackYarnTask) {
            dependsOn corepackYarnInstall
            yarnCommand = ["run", "build"]
            args = ["--out-dir", "output"]
            inputs.dir("javascript-project/src")
            outputs.dir("javascript-project/output")
        }
        ''')
        copyResources("fixtures/javascript-project/", "javascript-project")

        when:
        def result1 = build("build")

        then:
        result1.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result1.task(":corepackYarnInstall").outcome == TaskOutcome.SUCCESS
        result1.task(":build").outcome == TaskOutcome.SUCCESS
        createFile("javascript-project/yarn.lock").isFile()
        createFile("javascript-project/node_modules").isDirectory()
        !createFile("yarn.lock").exists()
        !createFile("node_modules").exists()
        createFile("javascript-project/output/index.js").isFile()

        when:
        def result2 = build("build", "--info")

        then:
        result2.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        // Not up-to-date because the package-lock.json now exists
        result2.task(":corepackYarnInstall").outcome == TaskOutcome.SUCCESS
        result2.task(":build").outcome == TaskOutcome.UP_TO_DATE

        when:
        def result3 = build("build")

        then:
        result3.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":corepackYarnInstall").outcome == TaskOutcome.UP_TO_DATE
        result3.task(":build").outcome == TaskOutcome.UP_TO_DATE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
