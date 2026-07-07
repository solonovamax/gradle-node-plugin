package com.github.gradle.node.corepack.task.pnpm

import com.github.gradle.AbstractIntegTest
import com.github.gradle.node.Versions
import org.gradle.testkit.runner.TaskOutcome

class CorepackPnpmRule_integTest extends AbstractIntegTest {
    def 'execute corepack_pnpm_install rule (#gv.version)'() {
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
        ''')
        writeEmptyPackageJson()

        when:
        def result = buildTask('corepack_pnpm_install')

        then:
        result.outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'Use local pnpm version (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild("""
            plugins {
                id 'com.github.node-gradle.node'
            }
            node {
                download = true
            }
        """)
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = build('corepack_pnpm_--version')

        then:
        result.output =~ Versions.TEST_PNPM_DOWNLOAD_REGEX
        result.task(':corepack_pnpm_--version').outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'can execute an pnpm module using corepack_pnpm_run_ (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }

            node {
                download = true
            }
        ''')

        copyResources('fixtures/npm-missing/package.json', 'package.json')

        when:
        def result = buildTask('corepack_pnpm_run_echoTest')

        then:
        result.outcome == TaskOutcome.SUCCESS
        fileExists('test.txt')

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'can execute subtasks using pnpm (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
            node {
                download = true
            }
        ''')
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "scripts": {
                "parent" : "echo 'parent1' > parent1.txt && pnpm run child1 && pnpm run child2 && echo 'parent2' > parent2.txt",
                "child1": "echo 'child1' > child1.txt",
                "child2": "echo 'child2' > child2.txt"
            },
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = buildTask('corepack_pnpm_run_parent')

        then:
        result.outcome == TaskOutcome.SUCCESS
        fileExists('parent1.txt')
        fileExists('child1.txt')
        fileExists('child2.txt')
        fileExists('parent2.txt')

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'Custom workingDir (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild("""
            plugins {
                id 'com.github.node-gradle.node'
            }
            node {
                download = true
                nodeProjectDir = file("frontend")
            }
        """)
        writeFile('frontend/package.json', """{
            "name": "example",
            "dependencies": {},
            "scripts": {
                "whatVersion": "pnpm --version"
            },
            "packageManager": "pnpm@${Versions.TEST_PNPM_DOWNLOAD_VERSION}"
        }""")

        when:
        def result = build('corepack_pnpm_run_whatVersion')

        then:
        result.output =~ Versions.TEST_PNPM_DOWNLOAD_REGEX
        result.task(':corepack_pnpm_run_whatVersion').outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
