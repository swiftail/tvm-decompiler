package io.swee.tvm.decompiler.internal

import org.ton.bytecode.MethodId

data class ContractHeader(val functions: Map<MethodId, FunctionData>)
