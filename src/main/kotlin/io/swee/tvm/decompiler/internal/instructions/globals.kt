package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmAppGlobalGetglobInst
import org.ton.bytecode.TvmAppGlobalSetglobInst

fun registerGlobalsParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmAppGlobalSetglobInst> { ctx, inst, ident ->
            val entry = ctx.popAny()
            ctx.append(AstElement.Raw("__global_${inst.k} = ")).append(AstElement.VariableUsage(entry, true))
        }
        register<TvmAppGlobalGetglobInst> { ctx, inst, ident ->
            val entry = newUnknown(StackEntryName.Const("global_${inst.k}"))
            ctx.push(entry)
            ctx.append(AstElement.VariableDeclaration(listOf(entry), AstElement.Raw("__global_${inst.k}")))
        }
    }
}
