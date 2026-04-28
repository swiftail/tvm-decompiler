package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.*

private fun StackEntry.continuationInstructions(): List<TvmInst> =
    (concreteValue as? ConcreteValue.ContinuationVal)?.instructions
        ?: error("Dynamic continuation: no concrete instruction list")

fun parseContinuation(
    registry: ParserRegistry,
    parent: IrBlockBuilder,
    instList: List<TvmInst>,
    isExpression: Boolean = false,
    isReturn: Boolean = false
): Pair<IRNode.CodeBlock, IrBlockBuilder> {
    val childCtx = parent.fork(isExpression)
    childCtx.isReturnContext = isReturn
    val block = TvmDecompilerImpl.parseCodeBlock(
        registry,
        childCtx,
        instList,
        isReturn
    )
    return block to childCtx
}

fun parseCallContinuation(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,
    instList: List<TvmInst>
) {
    val parentStackBefore = ctx.stackCopy()
    val childCtx = ctx.fork()
    TvmDecompilerImpl.parseCodeBlock(registry, childCtx, instList, false)

    val childStack = childCtx.stackCopy()

    val parentDepth = parentStackBefore.size
    val childDepth = childStack.size

    var matchCount = 0
    val minDepth = minOf(parentDepth, childDepth)
    for (i in 0 until minDepth) {
        if (parentStackBefore[parentDepth - 1 - i] === childStack[childDepth - 1 - i]) {
            matchCount++
        } else {
            break
        }
    }

    val consumed = parentDepth - matchCount
    val produced = childDepth - matchCount

    if (consumed == 0 && produced == 0) {
        val block = childCtx.build()
        for (entry in block.entries) {
            ctx.appendNode(entry)
        }
        ctx.mergeUpstreams(listOf(childCtx))
        return
    }

    repeat(consumed) { ctx.stackPop() }

    val returnEntries = (0 until produced).map { childStack[it] }

    val block = childCtx.build()
    for (entry in block.entries) {
        ctx.appendNode(entry)
    }

    for (entry in returnEntries.reversed()) {
        ctx.stackPush(entry)
    }
    ctx.mergeUpstreams(listOf(childCtx))
}

fun parseWhileBlock(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,
    condEntry: StackEntry,
    bodyEntry: StackEntry,
) {
    val bodyContinuation = bodyEntry.continuationInstructions()
    val condContinuation = condEntry.continuationInstructions()

    val (_, dryRunBodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )

    var backwardUpdates = ControlFlowResolver.preResolveBackward(
        ctx,
        dryRunBodyCtx,
        false
    )
    if (backwardUpdates != null &&
        dryRunBodyCtx.upstream.getUsedEntries().size > ctx.upstream.getUsedEntries().size) {
        val extraNeeded = dryRunBodyCtx.upstream.getUsedEntries().size - ctx.upstream.getUsedEntries().size
        ctx.stackEnsureAtLeast(ctx.stackDepth() + extraNeeded)
        backwardUpdates = null
    }
    if (backwardUpdates == null) {
        val (_, dryRunBodyCtxCorrected) = parseContinuation(
            registry,
            ctx,
            bodyContinuation
        )
        backwardUpdates = ControlFlowResolver.preResolveBackward(
            ctx,
            dryRunBodyCtxCorrected,
            false
        ) ?: error("Stack depth mismatch")
    }

    val (condNode, _) = parseContinuation(
        registry,
        ctx,
        condContinuation,
        isExpression = true
    )
    val (_, bodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )

    ControlFlowResolver.postResolveBackward(
        ctx,
        bodyCtx,
        { newBodyCtx ->
            listOf(IRNode.WhileLoop(
                condNode,
                newBodyCtx.build()
            ) )
        },
        backwardUpdates,
        false
    )
}

fun parseUntilBlock(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,
    bodyEntry: StackEntry
) {
    val bodyContinuation = bodyEntry.continuationInstructions()

    val (_, dryRunBodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )

    var backwardUpdates = ControlFlowResolver.preResolveBackward(
        ctx,
        dryRunBodyCtx,
        true
    )
    if (backwardUpdates != null &&
        dryRunBodyCtx.upstream.getUsedEntries().size > ctx.upstream.getUsedEntries().size) {
        val extraNeeded = dryRunBodyCtx.upstream.getUsedEntries().size - ctx.upstream.getUsedEntries().size
        ctx.stackEnsureAtLeast(ctx.stackDepth() + extraNeeded)
        backwardUpdates = null
    }
    if (backwardUpdates == null) {
        val (_, dryRunBodyCtxCorrected) = parseContinuation(
            registry,
            ctx,
            bodyContinuation
        )
        backwardUpdates = ControlFlowResolver.preResolveBackward(
            ctx,
            dryRunBodyCtxCorrected,
            true
        ) ?: error("Stack depth mismatch")
    }

    val (_, bodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )
    val condEntry = bodyCtx.stackFetch(0)

    ControlFlowResolver.postResolveBackward(
        ctx,
        bodyCtx,
        { newBodyCtx ->
            listOf(IRNode.UntilLoop(
                newBodyCtx.build(),
                IRNode.VariableUsage(condEntry, tracked = true)
            ) )
        },
        backwardUpdates,
        true
    )
}

fun parseRepeatBlock(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,
    countEntry: StackEntry,
    bodyEntry: StackEntry
) {
    val bodyContinuation = bodyEntry.continuationInstructions()

    val (_, dryRunBodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )

    var backwardUpdates = ControlFlowResolver.preResolveBackward(
        ctx,
        dryRunBodyCtx,
        false
    )
    if (backwardUpdates != null &&
        dryRunBodyCtx.upstream.getUsedEntries().size > ctx.upstream.getUsedEntries().size) {
        val extraNeeded = dryRunBodyCtx.upstream.getUsedEntries().size - ctx.upstream.getUsedEntries().size
        ctx.stackEnsureAtLeast(ctx.stackDepth() + extraNeeded)
        backwardUpdates = null
    }
    if (backwardUpdates == null) {
        val (_, dryRunBodyCtxCorrected) = parseContinuation(
            registry,
            ctx,
            bodyContinuation
        )
        backwardUpdates = ControlFlowResolver.preResolveBackward(
            ctx,
            dryRunBodyCtxCorrected,
            false
        ) ?: error("Stack depth mismatch")
    }

    val (_, bodyCtx) = parseContinuation(
        registry,
        ctx,
        bodyContinuation
    )

    ControlFlowResolver.postResolveBackward(
        ctx,
        bodyCtx,
        { newBodyCtx ->
            listOf(IRNode.RepeatLoop(
                IRNode.VariableUsage(countEntry, tracked = true),
                newBodyCtx.build()
            ))
        },
        backwardUpdates,
        false
    )
}

fun parseIfBlock(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,

    ifEntry: StackEntry?,
    ifReturn: Boolean,

    elseEntry: StackEntry?,
    elseReturn: Boolean,

    condEntry: StackEntry,
    ifnot: Boolean
) {
    val ifContinuation = ifEntry?.continuationInstructions()
    val elseContinuation = elseEntry?.continuationInstructions()
    val condNode = IRNode.CodeBlock(listOf(IRNode.VariableUsage(condEntry, tracked = true)), isExpression = true)

    val (ifNode, ifCtx) = ifContinuation?.let {
        parseContinuation(registry, ctx, it, isReturn = ifReturn)
    } ?: (null to null)
    val (elseNode, elseCtx) = elseContinuation?.let {
        parseContinuation(registry, ctx, it, isReturn = elseReturn)
    } ?: (null to null)
    val fallthroughCtx = ctx.fork()

    val finalized = buildList {
        if (ifCtx != null && !ifReturn) {
            add(ifCtx)
        }
        if (elseCtx != null && !elseReturn) {
            add(elseCtx)
        }
    }

    ControlFlowResolver.resolveForward(
        ctx,
        listOf(
            IRNode.IfElse(
                condNode,
                ifNode,
                elseNode,
                ifnot
            ),
        ),
        finalized,
        if (finalized.size < 2 && !(ifReturn && elseEntry != null)) {
            fallthroughCtx
        } else {
            null
        },
        listOfNotNull(ifCtx, elseCtx)
    )
}

fun parseIfjmpBlock(
    registry: ParserRegistry,
    ctx: IrBlockBuilder,
    ifEntry: StackEntry,
    condEntry: StackEntry,
    ifnot: Boolean,
    forceReturn: Boolean = false
) {
    val bodyIsReturn = ctx.isReturnContext || forceReturn ||
        (ctx.hasAltReturn && containsAltReturn(ifEntry.continuationInstructions()))

    if (ctx.isReturnContext) {
        parseIfBlock(registry, ctx, ifEntry, true, null, false, condEntry, ifnot)
    } else {
        val remaining = ctx.remainingInstructions!!
        val elseInstructions = remaining.toList()
        remaining.clear()

        val elseEntry = if (elseInstructions.isNotEmpty()) {
            newContinuation(name("continuation"), elseInstructions)
        } else null

        parseIfBlock(registry, ctx, ifEntry, bodyIsReturn, elseEntry, false, condEntry, ifnot)
    }
}

private fun containsAltReturn(instructions: List<TvmInst>): Boolean {
    return instructions.any {
        it is TvmContBasicRetaltInst ||
        it is TvmContConditionalIfretaltInst ||
        it is TvmContConditionalIfnotretaltInst
    }
}

fun registerContinuationParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmConstDataPushcontShortInst>(ParserLevel.MANUAL) { ctx, inst ->
            ctx.stackPush(newContinuation(name("continuation"), inst.c))
        }
        register<TvmConstDataPushrefcontInst>(ParserLevel.MANUAL) { ctx, inst ->
            ctx.stackPush(newContinuation(name("continuation"), inst.c))
        }
        register<TvmConstDataPushcontInst> (ParserLevel.MANUAL){ ctx, inst ->
            ctx.stackPush(newContinuation(name("continuation"), inst.c))
        }
        register<TvmContLoopsWhileInst>(ParserLevel.MANUAL) { ctx, inst ->
            val bodyEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)

            parseWhileBlock(registry, ctx, condEntry, bodyEntry)
        }
        register<TvmContLoopsUntilInst>(ParserLevel.MANUAL) { ctx, inst ->
            val bodyEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)

            parseUntilBlock(registry, ctx, bodyEntry)
        }
        register<TvmContLoopsRepeatInst>(ParserLevel.MANUAL) { ctx, inst ->
            val bodyEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val countEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseRepeatBlock(registry, ctx, countEntry, bodyEntry)
        }
        register<TvmContConditionalIfjmpInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, false)
        }
        register<TvmContConditionalIfjmprefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = newContinuation(name("continuation"), inst.c)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, false)
        }
        register<TvmContConditionalIfrefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), inst.c)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                null,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfelserefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val elseEntry = newContinuation(name("continuation"), inst.c)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                elseEntry,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfelseInst>(ParserLevel.MANUAL) { ctx, inst ->
            val elseEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                elseEntry,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                null,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfnotInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfBlock(
                registry, ctx, ifEntry, false, null, false, condEntry, true
            )
        }
        register<TvmContConditionalIfnotrefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), inst.c)

            parseIfBlock(
                registry, ctx, ifEntry, false, null, false, condEntry, true
            )
        }
        register<TvmContConditionalIfnotjmprefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = newContinuation(name("continuation"), inst.c)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, true)
        }
        register<TvmContConditionalIfnotretInst>(ParserLevel.MANUAL) { ctx, inst ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            val retEntry = newContinuation(name("continuation"), emptyList())

            parseIfjmpBlock(registry, ctx, retEntry, condEntry, true, forceReturn = true)
        }
        register<TvmContConditionalIfrefelserefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val elseEntry = newContinuation(name("continuation"), inst.c2)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), inst.c1)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                elseEntry,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfrefelseInst>(ParserLevel.MANUAL) { ctx, inst ->
            val elseEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), inst.c)

            parseIfBlock(
                registry,
                ctx,
                ifEntry,
                false,
                elseEntry,
                false,
                condEntry,
                false
            )
        }
        register<TvmContConditionalIfretInst> (ParserLevel.MANUAL){ ctx, inst ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), listOf())

            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, false)
        }
        register<TvmContConditionalIfnotjmpInst>(ParserLevel.MANUAL) { ctx, inst ->
            val ifEntry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)

            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, true)
        }
        register<TvmContBasicCallrefInst>(ParserLevel.MANUAL) { ctx, inst ->
            val syntheticId = ctx.callRefMapping?.get(inst.c)
            if (syntheticId != null) {
                val idx = (-syntheticId.toLong() - 1000).toInt()
                handleCallById(ctx, syntheticId, "callref_$idx")
            } else {
                parseCallContinuation(registry, ctx, inst.c)
            }
        }
        register<TvmContBasicExecuteInst>(ParserLevel.MANUAL) { ctx, inst ->
            val entry = ctx.stackPop(TvmStackEntryType.CONTINUATION.typename)
            val contInsts = entry.continuationInstructions()
            val syntheticId = ctx.callRefMapping?.get(contInsts)
            if (syntheticId != null) {
                val idx = (-syntheticId.toLong() - 1000).toInt()
                handleCallById(ctx, syntheticId, "callref_$idx")
            } else {
                parseCallContinuation(registry, ctx, contInsts)
            }
        }
        register<TvmContDictCalldictInst>(ParserLevel.MANUAL) { ctx, inst ->
            handleCalldict(ctx, inst.n)
        }
        register<TvmContDictCalldictLongInst>(ParserLevel.MANUAL) { ctx, inst ->
            handleCalldict(ctx, inst.n)
        }

        register(
            TvmContRegistersSaveInst::class.java,
            ParserLevel.MANUAL,
            parser = { ctx, inst ->
                if (inst.i == 2) {
                    val remaining = ctx.remainingInstructions
                    if (remaining != null && remaining.isNotEmpty() && remaining[0] is TvmContRegistersSamealtsaveInst) {
                        remaining.removeFirst()
                        ctx.hasAltReturn = true
                    }
                }
                true
            }
        )

        register<TvmContRegistersSamealtsaveInst>(ParserLevel.MANUAL) { _, _ -> }

        register<TvmContBasicRetaltInst>(ParserLevel.MANUAL) { ctx, _ ->
            ctx.remainingInstructions?.clear()
        }

        register<TvmContConditionalIfretaltInst>(ParserLevel.MANUAL) { ctx, _ ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), listOf())
            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, false, forceReturn = true)
        }

        register<TvmContConditionalIfnotretaltInst>(ParserLevel.MANUAL) { ctx, _ ->
            val condEntry = ctx.stackPop(TvmStackEntryType.INT.typename)
            val ifEntry = newContinuation(name("continuation"), listOf())
            parseIfjmpBlock(registry, ctx, ifEntry, condEntry, true, forceReturn = true)
        }
    }
}

private fun handleCalldict(ctx: IrBlockBuilder, methodId: Int) {
    val bigId = java.math.BigInteger.valueOf(methodId.toLong())
    val functionName = when (bigId) {
        java.math.BigInteger("-1") -> "recv_external"
        java.math.BigInteger("0") -> "recv_internal"
        else -> "fn_$methodId"
    }
    handleCallById(ctx, bigId, functionName)
}

private fun handleCallById(ctx: IrBlockBuilder, methodId: java.math.BigInteger, functionName: String) {
    val sig = ctx.callSignatures?.get(methodId)

    if (sig == null) {
        ctx.appendNode(IRNode.Comment("unresolved call $functionName"))
        return
    }

    val args = (0 until sig.nArgs).map { i ->
        val entry = ctx.stackPop()
        val argType = sig.argTypes.getOrNull(i)
        if (entry.type == TvmStackEntryType.UNKNOWN && argType != null && argType != TvmStackEntryType.UNKNOWN) {
            ctx.typeRefinements.putIfAbsent(entry, argType)
        }
        IRNode.VariableUsage(entry, tracked = true)
    }.reversed()

    val returnEntries = sig.returnTypes.mapIndexed { idx, type ->
        StackEntry.Simple(type, name("result"))
    }

    val call = IRNode.FunctionCall(functionName, args)

    if (returnEntries.isEmpty()) {
        ctx.appendNode(IRNode.VariableDeclaration(listOf(), call))
    } else {
        ctx.appendNode(IRNode.VariableDeclaration(returnEntries.reversed(), call))
        for (entry in returnEntries.reversed()) {
            ctx.stackPush(entry)
        }
    }
}
