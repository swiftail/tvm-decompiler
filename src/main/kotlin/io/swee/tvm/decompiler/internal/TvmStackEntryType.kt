package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.instructions.Cp0InstructionRegistry

sealed class TvmStackEntryType(val typename: String) {
    data object SLICE : TvmStackEntryType("slice")
    data object CELL : TvmStackEntryType("cell")
    data object INT : TvmStackEntryType("int")
    data object BUILDER : TvmStackEntryType("builder")
    data object CONTINUATION : TvmStackEntryType("continuation")
    data class TUPLE(
        val elements: List<TvmStackEntryType>,
    ) : TvmStackEntryType(typename) {
        companion object {
            const val typename = "tuple"
        }
    }

    data object UNKNOWN : TvmStackEntryType("var");

    companion object {
        fun fromTvmDescription(value: Cp0InstructionRegistry.TvmInstSimpleStackEntry): TvmStackEntryType {
            if (value.value_types.size == 1) {
                when (value.value_types.single()) {
                    "Cell" -> return CELL
                    "Slice" -> return SLICE
                }
            }
            return UNKNOWN
        }
    }
}
