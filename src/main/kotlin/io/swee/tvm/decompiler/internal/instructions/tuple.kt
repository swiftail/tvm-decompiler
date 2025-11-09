package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmTupleIndexInst
import org.ton.bytecode.TvmTupleUntupleInst

fun registerTupleParsers(registry: ParserRegistry) {
   with(registry) {
//       register<TvmTupleIndexInst> { ctx, inst, ident ->
//           val tuple = ctx.popTuple()
//           val res = newUnknown(name("val"))
//           ctx.push(res)
//           ctx.append(declaration(
//               res,
//               composition(
//                   AstElement.Raw("tuple_at("),
//                   usage(tuple),
//                   AstElement.Raw(", ${inst.k})"),
//               )
//           )).append(AstElement.Raw(";"))
//       }
       register<TvmTupleUntupleInst> { ctx, inst, ident ->
           val tuple = ctx.popTuple()
           val tupleType = tuple.type

           check(tupleType is TvmStackEntryType.TUPLE)
           check(inst.n == tupleType.elements.size)

           val entries = tupleType.elements.mapIndexed { index, el ->
               StackEntry.Simple(el, name("element_$index"))
           }
           for (entry in entries) {
               ctx.push(entry)
           }

           ctx.append(
               AstElement.VariableDeclaration(entries, AstElement.VariableUsage(tuple, true), untuple = true)
           )
       }
   }
}
