package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmInst

object InstValueAccessor {
    fun <T : TvmInst> getValue(
        inst: T,
        field: String
    ): Any {
        val field = inst.javaClass.declaredFields.find { it.name == field }!!
        field.isAccessible = true
        val value = field.get(inst)

        return value
    }
}
