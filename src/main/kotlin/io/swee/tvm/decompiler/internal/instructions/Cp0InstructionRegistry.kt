package io.swee.tvm.decompiler.internal.instructions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
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
        val instClass: Class<out TvmInst>
    )

    fun getByOpcode(asmOpcodeName: String): InstructionData? {
        return instructions[asmOpcodeName]
    }

    fun getByClass(instructionClass: Class<out TvmInst>): InstructionData? {
        return getByOpcode(instructionClassToOpcode[instructionClass] ?: return null)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstValueFlowInputsStackEntry(
        val type: String,
        val name: String?,
        val valueTypes: List<TvmCp0InstStackEntryType>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstValueFlowInputs(
        val stack: List<TvmCp0InstValueFlowInputsStackEntry>?,
        val registers: List<Any>?
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = TvmCp0InstValueFlowOutputsEntry.Simple::class, name = "simple"),
        JsonSubTypes.Type(value = TvmCp0InstValueFlowOutputsEntry.Conditional::class, name = "conditional"),
        JsonSubTypes.Type(value = TvmCp0InstValueFlowOutputsEntry.Const::class, name = "const"),
        JsonSubTypes.Type(value = TvmCp0InstValueFlowOutputsEntry.Array::class, name = "array"),
    )
    sealed class TvmCp0InstValueFlowOutputsEntry {
        abstract fun contentEquals(other: TvmCp0InstValueFlowOutputsEntry): Boolean

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Simple(
            val name: String?,
            val valueTypes: List<TvmCp0InstStackEntryType>?
        ) : TvmCp0InstValueFlowOutputsEntry() {
            override fun contentEquals(other: TvmCp0InstValueFlowOutputsEntry): Boolean {
                return other is Simple && valueTypes?.filterNot { it.name == "NULL" } == other.valueTypes?.filterNot { it.name == "NULL" }
            }
        }
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Const(
            val value: Any?,
            val valueType: TvmCp0InstStackEntryType
        ) : TvmCp0InstValueFlowOutputsEntry() {
            override fun contentEquals(other: TvmCp0InstValueFlowOutputsEntry): Boolean {
                return other is Const && valueType == other.valueType
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Conditional(
            val name: String,
            val match: List<Match>
        ) : TvmCp0InstValueFlowOutputsEntry() {
            data class Match(
                val value: Long,
                val stack: List<TvmCp0InstValueFlowOutputsEntry>?
            )

            override fun contentEquals(other: TvmCp0InstValueFlowOutputsEntry): Boolean {
                return other is Conditional && match == other.match
            }
        }
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Array(
            val name: String,
            val lengthVar: String,
            val arrayEntry: List<TvmCp0InstValueFlowOutputsEntry>
        ): TvmCp0InstValueFlowOutputsEntry() {
            override fun contentEquals(other: TvmCp0InstValueFlowOutputsEntry): Boolean {
                return other is Array && other.lengthVar == lengthVar && other.arrayEntry == arrayEntry
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstValueFlowOutputs(
        val stack: List<TvmCp0InstValueFlowOutputsEntry>?,
        val registers: List<Any>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstValueFlow(
        val inputs: TvmCp0InstValueFlowInputs,
        val outputs: TvmCp0InstValueFlowOutputs,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstControlFlow(
        val branches: List<Any>,
        val nobranch: Boolean
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstDoc(
        val fift: String?,
        val category: String?
    )

    enum class TvmCp0InstBytecodeOperandType {
        @JsonProperty("uint")
        UINT,
        @JsonProperty("int")
        INT,
        @JsonProperty("pushint_long")
        PUSHINT_LONG,
        @JsonProperty("ref")
        REF,
        @JsonProperty("subslice")
        SUBSLICE,
    }

    enum class TvmCp0InstStackEntryType {
        @JsonProperty("Integer")
        INT,
        @JsonProperty("Tuple")
        TUPLE,
        @JsonProperty("Null")
        NULL,
        @JsonProperty("Cell")
        CELL,
        @JsonProperty("Slice")
        SLICE,
        @JsonProperty("Continuation")
        CONTINUATION,
        @JsonProperty("Builder")
        BUILDER,
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Add::class, name = "add"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Plduz::class, name = "plduz"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Stack::class, name = "stack"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Pushint4::class, name = "pushint4"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Continuation::class, name = "continuation"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.OptionalNargs::class, name = "optional_nargs"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Register::class, name = "register"),
        JsonSubTypes.Type(value = TvmCp0InstBytecodeOperandDisplayHint.Dictionary::class, name = "dictionary"),
    )
    sealed class TvmCp0InstBytecodeOperandDisplayHint {
        data class Add(
            val value: Long
        ) : TvmCp0InstBytecodeOperandDisplayHint()
        data object Plduz : TvmCp0InstBytecodeOperandDisplayHint()
        data object Stack : TvmCp0InstBytecodeOperandDisplayHint()
        data object Pushint4 : TvmCp0InstBytecodeOperandDisplayHint()
        data object Continuation : TvmCp0InstBytecodeOperandDisplayHint()
        data object OptionalNargs : TvmCp0InstBytecodeOperandDisplayHint()
        data object Register : TvmCp0InstBytecodeOperandDisplayHint()
        data class Dictionary(
            val sizeVar: String
        ) : TvmCp0InstBytecodeOperandDisplayHint()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstBytecodeOperand(
        val type: TvmCp0InstBytecodeOperandType,
        val name: String,
        val displayHints: List<TvmCp0InstBytecodeOperandDisplayHint>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0InstBytecode(
        val operands: List<TvmCp0InstBytecodeOperand>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0Inst(
        val mnemonic: String,
        val bytecode: TvmCp0InstBytecode,
        val valueFlow: TvmCp0InstValueFlow,
        val controlFlow: TvmCp0InstControlFlow,
        val doc: TvmCp0InstDoc?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0Alias(
        val mnemonic: String,
        val aliasOf: String,
        val operands: Map<String, Any>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class TvmCp0Data(
        val instructions: List<TvmCp0Inst>,
        val aliases: List<TvmCp0Alias>
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
            }.toMutableMap()

            val aliases: MutableMap<String, MutableSet<String>> =
                jsonInstructionsByName.keys.map { it to mutableSetOf(it) }.toMap().toMutableMap()

            for (item in jsonInstructions.aliases) {
                if (item.operands?.size != 0) {
                    continue
                }

                jsonInstructionsByName[item.aliasOf]?.let {
                    jsonInstructionsByName[item.mnemonic.uppercase()] = it
                    aliases[item.aliasOf]!!.add(item.mnemonic.uppercase())
                    aliases[item.mnemonic.uppercase()] = aliases[item.aliasOf]!!
                }
                jsonInstructionsByName[item.mnemonic]?.let {
                    jsonInstructionsByName[item.aliasOf.uppercase()] = it
                    aliases[item.mnemonic]!!.add(item.mnemonic.uppercase())
                    aliases[item.aliasOf.uppercase()] = aliases[item.mnemonic]!!
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

                jsonInstructionsByName[item.aliasOf]?.let {
                    jsonInstructionsByName[item.mnemonic.uppercase()] = it
                    aliases[item.aliasOf]!!.add(item.mnemonic.uppercase())
                    aliases[item.mnemonic.uppercase()] = aliases[item.aliasOf]!!
                }
                jsonInstructionsByName[item.mnemonic]?.let {
                    jsonInstructionsByName[item.aliasOf.uppercase()] = it
                    aliases[item.mnemonic]!!.add(item.mnemonic.uppercase())
                    aliases[item.aliasOf.uppercase()] = aliases[item.mnemonic]!!
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
                    InstructionData(jsonInst, libInst)
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
