import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "MainKt"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    defaultCharacterEncoding = "UTF-8"
    jvmArgs("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}
