package io.swee.tvm.decompiler.api

interface TvmDecompilerFacade {
    fun decompileAddress(address: String): TvmDecompilerResult

    fun decompileBoc(boc: ByteArray): TvmDecompilerResult
}
