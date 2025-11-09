package io.swee.tvm.decompiler.internal

import java.util.*

class InheritedStackTracker private constructor(val entries: LinkedList<StackEntry>, val usedEntries: LinkedList<StackEntry>) :
    ParentContext {
    constructor(
        args: List<MethodArg>
    ) : this(
        args.mapTo(LinkedList()) {
            StackEntry.Simple(it.type, it.name?.let {
                StackEntryName.Const(it)
            } ?: StackEntryName.Const("arg"))
        },
        LinkedList()
    )

    override fun pop(): StackEntry {
        val entry = entries.pop()
        usedEntries.add(entry)
        return entry
    }

    override fun backup(): ParentContext {
        return InheritedStackTracker(LinkedList(entries), LinkedList(usedEntries))
    }
}
