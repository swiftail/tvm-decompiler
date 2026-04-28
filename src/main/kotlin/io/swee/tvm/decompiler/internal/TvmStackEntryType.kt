package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.instructions.Cp0InstructionRegistry
import io.swee.tvm.decompiler.internal.ir.IRNode

sealed class TvmStackEntryType(val typename: String) {

    open val funcTypename: String get() = typename

    // todo need to call actual stuff from stdlib here :(
    data object SLICE : TvmStackEntryType("slice") {
        override fun default(): String {
            return "\"\"";
        }
    }
    data object CELL : TvmStackEntryType("cell") {
        // TODO
        override fun default(): String {
            return "begin_cell().end_cell()"
        }
    }
    data object INT : TvmStackEntryType("int") {
        override fun default(): String {
            return "0"
        }
    }
    data object BUILDER : TvmStackEntryType("builder") {
        // TODO
        override fun default(): String {
            return "begin_cell()"
        }
    }
    data object CONTINUATION : TvmStackEntryType("continuation") {
        override fun default(): String {
            return "null()"
        }
    }

    data class TUPLE(
        val elements: List<TvmStackEntryType>,
    ) : TvmStackEntryType(typename) {
        companion object {
            const val typename = "tuple"
        }

        override val funcTypename: String
            get() = if (elements.isNotEmpty()) "[${elements.joinToString(", ") { it.funcTypename }}]" else "tuple"

        override fun default(): String {
            return "[${elements.joinToString(", ") { it.default() }}]"
        }
    }

    data object UNKNOWN : TvmStackEntryType("var") {
        override fun default(): String {
            return "null()"
        }
    }

    abstract fun default(): String

    companion object {
        fun fromTvmStackEntryDescription(value: Cp0InstructionRegistry.TvmCp0InstValueFlowOutputsEntry.Simple): TvmStackEntryType {
            val types = value.valueTypes ?: return UNKNOWN
            if (types.size == 2) {
                val nonNull = types.filter { it != Cp0InstructionRegistry.TvmCp0InstStackEntryType.NULL }
                if (nonNull.size == 1) return fromTvmStackEntryType(nonNull.single(), value.tupleElements)
            }
            return fromTvmStackEntryType(types.singleOrNull(), value.tupleElements)
        }
        fun fromTvmStackEntryType(
            value: Cp0InstructionRegistry.TvmCp0InstStackEntryType?,
            tupleElements: List<Cp0InstructionRegistry.TvmCp0InstStackEntryType>? = null
        ): TvmStackEntryType {
            return when (value) {
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.INT -> INT
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.CELL -> CELL
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.SLICE -> SLICE
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.TUPLE -> TUPLE(
                    tupleElements?.map { fromTvmStackEntryType(it) } ?: listOf()
                )
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.BUILDER -> BUILDER
                Cp0InstructionRegistry.TvmCp0InstStackEntryType.CONTINUATION -> CONTINUATION
                else -> UNKNOWN
            }
        }
        fun fromTypename(typename: String): TvmStackEntryType {
            return when (typename) {
                "int" -> INT
                "cell" -> CELL
                "slice" -> SLICE
                "builder" -> BUILDER
                "continuation" -> CONTINUATION
                "tuple" -> TUPLE(listOf())
                else -> UNKNOWN
            }
        }

        fun fromTvmBytecodeOperandDescription(value: Cp0InstructionRegistry.TvmCp0InstBytecodeOperand): TvmStackEntryType {
            return when (value.type) {
                Cp0InstructionRegistry.TvmCp0InstBytecodeOperandType.UINT -> INT
                Cp0InstructionRegistry.TvmCp0InstBytecodeOperandType.INT -> INT
                Cp0InstructionRegistry.TvmCp0InstBytecodeOperandType.SUBSLICE -> SLICE
                else -> UNKNOWN
            }
        }
    }
}
