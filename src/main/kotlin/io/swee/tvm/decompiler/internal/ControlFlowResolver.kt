package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.ir.IRNode
import java.util.*

object ControlFlowResolver {
    // if else
    fun resolveForward(
        ctx: IrBlockBuilder,
        newNodes: List<IRNode>,
        finalized: List<IrBlockBuilder>,
        fallthrough: IrBlockBuilder? = null,
        used: List<IrBlockBuilder> = listOf()
    ) {
        val allBranches = buildList {
            fallthrough?.let(::add)
            addAll(finalized)
        }
        val headBranch = allBranches.firstOrNull() ?: error("No finalized entry")
        val tailBranches = allBranches.drop(1)

        for (branch in (allBranches + used).distinct()) {
            ctx.upstream.trackDiscoveries(branch.upstream)
        }

        ensureStackDepths(headBranch, tailBranches)

        val mergedStack = LinkedList<StackEntry>()

        val sampleDepth = headBranch.stackDepth()

        for (i in 0 until sampleDepth) {
            val entriesAtPosition = allBranches.map { it.stackFetch(i) }
            val sampleEntry = headBranch.stackFetch(i)

            val merged = StackEntry.merge(entriesAtPosition)!!
            if (merged === entriesAtPosition.first()) {
                mergedStack.add(merged)
            } else {
                mergedStack.add(merged)

                val declaration = IRNode.VariableDeclaration(
                    entries = listOf(merged),
                    value = if (fallthrough == null) {
                        IRNode.IntLiteral(sampleEntry.type.default()) // TODO
                    } else {
                        IRNode.VariableUsage(sampleEntry, tracked = true)
                    }
                )

                ctx.appendNode(declaration)

                allBranches.forEachIndexed { branchIndex, branchBuilder ->
                    val valueInBranch = entriesAtPosition[branchIndex]

                    branchBuilder.appendNode(
                        IRNode.VariableDeclaration(
                            listOf(merged),
                            IRNode.VariableUsage(valueInBranch, tracked = true),
                            reassignment = true
                        )
                    )
                }
            }
        }

        ctx.stackReplace(mergedStack)
        ctx.mergeUpstreams(finalized, fallthrough, used)

        for (newNode in newNodes) {
            ctx.appendNode(newNode)
        }
    }

    data class BackwardUpdate(
        val entry: StackEntry,
        val i: Int
    )
    // while
    fun preResolveBackward(
        ctx: IrBlockBuilder,
        after: IrBlockBuilder,
        until: Boolean
    ): List<BackwardUpdate>? {
        val afterStack = if (until) {
            after.stackCopy().drop(1)
        } else {
            after.stackCopy()
        }

        if (afterStack.size > ctx.stackDepth()) {
            ctx.stackEnsureAtLeast(afterStack.size)
            return null
        }

        check(ctx.stackDepth() == afterStack.size) {
            "Backward flow stack depth mismatch"
        }

        val mergedStack = LinkedList<StackEntry>()
        val updates = mutableListOf<BackwardUpdate>()

        val stackDepth = ctx.stackDepth()
        for (i in 0 until stackDepth) {
            val startVal = ctx.stackFetch(i)
            val endVal = afterStack[i]

            val merged = StackEntry.merge(listOf(startVal, endVal))!!
            if (merged === startVal) {
                mergedStack.add(startVal)
            } else {
                mergedStack.add(merged)

                ctx.appendNode(IRNode.VariableDeclaration(
                    entries = listOf(merged),
                    value = IRNode.VariableUsage(startVal, tracked = true)
                ))

                updates.add(BackwardUpdate(merged, i))
            }
        }

        ctx.stackReplace(mergedStack)

        return updates
    }

    fun postResolveBackward(
        ctx: IrBlockBuilder,
        after: IrBlockBuilder,
        newNodes: (IrBlockBuilder) -> List<IRNode>,
        backwardUpdate: List<BackwardUpdate>,
        until: Boolean
    ) {
        val afterStack = if (until) {
            after.stackCopy().drop(1)
        } else {
            after.stackCopy()
        }

        for (update in backwardUpdate.reversed()) {
            val newValue = afterStack[update.i]

            after.appendNode(IRNode.VariableDeclaration(
                entries = listOf(update.entry),
                value = IRNode.VariableUsage(newValue, tracked = true),
                reassignment = true
            ))
        }

        for (newNode in newNodes(after)) {
            ctx.appendNode(newNode)
        }
        ctx.mergeUpstreams(listOf(after))
    }

    private fun ensureStackDepths(
        head: IrBlockBuilder,
        tail: List<IrBlockBuilder>
    ) {
        val allBranches = listOf(head) + tail
        val maxDepth = allBranches.maxOf { it.stackDepth() }

        for (item in tail) {
            check(head.stackDepth() == item.stackDepth()) {
                "Stack size mismatch: head: ${head.stackDepth()}, tail: ${item.stackDepth()}"
            }
        }
    }
}
