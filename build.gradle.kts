plugins {
    kotlin("jvm") version "2.1.0"
    application
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

group = "io.swee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

application {
    mainClass = "io.swee.tvm.decompiler.MainKt"
}

dependencies {
    implementation(group = "com.github.espritoxyz.ton-disassembler", name = "tvm-disasm", version = "64366dba2b9ea0a8994248ca6249545d075552e7")
    implementation(group = "com.github.espritoxyz.ton-disassembler", name = "tvm-opcodes", version = "64366dba2b9ea0a8994248ca6249545d075552e7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("org.apache.commons:commons-collections4:4.5.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
