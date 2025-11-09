package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmAppConfigGetparamInst

fun registerAppParsers(registry: ParserRegistry) {
    with(registry) {
        registry.registerFull<TvmAppConfigGetparamInst> { ctx: CodeBlockContext, inst: TvmAppConfigGetparamInst, ident: String ->
            when (inst.i) {
                3 -> {
                    val entry = newInt(name("now"))
                    ctx.push(entry)
                    ctx
                        .append(AstElement.VariableDeclaration(listOf(entry), AstElement.FunctionCall("now", listOf())))
                        .append(AstElement.Raw(";"))
                }

                7 -> {
                    val entry = newTuple(
                        name("balance"), listOf(
                            TvmStackEntryType.INT,
                            TvmStackEntryType.CELL
                        )
                    )
                    ctx.push(entry)
                    ctx
                        .append(
                            AstElement.VariableDeclaration(
                                listOf(entry),
                                AstElement.FunctionCall("get_balance", listOf())
                            )
                        )
                        .append(AstElement.Raw(";"))
                }

                11 -> {
                    val entry = newUnknown(name("in_msg_value"))
                    ctx.push(entry)
                    ctx
                        .append(
                            AstElement.VariableDeclaration(
                                listOf(entry),
                                AstElement.FunctionCall("get_incoming_value", listOf())
                            )
                        )
                        .append(AstElement.Raw(";"))
                }

                12 -> {
                    val entry = newInt(name("storage_fees"))
                    ctx.push(entry)
                    ctx
                        .append(
                            AstElement.VariableDeclaration(
                                listOf(entry),
                                AstElement.FunctionCall("get_storage_fees", listOf())
                            )
                        )
                        .append(AstElement.Raw(";"))
                }

                else -> return@registerFull false
            }
            true
        }
    }
}
