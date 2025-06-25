plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
}

group = "me.cubesicle"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.akuleshov7:ktoml-core:0.7.0")
    implementation("com.akuleshov7:ktoml-file:0.7.0")
    implementation("net.kyori:adventure-text-minimessage:4.23.0")
    implementation("net.minestom:minestom-snapshots:1_21_6-a40d7115d4")
    implementation ("org.slf4j:slf4j-simple:2.0.16")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "${project.group}.MainKt"
        }
    }
}