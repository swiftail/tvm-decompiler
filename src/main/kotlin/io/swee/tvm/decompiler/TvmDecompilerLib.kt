package io.swee.tvm.decompiler

import io.swee.tvm.decompiler.api.TvmDecompilerFacade
import io.swee.tvm.decompiler.cli.TvmDecompilerFacadeImpl

object TvmDecompilerLib {
    fun facade(
        stdlibContent: String? = null
    ): TvmDecompilerFacade {
        return TvmDecompilerFacadeImpl
    }
}
