package io.swee.tvm.decompiler.cli

import io.swee.tvm.decompiler.api.TvmDecompilerResult
import java.io.File

object OutputWriter {

    fun write(result: TvmDecompilerResult, outputDir: File? = null, includeStdlib: Boolean = true) {
        if (outputDir == null) {
            writeToStdout(result, includeStdlib)
        } else {
            writeToDirectory(result, outputDir, includeStdlib)
        }
    }

    private fun writeToStdout(result: TvmDecompilerResult, includeStdlib: Boolean = true) {
        for (file in result.files) {
            if (!includeStdlib && file.name == "stdlib.fc") continue
            println(";;;; file: ${file.name}")
            println(file.content)
            println()
        }
    }

    private fun writeToDirectory(result: TvmDecompilerResult, outputDir: File, includeStdlib: Boolean = true) {
        outputDir.mkdirs()

        for (file in result.files) {
            if (!includeStdlib && file.name == "stdlib.fc") continue
            val outputFile = File(outputDir, file.name)
            outputFile.writeText(file.content)
            System.err.println("Wrote: ${outputFile.path}")
        }
    }
}
