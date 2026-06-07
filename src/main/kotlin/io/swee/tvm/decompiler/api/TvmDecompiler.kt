package io.swee.tvm.decompiler.api

import io.swee.tvm.decompiler.internal.DecompilerOptions

interface TvmDecompiler {
    fun decompile(boc: ByteArray, options: DecompilerOptions = DecompilerOptions()): TvmDecompilerResult
}
