import java.io.File

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
    implementation(
        group = "com.github.espritoxyz.ton-disassembler",
        name = "tvm-disasm",
        version = "64366dba2b9ea0a8994248ca6249545d075552e7"
    )
    implementation(
        group = "com.github.espritoxyz.ton-disassembler",
        name = "tvm-opcodes",
        version = "64366dba2b9ea0a8994248ca6249545d075552e7"
    )
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("org.ton.ton4j:cell:1.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    dependsOn("roundTripTest")
}
kotlin {
    jvmToolchain(17)
}

fun compileFuncToBoc(fcFile: File, outputDir: File): File {
    val bocFile = File(outputDir, "${fcFile.nameWithoutExtension}.boc")
    val bocBase64Output = File(outputDir, "${fcFile.nameWithoutExtension}.boc.base64.txt")
    val fiftOutput = File(outputDir, "${fcFile.nameWithoutExtension}.fift")

    bocFile.parentFile?.mkdirs()

    val stdlibFile = file("func_sources/stdlib.fc")

    val args = mutableListOf(
        "func-js",
        fcFile.absolutePath
    )

    if (stdlibFile.exists()) {
        args.add(stdlibFile.absolutePath)
    }

    args.addAll(
        listOf(
            "--boc", bocFile.absolutePath,
            "--boc-base64", bocBase64Output.absolutePath,
            "--fift", fiftOutput.absolutePath
        )
    )

    println("EXECUTING CMD: ${args.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }}")

    exec {
        commandLine(args)
    }

    return bocFile
}

tasks.register("compileFunc") {
    group = "build"
    description = "Compiles a FunC source file using func-js. Usage: ./gradlew compileFunc -PinputFile=path/to/file.fc"

    val inputFile = project.findProperty("inputFile") as String?
    if (inputFile == null) {
        throw GradleException("Input file not specified. Usage: ./gradlew compileFunc -PinputFile=path/to/file.fc")
    }

    val inputFileFile = file(inputFile)
    if (!inputFileFile.exists()) {
        throw GradleException("Input file does not exist: $inputFile")
    }

    val outputDir = layout.buildDirectory.dir("func-out").get().asFile

    inputs.file(inputFileFile)

    doFirst {
        outputDir.mkdirs()
    }

    doLast {
        compileFuncToBoc(inputFileFile, outputDir)
    }
}

tasks.register("roundTripTest") {
    group = "verification"
    description =
        "Round-trip test: compile FunC -> BOC -> decompile -> recompile -> compare. Usage: ./gradlew roundTripTest -PinputFile=path/to/file.fc"

    // Ensure JAR is built before running tests
    dependsOn("shadowJar")

    val inputFile = project.findProperty("inputFile") as String?
    val testFiles = if (inputFile != null) {
        listOf(file(inputFile))
    } else {
        fileTree("func_sources") {
            include("*.fc")
            exclude("stdlib.fc")
        }.files.toList()
    }

    inputs.files(testFiles)
    outputs.dir(layout.buildDirectory.dir("roundtrip"))

    doLast {
        val outputDir = layout.buildDirectory.dir("roundtrip").get().asFile
        outputDir.mkdirs()

        for (fcFile in testFiles) {
            println("Testing: ${fcFile.name}")

            val step1Boc = compileFuncToBoc(fcFile, outputDir)

            val shadowJar = file("build/libs/tvm-decompiler-1.0-SNAPSHOT-all.jar")
            val decompiledDir = File(outputDir, "${fcFile.name}-decompiled")
            decompiledDir.mkdirs()

            exec {
                commandLine(
                    "java", "-jar", shadowJar.absolutePath,
                    "boc", step1Boc.absolutePath,
                    "-o", decompiledDir.absolutePath
                )
            }

            // Print decompiled files to console for inspection
            println("  --- Decompiled files ---")
            decompiledDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                println("  ;;;; ${file.name}")
                file.forEachLine { line -> println("  $line") }
                println()
            }

            val mainFc = File(decompiledDir, "main.fc")
            if (!mainFc.exists()) {
                throw GradleException("Decompiled main.fc not found for ${fcFile.name}")
            }
            val step2Boc = compileFuncToBoc(mainFc, outputDir)

            if (!step1Boc.readBytes().contentEquals(step2Boc.readBytes())) {
                // todo git diff here
                throw GradleException("Round-trip failed for ${fcFile.name}: BOCs don't match")
            }
            println("  ✓ Round-trip successful")
        }
    }
}

// Run decompiler with debugging support
// Usage: ./gradlew runDecompiler -PinputFile=func_sources/1.fc [-PoutputDir=/tmp/out]
tasks.register<JavaExec>("runDecompiler") {
    group = "application"
    description =
        "Run decompiler with auto-compile: ./gradlew runDecompiler -PinputFile=path/to/file.fc [-PoutputDir=/tmp/out]"

    val inputFile = project.findProperty("inputFile") as String?
    if (inputFile == null) {
        throw GradleException("Usage: ./gradlew runDecompiler -PinputFile=path/to/file.fc [-PoutputDir=/tmp/out]")
    }

    val inputFileFile = file(inputFile)
    if (!inputFileFile.exists()) {
        throw GradleException("Input file does not exist: $inputFile")
    }

    val bocFile = if (inputFileFile.extension == "fc") {
        val outputDir = layout.buildDirectory.dir("decompiler-boc").get().asFile
        outputDir.mkdirs()
        compileFuncToBoc(inputFileFile, outputDir)
        File(outputDir, "${inputFileFile.nameWithoutExtension}.boc")
    } else {
        inputFileFile
    }

//    val outputDir = project.findProperty("outputDir")?.let { file(it) }
//        ?: layout.buildDirectory.dir("decompiler-out").get().asFile

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.swee.tvm.decompiler.MainKt")
    args = listOf("boc", bocFile.absolutePath, "-n")
}

