package io.swee.tvm.decompiler.internal

object TypeLattice {
    fun join(a: TvmStackEntryType?, b: TvmStackEntryType?): TvmStackEntryType? {
        if (a == TvmStackEntryType.UNKNOWN || b == TvmStackEntryType.UNKNOWN) return TvmStackEntryType.UNKNOWN
        if (a == null) return b
        if (b == null) return a
        return if (a == b) a else TvmStackEntryType.UNKNOWN
    }
}
