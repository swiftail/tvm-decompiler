package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.ParserRegistry
import io.swee.tvm.decompiler.internal.popAny
import org.ton.bytecode.*
import kotlin.math.max

fun registerStackParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmStackBasicPopInst> { ctx, inst, ident ->
            ctx.ensureStackDepth(inst.i +1)
            ctx.stack[inst.i] = ctx.stack.first
            ctx.stack.removeFirst()
        }
        register<TvmStackComplexDrop2Inst> { ctx, _, _ ->
            ctx.popAny()
            ctx.popAny()
        }
        register<TvmStackComplexBlkdropInst> { ctx, inst, ident ->
            repeat(inst.i) {
                ctx.popAny()
            }
        }
        register<TvmStackComplexBlkdrop2Inst> { ctx, inst, ident ->
            ctx.ensureStackDepth(inst.j + 1 + inst.i + 1)
            repeat(inst.i) {
                ctx.stack.removeAt(inst.j + 1)
            }
        }
        register<TvmStackBasicPushInst> { ctx, inst, ident ->
            ctx.ensureStackDepth(inst.i + 1)
            ctx.push(ctx.stack[inst.i])
        }
        register<TvmStackBasicXchgIjInst> { ctx, inst, ident ->
            ctx.ensureStackDepth(max(inst.i, inst.j) + 2)
            val s0 = ctx.stack[inst.j + 1]
            val si = ctx.stack[inst.i + 1]
            ctx.stack[inst.i + 1] = s0
            ctx.stack[inst.j + 1] = si
        }
        register<TvmStackComplexDup2Inst> { ctx, inst, ident ->
            ctx.ensureStackDepth(2)
            ctx.push(ctx.stack[1])
            ctx.push(ctx.stack[1])
        }
        register<TvmStackBasicNopInst> { ctx, inst, ident ->
        }
    }
}
