plugins {
    kotlin("multiplatform") version "2.2.0"
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
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.fischerabruzese.graphvcli.main"
            }
        }
    }

    sourceSets {
        // Common code shared between JVM and Native
        val commonMain by getting {}

        // JVM-specific code (JavaFX)
        val jvmMain by getting {
            dependencies {
                implementation("org.openjfx:javafx-controls:24:linux")
                implementation("org.openjfx:javafx-fxml:24:linux")
                implementation("org.openjfx:javafx-base:24:linux")
                implementation("org.openjfx:javafx-graphics:24:linux")
            }
        }

        // Native-specific code (CLI with Clikt)
        val linuxX64Main by getting {
            dependencies {
                implementation("com.github.ajalt.clikt:clikt:5.0.3")
            }
        }
    }
}

afterEvaluate {
    tasks.named<Jar>("jvmJar") {
        archiveBaseName.set("fischerabruzese")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = "com.fischerabruzese.graphsFX.MainKt"
        }
        from(
            configurations.getByName("jvmRuntimeClasspath").map {
                if (it.isDirectory) it else zipTree(it)
            },
        )
        from(
            kotlin
                .jvm()
                .compilations
                .getByName("main")
                .output.allOutputs,
        )
    }
}

group = "com.fischerabruzese"
version = "1.0-SNAPSHOT"
description = "Graphs"
