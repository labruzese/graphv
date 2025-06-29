plugins {
    kotlin("multiplatform") version "2.2.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

kotlin {
    // JVM target for JavaFX application
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
        }
    }

    // Native Linux target for CLI application
    linuxX64("native") {
        binaries {
            executable {
                entryPoint = "com.fischerabruzese.graphvcli.main"
            }
        }
    }

    sourceSets {
        // Common code shared between JVM and Native
        val commonMain by getting {
            dependencies {
                // No external dependencies for common code
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // JVM-specific code (JavaFX)
        val jvmMain by getting {
            dependencies {
                // JavaFX dependencies are handled by the JavaFX plugin
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Native-specific code (CLI with Clikt)
        val nativeMain by getting {
            dependencies {
                implementation("com.github.ajalt.clikt:clikt:5.0.3")
            }
        }

        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// JavaFX configuration (only applies to JVM)
javafx {
    version = "24"
    modules = listOf("javafx.controls", "javafx.fxml")
}

// Task to run the JavaFX application
tasks.register<JavaExec>("runJvmApp") {
    dependsOn(tasks.named("jvmJar"))
    classpath = configurations["jvmRuntimeClasspath"] + files(tasks.named("jvmJar"))
    mainClass.set("com.fischerabruzese.graphsFX.MainKt")
}

// Configure JVM JAR task
tasks.named<Jar>("jvmJar") {
    manifest {
        attributes["Main-Class"] = "com.fischerabruzese.graphsFX.MainKt"
    }
}

group = "com.fischerabruzese"
version = "1.0-SNAPSHOT"
description = "Graphs"
