package io.swee.tvm.decompiler.internal


fun CodeBlockContext.popSlice() = pop(TvmStackEntryType.SLICE.typename)
fun CodeBlockContext.popCell() = pop(TvmStackEntryType.CELL.typename)
fun CodeBlockContext.popInt() = pop(TvmStackEntryType.INT.typename)
fun CodeBlockContext.popBuilder() = pop(TvmStackEntryType.BUILDER.typename)
fun CodeBlockContext.popTuple() = pop(TvmStackEntryType.TUPLE.typename)
fun CodeBlockContext.popContinuation() = pop(TvmStackEntryType.CONTINUATION.typename)
fun CodeBlockContext.popAny() = pop(TvmStackEntryType.UNKNOWN.typename)


fun newSlice(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.SLICE, name)
fun newCell(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.CELL, name)
fun newInt(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.INT, name)
fun newBuilder(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.BUILDER, name)
fun newTuple(name: StackEntryName, elements: List<TvmStackEntryType>) = StackEntry.Simple(TvmStackEntryType.TUPLE(elements), name)
fun newContinuation(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.CONTINUATION, name)
fun newUnknown(name: StackEntryName) = StackEntry.Simple(TvmStackEntryType.UNKNOWN, name)

fun name(name: String) = StackEntryName.Const(name)
fun copy(entry: StackEntry) = StackEntry.Simple(entry.type, entry.name.copy())
