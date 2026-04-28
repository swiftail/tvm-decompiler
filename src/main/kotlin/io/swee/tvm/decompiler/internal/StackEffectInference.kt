package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.ir.IRNode
import io.swee.tvm.decompiler.internal.ir.IRNodeVisitor
import org.ton.bytecode.*
import java.math.BigInteger

data class FunctionSignature(
    val nArgs: Int,
    val argTypes: List<TvmStackEntryType>,
    val nReturns: Int,
    val returnTypes: List<TvmStackEntryType>
)

data class CallrefExtractionResult(
    val augmentedMethods: Map<BigInteger, List<TvmInst>>,
    val callrefMapping: Map<List<TvmInst>, BigInteger>
)

fun extractCallrefBodies(methods: Map<BigInteger, List<TvmInst>>): CallrefExtractionResult {
    val callrefMapping = HashMap<List<TvmInst>, BigInteger>()
    var nextId = -1000L

    fun scan(instList: List<TvmInst>) {
        for (inst in instList) {
            if (inst is TvmContBasicCallrefInst) {
                if (inst.c !in callrefMapping) {
                    callrefMapping[inst.c] = BigInteger.valueOf(nextId--)
                }
            }
            if (inst is TvmContOperand1Inst) {
                scan(inst.c)
            }
            if (inst is TvmContOperand2Inst) {
                scan(inst.c1)
                scan(inst.c2)
            }
        }
    }

    for ((_, instList) in methods) {
        scan(instList)
    }

    val augmentedMethods = methods.toMutableMap()
    for ((instList, id) in callrefMapping) {
        augmentedMethods[id] = instList
    }

    return CallrefExtractionResult(augmentedMethods, callrefMapping)
}

fun inferSignatures(
    methods: Map<BigInteger, List<TvmInst>>,
    registry: ParserRegistry,
    knownSignatures: Map<BigInteger, FunctionSignature>,
    callrefMapping: Map<List<TvmInst>, BigInteger> = emptyMap()
): Map<BigInteger, FunctionSignature> {
    val result = knownSignatures.toMutableMap()

    val callGraph = buildCallGraph(methods, callrefMapping)

    val sccs = tarjanSCC(methods.keys, callGraph)

    for (scc in sccs) {
        if (scc.size == 1) {
            val id = scc.single()
            if (id in result) continue
            val instList = methods[id] ?: continue
            val selfRecursive = callGraph[id]?.contains(id) == true
            if (!selfRecursive) {
                val sig = simulateFunction(instList, registry, result, callrefMapping)
                if (sig != null) result[id] = sig
            } else {
                fixedPointInfer(scc, methods, registry, result, callrefMapping)
            }
        } else {
            fixedPointInfer(scc, methods, registry, result, callrefMapping)
        }
    }

    return result
}

private fun fixedPointInfer(
    scc: List<BigInteger>,
    methods: Map<BigInteger, List<TvmInst>>,
    registry: ParserRegistry,
    result: MutableMap<BigInteger, FunctionSignature>,
    callrefMapping: Map<List<TvmInst>, BigInteger>
) {
    for (id in scc) {
        if (id !in result) {
            result[id] = FunctionSignature(0, emptyList(), 0, emptyList())
        }
    }

    var changed = true
    var iterations = 0
    val maxIterations = 20
    while (changed && iterations < maxIterations) {
        changed = false
        iterations++
        for (id in scc) {
            val instList = methods[id] ?: continue
            val sig = simulateFunction(instList, registry, result, callrefMapping) ?: continue
            val old = result[id]
            if (old != sig) {
                result[id] = sig
                changed = true
            }
        }
    }
}

private fun simulateFunction(
    instList: List<TvmInst>,
    registry: ParserRegistry,
    signatures: Map<BigInteger, FunctionSignature>,
    callrefMapping: Map<List<TvmInst>, BigInteger>
): FunctionSignature? {
    val upstream = DiscoveryUpstreamStack()
    val builder = IrBlockBuilder(upstream)
    builder.callSignatures = signatures
    builder.callRefMapping = callrefMapping

    return try {
        val codeBlock = TvmDecompilerImpl.parseCodeBlock(registry, builder, instList, false)
        val usedEntries = upstream.getUsedEntries()

        val earlyReturns = collectEarlyReturns(codeBlock)

        val nReturns: Int
        val returnTypes: List<TvmStackEntryType>
        if (earlyReturns.isNotEmpty()) {
            nReturns = earlyReturns.first().variables.size
            returnTypes = earlyReturns.first().variables.map { it.entry.type }
        } else {
            nReturns = builder.stackDepth()
            returnTypes = builder.stackCopy().map { builder.typeRefinements[it] ?: it.type }
        }

        FunctionSignature(
            nArgs = usedEntries.size,
            argTypes = usedEntries.map { builder.typeRefinements[it] ?: it.type },
            nReturns = nReturns,
            returnTypes = returnTypes
        )
    } catch (_: Throwable) {
        null
    }
}

private fun collectEarlyReturns(node: IRNode): List<IRNode.FunctionReturnStatement> {
    val result = mutableListOf<IRNode.FunctionReturnStatement>()
    node.accept(object : IRNodeVisitor {
        override fun visit(node: IRNode.FunctionReturnStatement) {
            result.add(node)
        }
    })
    return result
}

private fun buildCallGraph(
    methods: Map<BigInteger, List<TvmInst>>,
    callrefMapping: Map<List<TvmInst>, BigInteger>
): Map<BigInteger, Set<BigInteger>> {
    val graph = mutableMapOf<BigInteger, MutableSet<BigInteger>>()
    for ((id, instList) in methods) {
        val callees = graph.getOrPut(id) { mutableSetOf() }
        collectCallees(instList, callees, callrefMapping)
    }
    return graph
}

private fun collectCallees(
    instList: List<TvmInst>,
    callees: MutableSet<BigInteger>,
    callrefMapping: Map<List<TvmInst>, BigInteger>
) {
    for (inst in instList) {
        when (inst) {
            is TvmContDictCalldictInst -> callees.add(BigInteger.valueOf(inst.n.toLong()))
            is TvmContDictCalldictLongInst -> callees.add(BigInteger.valueOf(inst.n.toLong()))
            is TvmContBasicCallrefInst -> {
                val syntheticId = callrefMapping[inst.c]
                if (syntheticId != null) callees.add(syntheticId)
            }
            else -> {}
        }
        if (inst is TvmContOperand1Inst) {
            collectCallees(inst.c, callees, callrefMapping)
        }
        if (inst is TvmContOperand2Inst) {
            collectCallees(inst.c1, callees, callrefMapping)
            collectCallees(inst.c2, callees, callrefMapping)
        }
    }
}

private fun tarjanSCC(
    nodes: Set<BigInteger>,
    graph: Map<BigInteger, Set<BigInteger>>
): List<List<BigInteger>> {
    var index = 0
    val nodeIndex = mutableMapOf<BigInteger, Int>()
    val nodeLowlink = mutableMapOf<BigInteger, Int>()
    val onStack = mutableSetOf<BigInteger>()
    val stack = ArrayDeque<BigInteger>()
    val result = mutableListOf<List<BigInteger>>()

    fun strongConnect(v: BigInteger) {
        nodeIndex[v] = index
        nodeLowlink[v] = index
        index++
        stack.addLast(v)
        onStack.add(v)

        for (w in graph[v] ?: emptySet()) {
            if (w !in nodes) continue
            if (w !in nodeIndex) {
                strongConnect(w)
                nodeLowlink[v] = minOf(nodeLowlink[v]!!, nodeLowlink[w]!!)
            } else if (w in onStack) {
                nodeLowlink[v] = minOf(nodeLowlink[v]!!, nodeIndex[w]!!)
            }
        }

        if (nodeLowlink[v] == nodeIndex[v]) {
            val scc = mutableListOf<BigInteger>()
            do {
                val w = stack.removeLast()
                onStack.remove(w)
                scc.add(w)
            } while (w != v)
            result.add(scc)
        }
    }

    for (v in nodes) {
        if (v !in nodeIndex) {
            strongConnect(v)
        }
    }

    return result
}
