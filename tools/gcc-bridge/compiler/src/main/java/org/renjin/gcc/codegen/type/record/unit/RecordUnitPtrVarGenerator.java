package org.renjin.gcc.codegen.type.record.unit;

import com.google.common.base.Optional;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.VarGenerator;
import org.renjin.gcc.codegen.type.record.RecordTypeStrategy;
import org.renjin.gcc.codegen.var.Var;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.gimple.type.GimpleType;

public class RecordUnitPtrVarGenerator extends AbstractExprGenerator implements VarGenerator, RecordUnitPtrGenerator {
  private Var var;
  private RecordTypeStrategy strategy;
  private GimpleType pointerType;

  public RecordUnitPtrVarGenerator(RecordTypeStrategy strategy, Var var) {
    this.strategy = strategy;
    this.var = var;
    this.pointerType = new GimplePointerType(strategy.getRecordType());
  }
  
  @Override
  public GimpleType getGimpleType() {
    return pointerType;
  }

  @Override
  public ExprGenerator valueOf() {
    return new ValueOf();
  }


  @Override
  public void emitPushRecordRef(MethodVisitor mv) {
    var.load(mv);
  }

  @Override
  public void emitStore(MethodVisitor mv, ExprGenerator valueGenerator) {
    valueGenerator.emitPushRecordRef(mv);
    var.store(mv);
  }

  @Override
  public void emitDefaultInit(MethodVisitor mv, Optional<ExprGenerator> initialValue) {
    if(initialValue.isPresent()) {
      emitStore(mv, initialValue.get());
    }
  }

  @Override
  public void emitPushPtrArrayAndOffset(MethodVisitor mv) {
    mv.visitInsn(Opcodes.ICONST_1);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, strategy.getJvmType().getInternalName());
    mv.visitInsn(Opcodes.ICONST_0);
  }

  private class ValueOf extends AbstractExprGenerator {

    @Override
    public GimpleType getGimpleType() {
      return pointerType.getBaseType();
    }

    @Override
    public ExprGenerator addressOf() {
      return RecordUnitPtrVarGenerator.this;
    }

    @Override
    public ExprGenerator memberOf(String memberName) {
      return strategy.getFieldGenerator(memberName).memberExprGenerator(RecordUnitPtrVarGenerator.this);
    }
  }
}