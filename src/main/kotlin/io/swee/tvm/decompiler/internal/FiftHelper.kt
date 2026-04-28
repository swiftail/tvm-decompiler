package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.instructions.Cp0InstructionRegistry
import org.ton.bytecode.TvmInst

object FiftHelper {

    data class AsmConstraint(val fieldName: String, val expectedValue: String)

    data class InstructionDef(
        val opcodeName: String,
        val constraints: List<AsmConstraint>
    )

    private val IDIOMS = listOf(
        Regex("c(\\d+)\\s+PUSH") to "$1 PUSHCTR",
        Regex("c(\\d+)\\s+POP") to "$1 POPCTR",
    )

    fun parseSequence(
        asmExpr: String,
        cp0: Cp0InstructionRegistry
    ): List<InstructionDef> {
        val result = mutableListOf<InstructionDef>()
        val stack = ArrayDeque<String>()

        var asmExpr = asmExpr
        for ((regex, replacement) in IDIOMS) {
            asmExpr = asmExpr.replace(regex, replacement)
        }

        val tokens = asmExpr.replace("\"", "").trim().split(Regex("\\s+"))

        for (token in tokens) {
            if (token.isEmpty()) continue

            val instData = cp0.getByOpcode(token)

            if (instData != null) {
                val operands = instData.instDescriptionRaw.bytecode.operands

                val constraints = mutableListOf<AsmConstraint>()

                for (operand in operands.reversed()) {
                    if (stack.isEmpty()) {
                        break
                    }
                    val value = stack.removeLast()
                    constraints.add(AsmConstraint(operand.name, value))
                }

                result.add(InstructionDef(token, constraints.reversed()))
            } else {
                val constant = parseConstant(token)
                stack.addLast(constant)
            }
        }

        return result
    }

    private fun parseConstant(token: String): String {
        token.toIntOrNull()?.let { return it.toBigInteger().toString() }

        if (token.matches(Regex("^c\\d+$"))) {
            return token.substring(1).toBigInteger().toString()
        }

        if (token.matches(Regex("^s\\d+$"))) {
            return token.substring(1).toInt().toBigInteger().toString()
        }

        return token
    }

    fun createPredicate(constraints: List<AsmConstraint>): (List<TvmInst>) -> Boolean {
        if (constraints.isEmpty()) return { true }
        return { inputs ->
            val primaryInst = inputs.first()
            constraints.all { (field, expected) ->
                checkField(primaryInst, field, expected)
            }
        }
    }

    private fun checkField(inst: TvmInst, fieldName: String, expected: String): Boolean {
        try {
            val actual = InstValueAccessor.getValue(inst, fieldName)

            val actualString = when (actual) {
                is Int -> actual.toBigInteger().toString()
                is Long -> actual.toBigInteger().toString()
                else -> actual.toString()
            }
            return actualString == expected
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
