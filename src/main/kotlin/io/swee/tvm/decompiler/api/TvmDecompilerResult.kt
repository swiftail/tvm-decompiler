package io.swee.tvm.decompiler.api

interface TvmDecompilerResult {
    interface File {
        val name: String
        val content: String
    }
    val files: List<File>
}
