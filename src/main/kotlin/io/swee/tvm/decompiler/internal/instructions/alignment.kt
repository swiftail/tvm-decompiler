package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.CodeBlockContext
import io.swee.tvm.decompiler.internal.ParserRegistry
import org.ton.bytecode.*

fun registerAlignmentParsers(registry: ParserRegistry) {
    listOf(
        TvmTupleNullswapifInst::class.java,
        TvmTupleNullswapifnotInst::class.java,
        TvmTupleNullswapif2Inst::class.java,
        TvmTupleNullswapifnot2Inst::class.java,
        TvmTupleNullrotrifInst::class.java,
        TvmTupleNullrotrifnotInst::class.java,
        TvmTupleNullrotrif2Inst::class.java,
        TvmTupleNullrotrifnot2Inst::class.java
    ).forEach { instClass ->
        registry.register(instClass) { ctx: CodeBlockContext, inst: TvmInst, ident: String ->
            error("stack alignment failed")
        }
    }
}
