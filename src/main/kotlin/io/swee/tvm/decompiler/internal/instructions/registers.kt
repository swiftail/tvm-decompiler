package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmContRegistersPopctrInst
import org.ton.bytecode.TvmContRegistersPushctrInst

fun registerRegistersParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmContRegistersPushctrInst> { ctx: CodeBlockContext, inst: TvmContRegistersPushctrInst, ident: String ->
            val entryResult = StackEntry.Simple(TvmStackEntryType.CELL, StackEntryName.Const("contract_data"))
            ctx.push(entryResult)
            ctx.append(AstElement.VariableDeclaration(listOf(entryResult), AstElement.FunctionCall("get_data", listOf())))
        }
        registerFull<TvmContRegistersPopctrInst> { ctx: CodeBlockContext, inst: TvmContRegistersPopctrInst, ident: String ->
            val entry = ctx.popAny()
            when (inst.i) {
                4 -> {
                    ctx.append(
                        AstElement.FunctionCall("set_data", listOf(
                            AstElement.VariableUsage(entry, true)
                        )))
                }
                else -> {
                    return@registerFull false
                }
            }
            return@registerFull true
        }
    }
}
