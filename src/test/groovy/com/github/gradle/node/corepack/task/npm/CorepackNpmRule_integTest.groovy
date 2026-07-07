package com.github.gradle.node.corepack.task.npm

import com.github.gradle.AbstractIntegTest
import com.github.gradle.node.Versions
import org.gradle.testkit.runner.TaskOutcome

import java.util.regex.Pattern

class CorepackNpmRule_integTest extends AbstractIntegTest {
    def 'execute npm_install rule (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
        ''')
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "packageManager": "npm@${Versions.TEST_NPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = buildTask('corepack_npm_install')

        then:
        result.outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'rules can be disabled (#gv.version)'() {
        given:
        gradleVersion = gv
        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
            
            node {
                enableTaskRules = false
            }
        ''')
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "packageManager": "npm@${Versions.TEST_NPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = buildAndFail('corepack_npm_install')

        then:
        result.output.contains("Task 'corepack_npm_install' not found")

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'can configure npm_ rule task (#gv.version)'() {
        given:
        gradleVersion = gv

        writeBuild('''
           plugins {
               id 'com.github.node-gradle.node'
           }

           npm_run_build {
               doFirst { project.logger.info('configured') }
           }
       ''')
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "packageManager": "npm@${Versions.TEST_NPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = buildTask('help')

        then:
        result.outcome == TaskOutcome.SUCCESS

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'can execute an npm module using npm_run_ (#gv.version)'() {
        given:
        gradleVersion = gv

        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
        ''')

        copyResources("fixtures/npm-missing/package.json", "package.json")

        when:
        def result = buildTask('corepack_npm_run_echoTest')

        then:
        result.outcome == TaskOutcome.SUCCESS
        fileExists('test.txt')

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }

    def 'succeeds to run npm module using npm_run_ when the package.json file contains local npm (#gv.version)'() {
        given:
        gradleVersion = gv

        writeBuild('''
            plugins {
                id 'com.github.node-gradle.node'
            }
        ''')
        writePackageJson("""{
            "name": "example",
            "dependencies": {},
            "scripts": {
                "npmVersion": "echo Version && corepack npm --version"
            },
            "packageManager": "npm@${Versions.TEST_NPM_DOWNLOAD_VERSION}"
        }
        """)

        when:
        def result = build('corepack_npm_run_npmVersion')

        then:
        result.task(":corepackNpmInstall").outcome == TaskOutcome.SUCCESS
        result.task(":corepack_npm_run_npmVersion").outcome == TaskOutcome.SUCCESS
        def versionPattern = Pattern.compile(".*Version\\s+${Versions.TEST_NPM_DOWNLOAD_REGEX}.*", Pattern.DOTALL)
        versionPattern.matcher(result.output).find()

        where:
        gv << GRADLE_VERSIONS_UNDER_TEST
    }
}
