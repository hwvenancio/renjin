package org.renjin.gcc.codegen.type.record.fat;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.AbstractExprGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.type.FieldStrategy;
import org.renjin.gcc.codegen.type.record.RecordClassTypeStrategy;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimpleType;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class RecordArrayFieldStrategy extends FieldStrategy {

  private GimpleArrayType arrayType;
  private String className;
  private String fieldName;
  private RecordClassTypeStrategy strategy;
  private String fieldDescriptor;

  public RecordArrayFieldStrategy(String className, String fieldName,
                                  RecordClassTypeStrategy strategy, GimpleArrayType arrayType) {
    this.className = className;
    this.fieldName = fieldName;
    this.strategy = strategy;
    this.arrayType = arrayType;
    fieldDescriptor = "[" + strategy.getJvmType().getDescriptor();
  }

  @Override
  public void emitInstanceField(ClassVisitor cv) {
    emitField(ACC_PUBLIC, cv);
  }

  private void emitField(int access, ClassVisitor cv) {
    cv.visitField(access, fieldName, fieldDescriptor, null, null).visitEnd();
  }

  @Override
  public void emitInstanceInit(MethodGenerator mv) {
    mv.visitVarInsn(Opcodes.ALOAD, 0); // this;
    mv.visitInsn(arrayType.getElementCount());
    mv.visitTypeInsn(Opcodes.ANEWARRAY, strategy.getJvmType().getInternalName());
    mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, fieldDescriptor);
  }

  @Override
  public ExprGenerator memberExprGenerator(ExprGenerator instanceGenerator) {
    return new MemberValue(instanceGenerator);
  }

  private class StaticArrayValue extends AbstractExprGenerator {

    @Override
    public GimpleType getGimpleType() {
      return arrayType;
    }

    @Override
    public ExprGenerator addressOf() {
      return new AddressOfRecordArray(this);
    }

    @Override
    public void emitStore(MethodGenerator mv, ExprGenerator valueGenerator) {
      valueGenerator.emitPushArray(mv);
      mv.visitFieldInsn(Opcodes.PUTSTATIC, className, fieldName, fieldDescriptor);
    }

    @Override
    public void emitPushArray(MethodGenerator mv) {
      mv.visitFieldInsn(Opcodes.GETSTATIC, className, fieldName, fieldDescriptor);
    }

    @Override
    public ExprGenerator elementAt(ExprGenerator indexGenerator) {
      return new RecordArrayElement(strategy, this, indexGenerator);
    }
  }
  
  private class MemberValue extends AbstractExprGenerator {

    private ExprGenerator instanceGenerator;

    public MemberValue(ExprGenerator instanceGenerator) {
      this.instanceGenerator = instanceGenerator;
    }

    @Override
    public GimpleType getGimpleType() {
      return arrayType;
    }

    @Override
    public void emitPushArray(MethodGenerator mv) {
      instanceGenerator.emitPushRecordRef(mv);
      mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, fieldDescriptor);
    }

    @Override
    public ExprGenerator addressOf() {
      return new AddressOfRecordArray(this);
    }

    @Override
    public ExprGenerator elementAt(ExprGenerator indexGenerator) {
      return new RecordArrayElement(strategy, this, indexGenerator);
    }
  }
}