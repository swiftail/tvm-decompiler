package io.swee.tvm.decompiler.internal

import org.ton.bytecode.MethodId

data class Header(val methods: Map<MethodId, MethodData>)
