package io.swee.tvm.decompiler.internal

interface ParentContext {
    fun pop(): StackEntry
    fun backup(): ParentContext
}
