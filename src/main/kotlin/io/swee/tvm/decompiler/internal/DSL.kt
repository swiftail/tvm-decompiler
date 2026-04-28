package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmInst

fun newContinuation(name: StackEntryName, instructions: List<TvmInst>) = StackEntry.Simple(
    TvmStackEntryType.CONTINUATION, name, ConcreteValue.ContinuationVal(instructions)
)
fun newUnknown(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.UNKNOWN, name)

fun name(name: String) = StackEntryName.Const(name)

