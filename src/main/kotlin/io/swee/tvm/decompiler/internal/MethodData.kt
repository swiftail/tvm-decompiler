package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmInst

data class MethodData(
    val id: String,
    val instructions: List<TvmInst>,
    val args: List<MethodArg>
)
