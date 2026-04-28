package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.TvmTupleUntupleInst

fun registerTupleParsers(registry: ParserRegistry) {
   with(registry) {
       register<TvmTupleUntupleInst>(ParserLevel.MANUAL) { ctx, inst ->
           val tuple = ctx.stackPop(TvmStackEntryType.TUPLE.typename)
           val tupleType = tuple.type

           val entries = if (tupleType is TvmStackEntryType.TUPLE && tupleType.elements.size == inst.n) {
               tupleType.elements.mapIndexed { index, el ->
                   StackEntry.Simple(el, name("element_$index"))
               }
           } else {
               (0 until inst.n).map { index ->
                   StackEntry.Simple(TvmStackEntryType.UNKNOWN, name("element_$index"))
               }
           }
           for (entry in entries) {
               ctx.stackPush(entry)
           }

           ctx.appendNode(
               IRNode.VariableDeclaration(entries, IRNode.VariableUsage(tuple, true), untuple = true)
           )
       }
   }
}
