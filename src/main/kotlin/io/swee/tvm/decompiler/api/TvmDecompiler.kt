package io.swee.tvm.decompiler.api

interface TvmDecompiler {
    fun decompile(boc: ByteArray): TvmDecompilerResult
}
