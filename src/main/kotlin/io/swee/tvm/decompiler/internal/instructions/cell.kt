package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.*

private fun registerCellParsingParsers(registry: ParserRegistry) {
}

fun registerCellParsers(registry: ParserRegistry) {
    registerCellParsingParsers(registry)
}
