package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmInst

class StdlibRegistry(
    private val cp0InstructionRegistry: Cp0InstructionRegistry,
    private val stdlibContent: String,
    private val builtinContent: String
) {
    private companion object {
        private val ASM_FN_REGEX = "(?:forall\\s+(?<forallArgs>\\w+(?:\\s*,\\s*\\w+)*)\\s+->\\s+)?(?<returnType>.+?)\\s+(?<fnName>[^\\s\\(\\)]+)\\s*\\((?<fnArgs>(?:\\w+\\s+\\w+)(?:\\s*,\\s*\\w+\\s+\\w+)*)?\\)\\s*(?:\\w+)*\\s*asm(?:\\s*\\((?<asmDescription>.+)\\)\\s*)?\\s*(?<asmExpression>\"[^\"]+\"(?:\\s*\"[^\"]+\")*)\\s*;".toRegex()
    }

    private fun registerAsmFunction(
        registry: ParserRegistry,
        instClass: Class<out TvmInst>,
        instDesc: Cp0InstructionRegistry.TvmInstSimple,
        fnName: String
    ) {
        try {
            registry.register(instClass) { ctx, _: Any?, _: String ->
                val args = instDesc.inputStack.map {
                    ctx.popAny()
                }

                val functionCall = AstElement.FunctionCall(
                    fnName,
                    args.map {
                        AstElement.VariableUsage(
                            it,
                            true
                        )
                    }
                )

                val newStackEntries = instDesc.outputStack.map {
                    StackEntry.Simple(
                        TvmStackEntryType.fromTvmDescription(it),
                        StackEntryName.Const(it.name)
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
                        ctx.append(AstElement.VariableDeclaration(
                            newStackEntries,
                            functionCall
                        ))
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
        if (line.contains("STSLICER")) {
            println("boobs")
        }
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

        val asmOpcodes = groupAsmExpression
            .trim()
            .replace("\"", "")
            .uppercase()
            .split("\\s+".toRegex())

        val firstOpcode = asmOpcodes.first()

        // TODO handle chains
        val (_, instClass, instDesc) = cp0InstructionRegistry.getByOpcode(firstOpcode) ?: return

        registerAsmFunction(
            registry,
            instClass,
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

    fun registerSimpleAsmInstructions(
        registry: ParserRegistry,
    ) {
        for ((opcode, instData) in cp0InstructionRegistry.instructions) {
            if (registry.getParser(instData.instClass) == null) {
                val instDescRaw = instData.instDescriptionRaw
                if (!instDescRaw.control_flow.nobranch) {
                    continue
                }
                if (instDescRaw.doc?.category == "stack" || instDescRaw.doc?.category == "stack_complex") {
                    continue
                }

                // no parser
                registerAsmFunction(
                    registry,
                    instData.instClass,
                    instData.instDescription,
                    "asm_${opcode}"
                )
            }
        }
    }
}
