package io.swee.tvm.decompiler.cli

import java.io.File
import java.util.Base64

enum class BocFormat {
    AUTO,
    BINARY,
    BASE64,
    HEX
}

object BocDecoder {
    fun decode(input: String, format: BocFormat = BocFormat.AUTO): ByteArray {
        val detectedFormat = if (format == BocFormat.AUTO) detectFormat(input) else format
        val file = File(input)

        return when (detectedFormat) {
            BocFormat.BINARY -> readBinaryFile(file)
            BocFormat.BASE64 -> decodeBase64(input, file)
            BocFormat.HEX -> decodeHex(input, file)
            BocFormat.AUTO -> throw IllegalArgumentException("Could not auto-detect format for: $input")
        }
    }

    private fun detectFormat(input: String): BocFormat {
        val file = File(input)

        return when {
            file.exists() -> {
                when {
                    input.endsWith(".boc") -> BocFormat.BINARY
                    input.endsWith(".base64.txt") -> BocFormat.BASE64
                    input.endsWith(".hex") -> BocFormat.HEX
                    else -> BocFormat.BINARY
                }
            }
            isInlineBase64(input) -> BocFormat.BASE64
            isInlineHex(input) -> BocFormat.HEX
            else -> throw IllegalArgumentException("Could not detect format for input: $input")
        }
    }

    private fun isInlineBase64(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.matches(Regex("^[a-zA-Z0-9+/=_]+$")) &&
                trimmed.length > 10 &&
                !trimmed.contains(File.separator)
    }

    private fun isInlineHex(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.matches(Regex("^[0-9a-fA-F]+$")) &&
                trimmed.length % 2 == 0 &&
                !trimmed.contains(File.separator)
    }

    private fun readBinaryFile(file: File): ByteArray {
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: ${file.path}")
        }
        return file.readBytes()
    }

    private fun decodeBase64(input: String, file: File): ByteArray {
        val content = if (file.exists()) {
            file.readText().trim()
        } else {
            input.trim()
        }

        return try {
            Base64.getDecoder().decode(content)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid Base64 input: ${e.message}", e)
        }
    }

    private fun decodeHex(input: String, file: File): ByteArray {
        val content = if (file.exists()) {
            file.readText().trim()
        } else {
            input.trim()
        }

        return try {
            content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex input: ${e.message}", e)
        }
    }
}
