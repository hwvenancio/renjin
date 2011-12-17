package r.compiler.ir.optimize;

import javax.security.auth.login.Configuration;

import r.compiler.cfg.ControlFlowGraph;
import r.compiler.cfg.DominanceTree;
import r.compiler.ir.ssa.SsaTransformer;
import r.compiler.ir.tac.IRFunction;
import r.compiler.ir.tac.IRProgram;
import r.compiler.ir.tac.IRScope;

/**
 * Optimizes an R program statically. (Ahead of time)
 * 
 */
public class StaticOptimizer {

  
  private final IRProgram program;
  
  public StaticOptimizer(IRProgram program) {
    this.program = program;
  }
  
  public void optimize() {
    doIntraScopeOptimizations();
  }

  private void doIntraScopeOptimizations() {
    System.out.println("MAIN:");
    doIntraScopeOptimization(program.getMain());
    for(IRFunction function : program.getFunctions()) {
      System.out.println(function);
      doIntraScopeOptimization(function.getScope());
    }
  }

  private void doIntraScopeOptimization(IRScope main) {
    ControlFlowGraph cfg = new ControlFlowGraph(main);
    DominanceTree dtree = new DominanceTree(cfg);
    SsaTransformer ssaTransformer = new SsaTransformer(cfg, dtree);
    ssaTransformer.transform();
    
    System.out.println(cfg);
    
  }
}
