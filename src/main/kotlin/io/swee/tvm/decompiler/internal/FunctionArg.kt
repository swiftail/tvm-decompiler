package io.swee.tvm.decompiler.internal

data class FunctionArg(
    val type: TvmStackEntryType,
    var name: String?
)
