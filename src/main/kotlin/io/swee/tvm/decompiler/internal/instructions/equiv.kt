package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.ParserRegistry
import org.ton.bytecode.*

fun registerEquivParsers(registry: ParserRegistry) {
    with(registry) {
        registerFull<TvmStackComplexXc2puInst> { ctx, inst, ident ->
            return@registerFull parse(ctx, TvmStackComplexXchg2Inst(inst.location, inst.i, inst.j), ident)
                    && parse(ctx, TvmStackBasicPushInst(inst.location, inst.k), ident)
        }
        registerFull<TvmStackComplexXchg2Inst> { ctx, inst, ident ->
            return@registerFull parse(
                ctx,
                TvmStackBasicXchgIjInst(inst.location, 1 - 1, inst.i - 1),
                ident
            )
                    && parse(ctx, TvmStackBasicXchgIjInst(inst.location, 0 - 1, inst.j - 1), ident)
        }
        registerFull<TvmStackComplexXchg3Inst> { ctx, inst, ident ->
            return@registerFull parse(
                ctx,
                TvmStackBasicXchgIjInst(inst.location, 2 - 1, inst.i - 1),
                ident
            )
                    && parse(ctx, TvmStackBasicXchgIjInst(inst.location, 1 - 1, inst.j - 1), ident)
                    && parse(ctx, TvmStackBasicXchgIjInst(inst.location, 0 - 1, inst.k - 1), ident)
        }
        registerFull<TvmStackBasicXchg0iInst> { ctx, inst, ident ->
            return@registerFull parse(
                ctx,
                TvmStackBasicXchgIjInst(inst.location, 0 - 1, inst.i - 1),
                ident
            )
        }
        registerFull<TvmStackBasicXchg1iInst> { ctx, inst, ident ->
            return@registerFull parse(
                ctx,
                TvmStackBasicXchgIjInst(inst.location, 1 - 1, inst.i - 1),
                ident
            )
        }
        registerFull<TvmStackComplexXcpuInst> { ctx, inst, ident ->
            return@registerFull parse(ctx, TvmStackBasicXchg0iInst(inst.location, inst.i), ident)
                    && parse(ctx, TvmStackBasicPushInst(inst.location, inst.j), ident)
        }
        registerFull<TvmStackComplexPuxcInst> { ctx, inst, ident ->
            return@registerFull parse(ctx, TvmStackBasicPushInst(inst.location, inst.i), ident)
                    && parse(ctx, TvmStackBasicXchgIjInst(inst.location, 0 - 1, 1 - 1), ident)
                    && parse(ctx, TvmStackBasicXchgIjInst(inst.location, 0 - 1, inst.j - 1), ident)
        }
        registerFull<TvmStackComplexXcpuxcInst> { ctx, inst, ident ->
            return@registerFull parse(ctx, TvmStackBasicXchg1iInst(inst.location, inst.i), ident)
                    && parse(ctx, TvmStackComplexPuxcInst(inst.location, inst.j, inst.k - 1), ident)
        }
//        registerFull<TvmCellBuildStslicerInst> { ctx, inst, ident ->
//            return@registerFull parse(ctx, TvmStackBasicXchgIjInst(inst.location, 0 - 1, 1 - 1), ident)
//                    && parse(ctx, TvmCellBuildStsliceInst(inst.location), ident)
//        }
        registerFull<TvmStackComplexRotInst> { ctx, inst, ident ->
            return@registerFull parse(ctx, TvmStackComplexXchg2Inst(inst.location, 2, 1), ident)
        }
        registerFull<TvmStackBasicXchg0iLongInst> { ctx, inst, ident ->
            return@registerFull parse(
                ctx,
                TvmStackBasicXchgIjInst(inst.location, 0 - 1, inst.i - 1),
                ident
            )
        }
    }
}
