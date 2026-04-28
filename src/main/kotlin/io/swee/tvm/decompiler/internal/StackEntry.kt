package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmCell
import org.ton.bytecode.TvmInst

sealed interface ConcreteValue {
    data class IntVal(val value: String) : ConcreteValue
    data class SliceVal(val value: TvmCell) : ConcreteValue
    data class ContinuationVal(val instructions: List<TvmInst>) : ConcreteValue
}

sealed interface StackEntry {
    val type: TvmStackEntryType
    val name: StackEntryName
    val concreteValue: ConcreteValue?

    data class Simple(
        override val type: TvmStackEntryType,
        override val name: StackEntryName,
        override val concreteValue: ConcreteValue? = null
    ) : StackEntry

    companion object {
        fun merge(entries: List<StackEntry>, name: StackEntryName = StackEntryName.Const("phi")): StackEntry? {
            if (entries.isEmpty()) return null
            val first = entries.first()
            if (entries.all { it === first }) return first

            val agreedValue = entries.map { it.concreteValue }.distinct().singleOrNull()
            return Simple(
                type = first.type,
                name = name,
                concreteValue = agreedValue
            )
        }
    }
}
