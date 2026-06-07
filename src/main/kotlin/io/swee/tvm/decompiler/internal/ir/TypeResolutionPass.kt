package io.swee.tvm.decompiler.internal.ir

import io.swee.tvm.decompiler.internal.StackEntry
import io.swee.tvm.decompiler.internal.TvmStackEntryType

object TypeResolutionPass {
    fun run(function: IRNode.Function, seeds: Map<StackEntry, TvmStackEntryType>) {
        val resolved = TypeSolver.solve(function.codeBlock, seeds)
        for ((entry, type) in resolved) {
            if (type != TvmStackEntryType.UNKNOWN) {
                entry.type = type
            }
        }
    }
}
