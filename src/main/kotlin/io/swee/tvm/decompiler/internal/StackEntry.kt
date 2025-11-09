package io.swee.tvm.decompiler.internal

sealed interface StackEntry {
    val type: TvmStackEntryType
    val name: StackEntryName

    data class Simple(override val type: TvmStackEntryType, override val name: StackEntryName) : StackEntry
}
