package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.TvmAppGlobalGetglobInst
import org.ton.bytecode.TvmAppGlobalSetglobInst

fun registerGlobalsParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmAppGlobalSetglobInst>(ParserLevel.MANUAL) { ctx, inst ->
            val entry = ctx.stackPop()

            ctx.appendNode(IRNode.GlobalWrite(inst.k, IRNode.VariableUsage(entry, true)))
        }
        register<TvmAppGlobalGetglobInst>(ParserLevel.MANUAL) { ctx, inst ->
            val entry = newUnknown(StackEntryName.Const("global_${inst.k}"))
            ctx.stackPush(entry)

            ctx.appendNode(IRNode.VariableDeclaration(listOf(entry), IRNode.GlobalRead(inst.k)))
        }
    }
}
