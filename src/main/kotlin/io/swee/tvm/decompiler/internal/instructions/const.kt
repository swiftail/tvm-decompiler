package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.TvmStackEntryType.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.*
import java.math.BigInteger

private fun <T : TvmInst> pushConst(type: TvmStackEntryType, value: (inst: T) -> String): InstParserShort<T> {
    return { ctx, inst, ident ->
        val res = StackEntry.Simple(type, name("val"))
        ctx.push(res)

        ctx
            .append(AstElement.VariableDeclaration(listOf(res), AstElement.Literal(value(inst))))
    }
}

fun registerConstParsers(registry: ParserRegistry) {
    with(registry) {
        register(pushConst<TvmConstIntPushpow2Inst>(INT) {
            BigInteger.TWO.pow(it.x + 1).toString()
        })
        register(pushConst<TvmConstIntPushint4Inst>(INT) { it.i.toString() })
        register(pushConst<TvmConstIntPushint8Inst>(INT) { it.x.toString() })
        register(pushConst<TvmConstIntPushint16Inst>(INT) { it.x.toString() })
        register(pushConst<TvmConstIntPushintLongInst>(INT) { it.x })
        register(pushConst<TvmTupleNullInst>(UNKNOWN) { "null()"})
    }
}
