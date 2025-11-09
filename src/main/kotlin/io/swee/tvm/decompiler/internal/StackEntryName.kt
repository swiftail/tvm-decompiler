package io.swee.tvm.decompiler.internal

sealed interface StackEntryName {
    fun copy(): StackEntryName
    class Parent(val parent: StackEntryName, val suffix: String) : StackEntryName {
        override fun copy() = Parent(parent, suffix)
        override fun toString() = "${parent}_$suffix@${hashCode()}"
    }

    class Const(val value: String) : StackEntryName {
        override fun copy() = Const(value)
        override fun toString() = "$value@${hashCode()}"
    }
}
