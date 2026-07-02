import com.github.gradle.buildlogic.GradleVersionData
import com.github.gradle.buildlogic.GradleVersionsCommandLineArgumentProvider
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    extra["nextVersion"] = "major"
}

plugins {
    `java-gradle-plugin`
    groovy
    `kotlin-dsl`
    idea
    jacoco
    id("com.gradle.plugin-publish") version "2.0.0"
    id("com.cinnober.gradle.semver-git") version "3.0.0"
    id("org.jetbrains.dokka") version "1.7.10"
}

group = "com.github.node-gradle"

val compatibilityVersion = JavaVersion.VERSION_1_8
val toolchainVersion = JavaLanguageVersion.of(17)

java {
    toolchain {
        languageVersion = toolchainVersion
    }

    targetCompatibility = compatibilityVersion
}

kotlin {
    jvmToolchain {
        languageVersion = toolchainVersion
    }

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_4
        jvmTarget = JvmTarget.JVM_1_8
    }
}

// tasks.compileKotlin {
//    kotlinOptions {
//        apiVersion = "1.3"
//        freeCompilerArgs = listOf("-Xno-optimized-callable-references")
//        jvmTarget = compatibilityVersion.toString()
//    }
//}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("cglib:cglib-nodep:3.3.0")
    testImplementation("org.objenesis:objenesis:3.5")
    testImplementation("commons-io:commons-io:2.22.0")
    testImplementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
    testImplementation("org.spockframework:spock-core")
    testImplementation("org.spockframework:spock-junit4")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
    testImplementation("org.mock-server:mockserver-netty:5.15.0")
}

tasks.compileTestGroovy {
    // Should be
    // classpath += files(sourceSets.test.get().kotlin.classesDirectory)
    // but unable to get it compile in the Kotlin DSL - works in the Groovy DSL as this
    // classpath += files(sourceSets.test.kotlin.classesDirectory)
    // classpath += files("${layout.buildDirectory}/kotlin/test")
    // This workaround works
    classpath += files(sourceSets.test.get().kotlin.classesDirectory)
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    systemProperty(
        GradleVersionsCommandLineArgumentProvider.PROPERTY_NAME,
        project.findProperty("testedGradleVersion") ?: gradle.gradleVersion
    )

    val processorsCount = Runtime.getRuntime().availableProcessors()
    val safeMaxForks = if (processorsCount > 2) processorsCount.div(2) else processorsCount
    maxParallelForks = safeMaxForks
    testLogging {
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }

    develocity {
        testDistribution {
            enabled = project.providers.gradleProperty("com.github.gradle.node.testdistribution").map { it.toBoolean() }.orElse(false)
            remoteExecutionPreferred = project.providers.gradleProperty("com.github.gradle.node.preferremote").map { it.toBoolean() }.orElse(false)
            if (project.providers.gradleProperty("com.github.gradle.node.remoteonly").getOrElse("false").toBoolean()) {
                maxLocalExecutors = 0
            }
        }

        predictiveTestSelection {
            enabled = project.providers.gradleProperty("com.github.gradle.node.predictivetestselection").map { it.toBoolean() }.orElse(false)
        }
    }
}

tasks.test {
    exclude("**/Pnpm*Test*")
}

tasks.register<Test>("unitTests") {
    exclude("**/*_integTest*")
}

tasks.register<Test>("integrationTests") {
    include("**/*_integTest*")
}

tasks.register<Test>("pnpmTests") {
    include("**/Pnpm*Test*")
}

tasks.register<Test>("testGradleReleases") {
    jvmArgumentProviders.add(
        GradleVersionsCommandLineArgumentProvider(GradleVersionData::getReleasedVersions)
    )
}

tasks.register("printVersions") {
    doLast {
        println(GradleVersionData.getReleasedVersions())
    }
}

tasks.register<Test>("testGradleNightlies") {
    jvmArgumentProviders.add(
        GradleVersionsCommandLineArgumentProvider(GradleVersionData::getNightlyVersions)
    )
}

tasks.register<JavaExec>("runParameterTest") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.github.gradle.node.util.PlatformHelperKt"
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            jdkVersion = compatibilityVersion.majorVersion.toInt()
        }
    }
}

gradlePlugin {
    website = "https://github.com/node-gradle/gradle-node-plugin"
    vcsUrl = "https://github.com/node-gradle/gradle-node-plugin"
    plugins {
        register("nodePlugin") {
            id = "com.github.node-gradle.node"
            implementationClass = "com.github.gradle.node.NodePlugin"
            displayName = "Gradle Node.js Plugin"
            description = "Gradle plugin for executing Node.js scripts. Supports npm, pnpm, Yarn and Bun."

            tags = listOf("java", "node", "node.js", "npm", "yarn", "pnpm", "bun")
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")

    develocity.testRetry {
        maxRetries = 3
        filter {
            includeClasses.add("*_integTest")
        }
    }
}
publishing.publications.withType<MavenPublication>().configureEach {
    pom.licenses {
        license {
            name = "Apache License, Version 2.0"
            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
        }
    }
}
