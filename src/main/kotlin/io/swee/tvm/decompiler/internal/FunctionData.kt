package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmInst
import java.math.BigInteger

data class FunctionData(
    val id: BigInteger,
    val instructions: List<TvmInst>,
    val args: List<FunctionArg>
)
