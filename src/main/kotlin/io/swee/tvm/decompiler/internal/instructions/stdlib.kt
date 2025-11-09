package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import io.swee.tvm.decompiler.internal.instructions.Cp0InstructionRegistry.TvmCp0InstValueFlowOutputsEntry
import org.ton.bytecode.*

class StdlibRegistry(
    private val cp0InstructionRegistry: Cp0InstructionRegistry,
    private val stdlibContent: String,
    private val builtinContent: String
) {
    private companion object {
        private val ASM_FN_REGEX =
            "(?:forall\\s+(?<forallArgs>\\w+(?:\\s*,\\s*\\w+)*)\\s+->\\s+)?(?<returnType>.+?)\\s+(?<fnName>[^\\s\\(\\)]+)\\s*\\((?<fnArgs>(?:\\w+\\s+\\w+)(?:\\s*,\\s*\\w+\\s+\\w+)*)?\\)\\s*(?:\\w+)*\\s*asm(?:\\s*\\((?<asmDescription>.+)\\)\\s*)?\\s*(?<asmExpression>\"[^\"]+\"(?:\\s*\"[^\"]+\")*)\\s*;".toRegex()
        private val ASM_EXP_REGEX = "\"[^\"]+\"".toRegex()
    }

    private fun registerAsmFunction(
        registry: ParserRegistry,
        instData: Cp0InstructionRegistry.InstructionData,
        fnName: String
    ) {
        try {
            registry.register(instData.instClass) { ctx, inst: Any?, _: String ->
                val virtualArgs = instData.instDescriptionRaw.bytecode.operands.map {
                    val virtualEntry = StackEntry.Simple(
                        TvmStackEntryType.fromTvmBytecodeOperandDescription(it),
                        StackEntryName.Const("virtual")
                    )
                    ctx.pushVirtual(virtualEntry)

                    val literalValueField = inst!!.javaClass.declaredFields.find { field -> field.name == it.name }!!
                    literalValueField.isAccessible = true
                    var literalValue = literalValueField.get(inst)

//                    for (displayHint in it.displayHints) {
//                        when (displayHint) {
//                            is Cp0InstructionRegistry.TvmCp0InstBytecodeOperandDisplayHint.Add -> {
//                                literalValue = literalValue.toString().toLong() + displayHint.value
//                            }
//                            else -> {}
//                        }
//                    }

                    ctx.append(AstElement.VariableDeclaration(listOf(virtualEntry), AstElement.Literal(literalValue)))
                    ctx.popAny()
                }

                val args = (instData.instDescriptionRaw.valueFlow.inputs.stack ?: emptyList()).map {
                    ctx.popAny() // TODO
                }

                val allArgs = virtualArgs + args

                val functionCall = AstElement.FunctionCall(
                    fnName,
                    allArgs.map {
                        AstElement.VariableUsage(
                            it,
                            true
                        )
                    }.reversed()
                )

                val newStackEntries = (instData.instDescriptionRaw.valueFlow.outputs.stack ?: emptyList()).map {
                    it as TvmCp0InstValueFlowOutputsEntry.Simple
                    StackEntry.Simple(
                        TvmStackEntryType.fromTvmStackEntryDescription(it),
                        StackEntryName.Const(it.name ?: "var")
                    )
                }
                for (newStackEntry in newStackEntries) {
                    ctx.push(newStackEntry)
                }

                when (newStackEntries.size) {
                    0 -> {
                        ctx.append(functionCall)
                    }

                    else -> {
                        ctx.append(
                            AstElement.VariableDeclaration(
                                newStackEntries.reversed(),
                                functionCall
                            )
                        )
                    }

                }

                true
            }
        } catch (e: Throwable) {
            if (e.message?.contains("Inst redefinition") == true) {
                // TODO
                return
            }
            throw e
        }
    }

    private fun parseStdlibLine(registry: ParserRegistry, line: String) {
        val match = ASM_FN_REGEX.matchEntire(line) ?: return
        val groupForallArgs = match.groups["forallArgs"]?.value
        val groupReturnType = match.groups["returnType"]?.value ?: return
        val groupFnName = match.groups["fnName"]?.value ?: return
        val groupFnArgs = match.groups["fnArgs"]?.value
        val groupAsmExpression = match.groups["asmExpression"]?.value ?: return

        if (groupFnName.contains("~") && groupFnName != "~_") {
            // TODO tilda function
            return
        }

        val asmOpcodes = ASM_EXP_REGEX.findAll(groupAsmExpression).map { it.value.replace("\"", "") }.toList()

        val firstOpcode = asmOpcodes.first()

        if (asmOpcodes.size > 1) {
//            println("skipping chain: ${asmOpcodes}")
            // TODO handle chains
            return
        }
        if (firstOpcode.split("\\s+".toRegex()).size > 1) {
//            println("skipping opcode with arg: ${firstOpcode}")
            return
        }

        val instDesc = cp0InstructionRegistry.getByOpcode(firstOpcode) ?: return

        registerAsmFunction(
            registry,
            instDesc,
            groupFnName
        )
    }

    fun registerStdlibInstructions(
        registry: ParserRegistry,
    ) {
        builtinContent
            .lineSequence()
            .map { it.trim() }
            .forEach { parseStdlibLine(registry, it) }
        stdlibContent
            .lineSequence()
            .map { it.trim() }
            .forEach { parseStdlibLine(registry, it) }
    }

    private fun processConditionalInstruction(
        registry: ParserRegistry,
        instData: Cp0InstructionRegistry.InstructionData
    ) {
        println("checking ${instData.instDescriptionRaw.mnemonic} ${instData.instClass}")

        val outputStack = (instData.instDescriptionRaw.valueFlow.outputs.stack ?: emptyList()).toMutableList()
        val status = outputStack.removeLastOrNull()
        if (status != null && status !is TvmCp0InstValueFlowOutputsEntry.Conditional) {
            val conditional = outputStack.removeLastOrNull()
            if (conditional != null && conditional is TvmCp0InstValueFlowOutputsEntry.Conditional) {
                val status0 = conditional.match.find { it.value == 0L }?.stack
                val statusn1 = conditional.match.find { it.value == -1L }?.stack


                if (status0 != null && statusn1 != null) {
                    if (status0.size == statusn1.size && status0.indices.all {
                        status0[it].contentEquals(statusn1[it])
                        }) {
                        registry.register (instData.instClass) { ctx: CodeBlockContext, inst: Any, ident: String ->
                            TODO()
                        }
                        return
                    }

                    if (statusn1.size == status0.size + 1 && status0.indices.all {
                        status0[it].contentEquals(statusn1[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifnotInst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }
                    if (statusn1.size == status0.size + 2 && status0.indices.all {
                        status0[it].contentEquals(statusn1[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifnot2Inst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return

                    }
                    if (statusn1.size == status0.size + 3 && status0.indices.all {
                        status0[it].contentEquals(statusn1[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifnot2Inst::class.java,TvmTupleNullswapifnotInst::class.java ),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifnotInst::class.java,TvmTupleNullswapifnot2Inst::class.java ),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }
                    if (status0.size == statusn1.size + 1 && statusn1.indices.all {
                        statusn1[it].contentEquals(status0[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifInst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }
                    if (status0.size == statusn1.size + 2 && statusn1.indices.all {
                        statusn1[it].contentEquals(status0[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapif2Inst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }
                    if (status0.size == statusn1.size + 3 && statusn1.indices.all {
                        statusn1[it].contentEquals(status0[it])
                        }) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapif2Inst::class.java, TvmTupleNullswapifInst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullswapifInst::class.java, TvmTupleNullswapif2Inst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }

                    if (
                        statusn1.size == status0.size + 1 && status0.indices.all {
                            status0[it].contentEquals(statusn1[it + 1])
                        }
                    ) {
                        registry.registerChain(
                            listOf(instData.instClass, TvmTupleNullrotrifnotInst::class.java),
                            { ctx: CodeBlockContext, inst: List<*>, ident: String ->
                                TODO()
                            }
                        )
                        return
                    }
                }
            }
        }
        println("Unknown conditional instruction: ${instData.instDescriptionRaw.mnemonic}")
    }

    fun registerSimpleAsmInstructions(
        registry: ParserRegistry,
    ) {
        val processed = mutableSetOf<Class<out TvmInst>>()

        for ((_, instData) in cp0InstructionRegistry.instructions) {
            if (processed.contains(instData.instClass)) {
                continue
            } else {
                processed += instData.instClass
            }

            if (registry.getParser(instData.instClass) == null) {
                val instDescRaw = instData.instDescriptionRaw
                if (instDescRaw.controlFlow.branches.isNotEmpty()) {
                    println("Skipping ${instDescRaw.mnemonic}: control flow branches")
                    continue
                }
                if (instDescRaw.doc?.category == "stack" || instDescRaw.doc?.category == "stack_complex") {
                    println("Skipping ${instDescRaw.mnemonic}: stack")
                    continue
                }

                if ((instData.instDescriptionRaw.valueFlow.outputs.stack ?: emptyList()).any { it is TvmCp0InstValueFlowOutputsEntry.Conditional }) {
                    processConditionalInstruction(registry, instData)
                    continue
                }

                // no parser
                registerAsmFunction(
                    registry,
                    instData,
                    "asm_${instData.instDescriptionRaw.mnemonic}"
                )
            }
        }
    }
}
