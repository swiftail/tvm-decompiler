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
        fun fromTvmStackEntryDescription(value: Cp0InstructionRegistry.TvmCp0InstValueFlowOutputsEntry.Simple): TvmStackEntryType {
            return when (value.valueTypes?.singleOrNull()) {
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.INT -> INT
                else -> UNKNOWN
            }
        }
        fun fromTvmBytecodeOperandDescription(value: Cp0InstructionRegistry.TvmCp0InstBytecodeOperand): TvmStackEntryType {
            return when (value.type) {
                Cp0InstructionRegistry.TvmCp0InstBytecodeOperandType.UINT -> INT
                else -> UNKNOWN
            }
        }
    }
}
