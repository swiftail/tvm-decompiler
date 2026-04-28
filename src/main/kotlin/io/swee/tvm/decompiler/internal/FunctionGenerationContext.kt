package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.ir.IRNode
import io.swee.tvm.decompiler.internal.ir.IRNode.VariableDeclaration
import io.swee.tvm.decompiler.internal.ir.IRNodeTransformer
import io.swee.tvm.decompiler.internal.ir.IRNodeVisitor
import io.swee.tvm.decompiler.internal.ir.StackEntryVisitor
import org.ton.bytecode.TvmCell

data class RootGenerationContext(
    val functions: Map<IRNode.Function, FunctionGenerationContext>,
    val node: IRNode.Root,
    val globalVariableTypes: Map<Int, TvmStackEntryType> = emptyMap()
)

data class FunctionGenerationContext(
    val stackEntryNameResolver: (entry: StackEntryName) -> String,
    val stackEntryUsage: Map<StackEntry, Int>,
    val stackEntrySources: Map<StackEntry, List<IRNode>>,
    val stackEntryProducts: Map<IRNode, VariableDeclaration>,
    val expressionDeclarations: Set<VariableDeclaration>,
)

fun analyzeFunction(
    function: IRNode.Function
): FunctionGenerationContext {
    val stackEntryUsage: MutableMap<StackEntry, Int> = mutableMapOf()
    val stackEntrySources: MutableMap<StackEntry, MutableList<IRNode>> = mutableMapOf()
    val stackEntryProducts: MutableMap<IRNode, VariableDeclaration> = mutableMapOf()
    val stackEntryNameCache: MutableMap<StackEntryName, String> = mutableMapOf()
    val usedVariableNames: MutableMap<String, Int> = mutableMapOf()

    function.accept(object : IRNodeVisitor {
        override fun visit(node: VariableDeclaration) {
            node.entries.forEach { entry ->
                if (!stackEntrySources.containsKey(entry)) {
                    stackEntrySources[entry] = mutableListOf()
                }
                stackEntrySources[entry]!!.add(node.value)
            }
            if (stackEntryProducts.containsKey(node.value)) {
                error("Duplicate product for IR node: ${node.value} already maps to ${stackEntryProducts[node.value]}")
            }
            stackEntryProducts[node.value] = node
        }
    })
    function.accept(object : StackEntryVisitor {
        override fun visitEntry(node: IRNode, entry: StackEntry) {
            stackEntryUsage[entry] = stackEntryUsage.getOrDefault(entry, 0) + 1
        }
    })

    fun stackEntryNameResolver(entry: StackEntryName): String {
        if (entry in stackEntryNameCache) {
            return stackEntryNameCache[entry]!!
        }

        val notCountedName = when (entry) {
            is StackEntryName.Const -> entry.value
            is StackEntryName.Parent -> stackEntryNameResolver(entry.parent) + "_" + entry.suffix
        }
        val usedTimes = usedVariableNames.getOrDefault(notCountedName, 0)
        val finalName = "${notCountedName}_${usedTimes.toBigInteger().toString(16).padStart(2, '0')}"

        usedVariableNames[notCountedName] = usedTimes + 1
        stackEntryNameCache[entry] = finalName
        return finalName
    }

    return FunctionGenerationContext(
        ::stackEntryNameResolver,
        stackEntryUsage,
        stackEntrySources,
        stackEntryProducts,
        setOf(),
    )
}

fun analyze(root: IRNode.Root): RootGenerationContext {
    val sliceDeclarations = mutableMapOf<TvmCell, Int>()

    val transformer = object : IRNodeTransformer {
        override fun transformSliceLiteral(node: IRNode.SliceLiteral): IRNode {
            val sliceIndex = sliceDeclarations.merge(
                node.slice,
                sliceDeclarations.size,
                { a, b -> a }
            )
            return IRNode.FunctionCall(
                "__slice_$sliceIndex",
                listOf()
            )
        }
    }
    val transformedRoot = transformer.transformRoot(root)

    val newAsmFunctions = sliceDeclarations.entries.map { entry ->
        IRNode.AsmFunction(
            "__slice_${entry.value}",
            listOf(),
            listOf(StackEntry.Simple(TvmStackEntryType.SLICE, StackEntryName.Const("slice"))),
            "\"${Literals.cellLiteral(entry.key)} PUSHSLICE\""
        )
    }

    val asmFunctionUsages = mutableMapOf<String, IRNode.AsmFunction>()
    val argNames = listOf("a", "b", "c", "d", "e", "f", "g", "h")
    transformedRoot.accept(object : IRNodeVisitor {
        override fun visit(node: VariableDeclaration) {
            val call = node.value as? IRNode.FunctionCall ?: return
            if (!call.name.startsWith("asm_") || call.name in asmFunctionUsages) return
            val body = call.asmBody ?: "\"${call.name.removePrefix("asm_")}\""
            asmFunctionUsages[call.name] = IRNode.AsmFunction(
                call.name,
                call.args.mapIndexed { i, arg ->
                    val type = (arg as IRNode.VariableUsage).entry.type
                    StackEntry.Simple(type, StackEntryName.Const(argNames.getOrElse(i) { "x$i" }))
                },
                node.entries.map { StackEntry.Simple(it.type, StackEntryName.Const("r")) },
                body
            )
        }
        override fun visit(node: IRNode.FunctionCall) {
            if (!node.name.startsWith("asm_") || node.name in asmFunctionUsages) return
            val body = node.asmBody ?: "\"${node.name.removePrefix("asm_")}\""
            asmFunctionUsages[node.name] = IRNode.AsmFunction(
                node.name,
                node.args.mapIndexed { i, arg ->
                    val type = (arg as IRNode.VariableUsage).entry.type
                    StackEntry.Simple(type, StackEntryName.Const(argNames.getOrElse(i) { "x$i" }))
                },
                listOf(),
                body
            )
        }
    })

    val newRoot = IRNode.Root(
        transformedRoot.asmFunctions + newAsmFunctions + asmFunctionUsages.values,
        transformedRoot.functions
    )

    val functionContexts = newRoot.functions.associateWith { analyzeFunction(it) }

    val globalTypes = mutableMapOf<Int, TvmStackEntryType>()
    newRoot.accept(object : IRNodeVisitor {
        override fun visit(node: IRNode.GlobalWrite) {
            val writeType = (node.value as? IRNode.VariableUsage)?.entry?.type ?: TvmStackEntryType.UNKNOWN
            if (writeType != TvmStackEntryType.UNKNOWN) {
                globalTypes.putIfAbsent(node.number, writeType)
            } else {
                globalTypes.putIfAbsent(node.number, TvmStackEntryType.UNKNOWN)
            }
        }
        override fun visit(node: IRNode.GlobalRead) {
            globalTypes.putIfAbsent(node.number, TvmStackEntryType.UNKNOWN)
        }
    })

    return RootGenerationContext(
        functionContexts,
        newRoot,
        globalTypes
    )
}
