package io.swee.tvm.decompiler.cli

import io.swee.tvm.decompiler.api.TvmDecompilerFacade
import io.swee.tvm.decompiler.api.TvmDecompilerResult
import io.swee.tvm.decompiler.internal.DecompilerOptions
import io.swee.tvm.decompiler.internal.TvmDecompilerImpl

object TvmDecompilerFacadeImpl : TvmDecompilerFacade {

    override fun decompileBoc(boc: ByteArray, exact: Boolean): TvmDecompilerResult {
        return TvmDecompilerImpl.decompile(boc, DecompilerOptions(exact = exact))
    }

    override fun decompileAddress(address: String): TvmDecompilerResult {
        throw UnsupportedOperationException(
            "Address-based decompilation is not yet implemented. " +
            "Use the 'boc' subcommand with a BOC file or literal."
        )
    }
}
