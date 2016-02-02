package org.renjin.gcc.codegen.type.record.unit;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.FieldStrategy;
import org.renjin.gcc.codegen.type.record.RecordClassTypeStrategy;
import org.renjin.gcc.gimple.type.GimplePointerType;
import org.renjin.gcc.gimple.type.GimpleType;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class RecordUnitPtrFieldStrategy extends FieldStrategy {
  private String className;
  private String fieldName;
  private RecordClassTypeStrategy strategy;

  public RecordUnitPtrFieldStrategy(String className, String fieldName, RecordClassTypeStrategy strategy) {
    this.className = className;
    this.fieldName = fieldName;
    this.strategy = strategy;
  }

  @Override
  public void emitInstanceField(ClassVisitor cv) {
    emitField(ACC_PUBLIC, cv);
  }

  private void emitField(int access, ClassVisitor cv) {
    cv.visitField(access, fieldName, strategy.getJvmType().getDescriptor(), null, null).visitEnd();
  }

  @Override
  public ExprGenerator memberExprGenerator(ExprGenerator instanceGenerator) {
    return new Member(instanceGenerator);
  }

  private class Member extends AbstractExprGenerator implements RecordUnitPtrGenerator {

    private ExprGenerator instanceGenerator;

    public Member(ExprGenerator instanceGenerator) {
      this.instanceGenerator = instanceGenerator;
    }

    @Override
    public GimpleType getGimpleType() {
      return new GimplePointerType(strategy.getRecordType());
    }

    @Override
    public void emitPushRecordRef(MethodGenerator mv) {
      instanceGenerator.emitPushRecordRef(mv);
      mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, strategy.getJvmType().getDescriptor());
    }

    @Override
    public void emitStore(MethodGenerator mv, ExprGenerator valueGenerator) {
      instanceGenerator.emitPushRecordRef(mv);
      valueGenerator.emitPushRecordRef(mv);
      mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, strategy.getJvmType().getDescriptor());
    }

    @Override
    public ExprGenerator valueOf() {
      return new DereferencedUnitRecordPtr(strategy, this);
    }
  }
}