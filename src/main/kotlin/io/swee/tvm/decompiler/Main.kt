package io.swee.tvm.decompiler

import io.swee.tvm.decompiler.internal.*
import java.math.BigInteger
import java.nio.charset.Charset
import kotlin.io.path.Path

fun main() {
   TvmDecompilerImpl.decompile(walletv4code)
}
