package com.github.gradle.node.corepack.task.pnpm

import com.github.gradle.AbstractIntegTest
import com.github.gradle.node.Versions
import org.gradle.testkit.runner.TaskOutcome

class CorepackPnpmInstall_integTest extends AbstractIntegTest {
    def 'install packages with corepack + pnpm (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
                workDir = file('build/node')
                pnpmWorkDir = file('build/pnpm')
            }
        ''')
        writePackageJson(""" {
            "name": "example",
            "dependencies": {},
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)
        writeFile("pnpm-lock.yaml", "lockfileVersion: '6.0'")

        when:
        def result = build('corepackPnpmInstall')

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build('corepackPnpmInstall')

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        // because pnpm-lock.yaml is created only when needed
        result.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'install packages with pnpm and postinstall task requiring pnpm and node (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
            node {
                download = true
                workDir = file('build/node')
                pnpmWorkDir = file('build/pnpm')
            }
        ''')
        writePackageJson(""" {
            "name": "example",
            "dependencies": {},
            "versionOutput" : "node --version",
            "postinstall" : "pnpm run versionOutput",
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)
        writeFile("pnpm-lock.yaml", "lockfileVersion: '6.0'")

        when:
        def result = build('corepackPnpmInstall', '--info')

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.SUCCESS
        result.task(":corepackSetup").outcome == TaskOutcome.SUCCESS
        result.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        result = build('corepackPnpmInstall')

        then:
        result.task(":nodeSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":corepackSetup").outcome == TaskOutcome.UP_TO_DATE
        result.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'install packages with pnpm in different directory (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
                workDir = file('build/node')
                pnpmWorkDir = file('build/pnpm')
                nodeProjectDir = file('subdirectory')
            }
        ''')
        writeFile('subdirectory/package.json', """{
            "name": "example",
            "dependencies": {
            },
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }""")

        when:
        def result = build('corepackPnpmInstall')

        then:
        result.task(':corepackPnpmInstall').outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'verify pnpm install inputs/outputs (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
                workDir = file('build/node')
                npmInstallCommand = 'install'
            }

            def lock = file('pnpm-lock.yaml')
            def installTask = tasks.named("corepackPnpmInstall").get()
            def outputs = installTask.outputs.files
            def inputs = installTask.inputs.files
            task verifyIO {
                doLast {
                    if (!outputs.contains(lock)) {
                        throw new RuntimeException("pnpm-lock.yaml is not in INSTALL'S outputs!")
                    }
                    if (inputs.contains(lock)) {
                        throw new RuntimeException("pnpm-lock.yaml is in INSTALL'S inputs!")
                    }
                }
            }
        ''')
        writePackageJson(""" {
            "name": "example",
            "dependencies": {},
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)
        writeFile('pnpm-lock.yaml', '')

        when:
        def result = buildTask('verifyIO')

        then:
        result.outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'verify output configuration (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
                workDir = file('build/node')
                pnpmWorkDir = file('build/pnpm')
            }
        ''')
        writePackageJson("""
            {
              "name": "hello",
              "dependencies": {
                "mocha": "6.2.0"
              },
              "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
            }
        """)

        when:
        def result1 = build("corepackPnpmInstall")

        then:
        result1.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        def result2 = build("corepackPnpmInstall")

        then:
        // Because pnpm-lock.yaml was created
        result2.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        // Let's add a file in the node_modules directory
        writeFile("node_modules/mocha/newFile.txt", "hello")
        def result3 = build("corepackPnpmInstall")

        then:
        // It should not make the build out-of-date
        result3.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's update a file in the node_modules directory
        createFile("node_modules/mocha/README.md").delete()
        writeFile("node_modules/mocha/README.md", "modified README")
        def result4 = build("corepackPnpmInstall")

        then:
        // This time the build should not be up-to-date and the file could (but it's not) reset
        result4.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS
        createFile("node_modules/mocha/README.md").text == "modified README"

        when:
        // Let's delete the node_modules directory
        createFile("node_modules").delete()
        def result5 = build("corepackPnpmInstall")

        then:
        // This time the build should not be up-to-date and the file should be reset
        result5.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE
        createFile("node_modules/mocha/package.json").exists()

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'verify output configuration when filtering node_modules output (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
                workDir = file('build/node')
            }

            corepackPnpmInstall {
                nodeModulesOutputFilter { 
                    exclude("mocha")
                }
            }
        ''')
        writePackageJson("""
            {
              "name": "hello",
              "dependencies": {
                "mocha": "6.2.0"
              },
              "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
            }
        """)

        when:
        createFile("node_modules").deleteDir()
        def result1 = build("corepackPnpmInstall")

        then:
        result1.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        // Let's add a file in the node_modules directory
        writeFile("node_modules/mocha/newFile.txt", "hello")
        def result2 = build("corepackPnpmInstall")

        then:
        // It should make the build out-of-date
        result2.task(":corepackPnpmInstall").outcome == TaskOutcome.SUCCESS

        when:
        // Let's update a file in the node_modules directory
        createFile("node_modules/mocha").delete()
        def result3 = build("corepackPnpmInstall")

        then:
        // The build should still be up-to-date
        result3.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE

        when:
        // Let's delete an excluded file in the node_modules directory
        createFile("node_modules/mocha").delete()
        def result4 = build("corepackPnpmInstall")

        then:
        // The build should still be up-to-date
        result4.task(":corepackPnpmInstall").outcome == TaskOutcome.UP_TO_DATE

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
