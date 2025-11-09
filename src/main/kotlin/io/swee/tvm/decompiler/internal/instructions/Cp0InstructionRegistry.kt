package io.swee.tvm.decompiler.internal.instructions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.serialization.SerialName
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.ton.bytecode.TvmInst

class Cp0InstructionRegistry private constructor(
    val instructions: Map<String, InstructionData>,
    val instructionClassToOpcode: Map<Class<out TvmInst>, String>
) {
    data class InstructionData(
        val instDescriptionRaw: TvmCp0Inst,
        val instClass: Class<out TvmInst>,
        val instDescription: TvmInstSimple
    )

    fun getByOpcode(asmOpcodeName: String): InstructionData? {
        return instructions[asmOpcodeName]
    }

    fun getByClass(instructionClass: Class<out TvmInst>): InstructionData? {
        return getByOpcode(instructionClassToOpcode[instructionClass] ?: return null)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstValueFlowInputsStackEntry(
        val type: String,
        val name: String?,
        val value_types: List<String>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstValueFlowInputs(
        val stack: List<TvmCp0InstValueFlowInputsStackEntry>?,
        val registers: List<Any>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstValueFlowOutputsEntry(
        val type: String,
        val name: String?,
        val value_types: List<String>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstValueFlowOutputs(
        val stack: List<TvmCp0InstValueFlowOutputsEntry>?,
        val registers: List<Any>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstValueFlow(
        val inputs: TvmCp0InstValueFlowInputs,
        val outputs: TvmCp0InstValueFlowOutputs,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstControlFlow(
        val branches: List<Any>,
        val nobranch: Boolean
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstDoc(
        val fift: String?,
        val category: String?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0InstBytecode(
        val operands: List<Any>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0Inst(
        val mnemonic: String,
        val bytecode: TvmCp0InstBytecode,
        val value_flow: TvmCp0InstValueFlow,
        val control_flow: TvmCp0InstControlFlow,
        val doc: TvmCp0InstDoc?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0Alias(
        val mnemonic: String,
        val alias_of: String,
        val operands: Map<String, Any>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TvmCp0Data(
        val instructions: List<TvmCp0Inst>,
        val aliases: List<TvmCp0Alias>
    )

    data class TvmInstSimpleStackEntry(
        val type: String,
        val name: String,
        val value_types: List<String>,
    )

    data class TvmInstSimple(
        val inputStack: List<TvmInstSimpleStackEntry>,
        val outputStack: List<TvmInstSimpleStackEntry>,
    )

    companion object {
        fun create(): Cp0InstructionRegistry {
            val jsonInstructions: TvmCp0Data = ObjectMapper().registerModule(KotlinModule.Builder().build()).readValue(
                TvmInst::class.java.getResourceAsStream("/cp0.json")
                    .readAllBytes(),
                TvmCp0Data::class.java
            )

            val jsonInstructionsByName = jsonInstructions.instructions.associateBy {
                it.mnemonic.uppercase()
            }.mapNotNull { (k, v) ->
                val inputsStack = v.value_flow.inputs.stack ?: emptyList()
                val outputsStack = v.value_flow.outputs.stack ?: emptyList()

                var idxInput = 0
                var idxOutput = 0
                k to (v to TvmInstSimple(
                    inputsStack.map {
                        TvmInstSimpleStackEntry(
                            it.type,
                            it.name ?: ("__var_${(idxInput++ + 'a'.code).toChar()}"),
                            it.value_types ?: emptyList()
                        )
                    },
                    outputsStack.map {
                        TvmInstSimpleStackEntry(
                            it.type,
                            it.name ?: ("__var_${(idxOutput++ + 'a'.code).toChar()}"),
                            it.value_types ?: emptyList()
                        )
                    },
                ))
            }.toMap().toMutableMap()

            val aliases: MutableMap<String, MutableSet<String>> =
                jsonInstructionsByName.keys.map { it to mutableSetOf(it) }.toMap().toMutableMap()

            for (item in jsonInstructions.aliases) {
                if (item.operands?.size != 0) {
                    continue
                }

                jsonInstructionsByName[item.alias_of]?.let {
                    jsonInstructionsByName[item.mnemonic.uppercase()] = it
                    aliases[item.alias_of]!!.add(item.mnemonic.uppercase())
                    aliases[item.mnemonic.uppercase()] = aliases[item.alias_of]!!
                }
                jsonInstructionsByName[item.mnemonic]?.let {
                    jsonInstructionsByName[item.alias_of.uppercase()] = it
                    aliases[item.mnemonic]!!.add(item.mnemonic.uppercase())
                    aliases[item.alias_of.uppercase()] = aliases[item.mnemonic]!!
                }
            }
            for (item in jsonInstructions.instructions) {
                val fift = item.doc?.fift ?: continue

                for (line1 in fift.lineSequence().map { it.trim().uppercase() }) {
                    for (line2 in fift.lineSequence().map { it.trim().uppercase() }) {
                        if (line1 != line2) {
                            jsonInstructionsByName[line1]?.let {
                                jsonInstructionsByName[line2] = it
                                aliases[line1]!!.add(line2)
                                aliases[line2] = aliases[line1]!!
                            }
                            jsonInstructionsByName[line2]?.let {
                                jsonInstructionsByName[line1] = it
                                aliases[line2]!!.add(line1)
                                aliases[line1] = aliases[line2]!!
                            }
                        }
                    }
                }
            }
            for (item in jsonInstructions.aliases) {
                if (item.operands?.size != 0) {
                    continue
                }

                jsonInstructionsByName[item.alias_of]?.let {
                    jsonInstructionsByName[item.mnemonic.uppercase()] = it
                    aliases[item.alias_of]!!.add(item.mnemonic.uppercase())
                    aliases[item.mnemonic.uppercase()] = aliases[item.alias_of]!!
                }
                jsonInstructionsByName[item.mnemonic]?.let {
                    jsonInstructionsByName[item.alias_of.uppercase()] = it
                    aliases[item.mnemonic]!!.add(item.mnemonic.uppercase())
                    aliases[item.alias_of.uppercase()] = aliases[item.mnemonic]!!
                }
            }
            for (item in jsonInstructions.instructions) {
                val fift = item.doc?.fift ?: continue

                for (line1 in fift.lineSequence().map { it.trim().uppercase() }) {
                    for (line2 in fift.lineSequence().map { it.trim().uppercase() }) {
                        if (line1 != line2) {
                            jsonInstructionsByName[line1]?.let {
                                jsonInstructionsByName[line2] = it
                                aliases[line1]!!.add(line2)
                                aliases[line2] = aliases[line1]!!
                            }
                            jsonInstructionsByName[line2]?.let {
                                jsonInstructionsByName[line1] = it
                                aliases[line2]!!.add(line1)
                                aliases[line1] = aliases[line2]!!
                            }
                        }
                    }
                }
            }

            val libInstructions: Map<String, Class<TvmInst>> = Reflections("org.ton.bytecode")
                .get(
                    Scanners.TypesAnnotated.of(TvmInst::class.java)
                        .add<Any>(Scanners.TypesAnnotated.of(SerialName::class.java))
                )
                .map {
                    val klass = Class.forName(it)
                    val annotation = klass.getAnnotation(SerialName::class.java)
                    val opName = annotation.value
                    opName to klass
                }.toMap() as Map<String, Class<TvmInst>>

            val libInstructionsByName: Map<String, InstructionData> = jsonInstructionsByName.map { (name, jsonInst) ->
                val aliasData = aliases[name]!!.firstNotNullOf {
                    val libInst = libInstructions[it] ?: return@firstNotNullOf null
                    InstructionData(jsonInst.first, libInst, jsonInst.second)
                }
                name to aliasData
            }.toMap()

            val instructionClassToOpcode = libInstructionsByName.entries.associate { it.value.instClass to it.key }

            return Cp0InstructionRegistry(
                libInstructionsByName,
                instructionClassToOpcode
            )
        }
    }
}
