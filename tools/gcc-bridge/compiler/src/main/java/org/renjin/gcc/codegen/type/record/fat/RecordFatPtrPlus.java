package org.renjin.gcc.codegen.type.record.fat;

import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.record.RecordClassTypeStrategy;
import org.renjin.gcc.gimple.type.GimpleType;

public class RecordFatPtrPlus extends AbstractExprGenerator {

  private RecordClassTypeStrategy recordTypeStrategy;
  private ExprGenerator basePointer;
  private ExprGenerator offset;

  public RecordFatPtrPlus(RecordClassTypeStrategy recordTypeStrategy, ExprGenerator basePointer, ExprGenerator offset) {
    this.recordTypeStrategy = recordTypeStrategy;
    this.basePointer = basePointer;
    this.offset = offset;
  }

  @Override
  public GimpleType getGimpleType() {
    return basePointer.getGimpleType();
  }

  @Override
  public void emitPushPtrArrayAndOffset(MethodGenerator mv) {
    basePointer.emitPushPtrArrayAndOffset(mv);
    offsetToElements(offset, recordTypeStrategy.getRecordType().getSize()).emitPrimitiveValue(mv);
    mv.visitInsn(Opcodes.IADD);
  }
}