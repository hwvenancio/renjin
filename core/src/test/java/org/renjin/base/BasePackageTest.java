/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.renjin.base;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.renjin.EvalTestCase;
import org.renjin.eval.EvalException;
import org.renjin.sexp.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Tests that ensure the primitives integrate nicely with the 
 * R-language functions of the base package
 */
public class BasePackageTest extends EvalTestCase {


  @Test
  public void loadBase() throws IOException {

    topLevelContext.init();
    
    StringVector letters = (StringVector) eval("letters");
    assertThat( letters.getElementAsString(0),  equalTo( "a" ));
    assertThat( letters.getElementAsString(25), equalTo( "z" ));

    eval( "assign('x', 42) ");
    assertThat( eval( "x" ) , equalTo( c(42) ));

    // make sure that closures are enclosed by the base namspace
    Closure closure = (Closure)getValue( topLevelContext.getSession().getBaseEnvironment(), "backsolve" );
    assertThat( closure.getEnclosingEnvironment(), equalTo(topLevelContext.getSession().getBaseNamespaceEnv() ));


    // make sure that base scripts are populated in both the base environment and the base namespace
    assertThat( getValue( topLevelContext.getSession().getBaseEnvironment(), "letters" ).length(), equalTo( 26 ));
    assertThat( getValue( topLevelContext.getSession().getBaseNamespaceEnv(), "letters" ).length(), equalTo( 26 ));

  }

  private SEXP getValue(Environment env, String name) {
    SEXP value = env.getVariable(name);
    if(value instanceof Promise) {
      value = value.force(topLevelContext);
    }
    return value;
  }

  @Test
  public void packageVersion() throws IOException {
    loadBasePackage();

    eval(" x <- package_version('1.2-4') ");
  }

  @Test
  public void groupGeneric() throws IOException {
    loadBasePackage();

    eval(" x <- as.numeric_version('1.2.3') ");
    eval(" y <- as.numeric_version('1.0.9') ");

    assertThat(eval(" x >= y"), equalTo(c(true)));
  }
  

  @Test
  public void versionCompare2() throws IOException {
    loadBasePackage();

    eval(" x <- as.numeric_version('2.10.1') ");
    eval(" y <- as.numeric_version('2.2.0') ");

    assertThat(eval(" x >= y"), equalTo(c(true)));
  }
  
  @Test
  public void oldRowNamesAreConverted() throws IOException {
    assumingBasePackagesLoad();
    
    eval(" xi <- list(c(55, 60, 30, 40, 11)) ");
    eval(" attr(xi, 'row.names') <- c(NA, -5) ");
    eval(" class(xi) <- 'data.frame' ");
    
    assertThat( eval(" identical(attr(xi, 'row.names'),  c('1','2','3','4','5') ) "), equalTo(c(true)));
    assertThat(eval(" identical(attributes(xi)$row.names, c('1','2','3','4','5'))"), equalTo(c(true)));
    assertThat(eval(" identical(row.names(xi), c('1','2','3','4','5')) "), equalTo(c(true)));
  }

  @Test
  public void fileInfo() throws IOException {

    loadBasePackage();

    eval("info <- file.info('" + getClass().getResource("/org/renjin/sexp/SEXP.class").getFile() + "')");

    assertThat(eval("info$isdir"), equalTo(c(false)));
    assertThat(eval("info$mode"), equalTo(c_i(Integer.parseInt("666", 8))));
  }

  @Test
  public void dquote() throws IOException {

    loadBasePackage();

    assertThat( eval(" dQuote('a') "), equalTo( c("\"a\"")) );
  }


  @Test
  public void formals() throws IOException {

    loadBasePackage();

    eval("g <- function() sys.parent() ");
    eval("f <- function() g() ");

    assertThat(eval("f()"), equalTo(c_i(1)));

    eval("g<-function() eval(formals(sys.function(sys.parent()))[['event']]) ");
    eval("f<-function(event=c('a','b','c')) g() ");

    SEXP result = eval("f(1) ");
    assertThat(result, Matchers.equalTo(c("a", "b", "c")));
  }
  
  @Test
  public void sysFunction() {
    assumingBasePackagesLoad();
    
    eval("g <- function() { y <- 99; x<- 42; function() { sys.function() }  };");
    eval("fn <- g()");
    assertThat(eval("environment(fn)$x"), equalTo(c(42)));
  }

  @Test
  public void lapply() throws Exception {
    loadBasePackage();

    eval("f<-function(a,b) a+b ");
    eval("x<-c(1)");
    assertThat( eval("lapply(x,f,2) "), equalTo(list(3d)));
  }

  @Test 
  public void genericSubscript() throws IOException {
    assumingBasePackagesLoad();

    eval("  d<-as.data.frame(list(ids=1:5)) ");
    assertThat( eval(" d[,1] "), elementsEqualTo(1, 2, 3, 4, 5));

  }

  @Test 
  public void factor() throws IOException {
    assumingBasePackagesLoad();
    
    eval(" cat <- factor(c(1:3), exclude= c(NA, NaN)) ");
    eval(" addNA(cat, ifany=TRUE) ");
    assertThat( eval("levels(cat)"), equalTo(c("1", "2", "3")));
    
    eval("nl <- length(ll <- levels(cat))");
    
    assertThat( eval("nl"), equalTo(c_i(3)));
  
    eval("exclude <- NA");
    eval("exclude <- as.vector(exclude, typeof(c(1,2,NA)))");
    assertThat(eval("is.na(exclude)"), equalTo(c(true)));
    
    // ensure that NA is NOT added as a level
    eval(" cat <- factor(c(1,2,NA)) ");
    assertThat( eval("levels(cat)"), equalTo(c("1", "2")));
    
  
  }
  

  @Test
  public void factorInteger() throws IOException {
    assumingBasePackagesLoad();
    
    eval("x <- 1:5");
    eval("exclude <- c(NA, NaN)");
    
    eval("y <- unique(x)");
    
    assertThat( eval("y"), equalTo(c_i(1,2,3,4,5)));
    
    eval("ind <- sort.list(y)");
    eval("y <- as.character(y)");
    eval("levels <- unique(y[ind])");
    
    assertThat( eval("levels"), equalTo(c("1","2","3","4", "5")));

    eval("force(ordered)");
    eval("exclude <- as.vector(exclude, typeof(x))");
    
    assertThat( eval("exclude"), equalTo( c_i(IntVector.NA, IntVector.NA)));
    
    eval("x <- as.character(x)");
    eval("levels <- levels[is.na(match(levels, exclude))]");
    
    assertThat( eval("levels"), equalTo(c("1","2","3","4","5")));
  }
  
  @Test
  public void factorIssue10() throws IOException {
    assumingBasePackagesLoad();
    
    eval(" gender <- c('F','F','F','F', 'M','M','M') ");
    eval(" gender <- factor(gender) ");
    
    assertThat( eval("class(gender) "), equalTo(c("factor")));
  }
  
  @Test
  public void factorPrint() throws IOException {
    assumingBasePackagesLoad();
    
    StringWriter stringWriter = new StringWriter();
    topLevelContext.getSession().setStdOut(new PrintWriter(stringWriter));
    
    eval(" gender <- factor(c('F','F','F','F', 'M','M','M'))");
    eval(" print(gender) ");
    
    assertThat(stringWriter.toString().replace("\r\n", "\n"), equalTo("[1] F F F F M M M\nLevels: F M\n"));
  }
  
  @Test
  public void parentFrameFromWithinEval() throws IOException {
    assumingBasePackagesLoad();
    
    eval("qq<-99");
    eval("g<-function(envir=parent.frame()) envir ");
    eval("env<-eval(parse(text= 'qq<-101;g() '), envir=parent.frame())");
    
    assertThat(eval("env$qq"), equalTo(c(101)));
  }
  

  @Test
  public void parse() throws IOException {
    loadBasePackage();

    SEXP sexp = eval(" parse(text='1', keep.source=TRUE) ");
    
    assertThat(sexp, equalTo(expression(1d)));
    
    SEXP srcref = sexp.getAttribute(Symbols.SRC_REF).getElementAsSEXP(0);
    assertThat(srcref.getS3Class(), equalTo(c("srcref")));
    
    SEXP srcfile = srcref.getAttribute(Symbols.SRC_FILE);
    assertThat(srcfile, instanceOf(Environment.class));
    assertTrue(srcfile.inherits("srcfilecopy"));
    assertTrue(srcfile.inherits("srcfile"));
  }

  @Test
  public void sapply() throws IOException {
    assumingBasePackagesLoad();
    
    eval(" x<-list() ");
    assertThat(eval("sapply(attr(~1,'vars'), deparse, width.cutoff = 500)[-1L]"), equalTo(list()));
  }

  @Test @Ignore("not working yet")
  public void lzmaDecompression() throws IOException {
    assumingBasePackagesLoad();
    
    eval("data(USArrests)");
    eval("names(USArrests)");
  }
  
  @Test
  public void asDataFrameForMatrix() throws IOException {
    assumingBasePackagesLoad();
    
    eval("g<-matrix(1:64,8)");
    eval("df<-as.data.frame(g)");
    assertThat(eval("length(unclass(df))"), equalTo(c_i(8)));
  }
  
  @Test
  public void factorEquality() throws IOException {
    assumingBasePackagesLoad();

    eval("y <- as.factor(c(1,0))");
    assertThat( eval("y == c('1', '0')"), equalTo(c(true, true)));
  }
  
  @Test
  public void outer() throws IOException {
    assumingBasePackagesLoad();
    
    eval("x <- c(1,0,1,0,1,0)");
    eval("y <- as.factor(c(1,0,1,0,1,0))");
    eval("h <- levels(y)");
    
    assertThat( eval("Y <- rep(h, rep.int(length(y), length(h)))"), equalTo(c("0","0","0","0","0","0","1","1","1","1","1","1")));
    
    eval("X <- rep(y, times = ceiling(length(h)/length(y)))");
    assertThat(eval("class(X)"), equalTo(c("factor")));
    
    eval("yp <- ifelse(outer(y,h,'=='),1,0)");
    assertThat(eval("dim(yp)"), equalTo(c_i(6, 2)));
    assertThat(eval("c(yp)"), equalTo(c(0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0)));
  }
  
  @Test
  public void issue8() throws IOException {
    assumingBasePackagesLoad();
    
    assertThat( eval("rep(seq(1,10,1),2)"), equalTo(c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
  }

  @Test
  @Ignore("has dependency on utils package!")
  public void source() throws IOException {
    assumingBasePackagesLoad();
    
    String file = BasePackageTest.class.getResource("SourceTest.R").getFile();
    global.setVariable(Symbol.get("fn"),
        StringVector.valueOf(new File(file).getAbsolutePath()));
      eval("source(fn)");
  }
  
  @Test
  public void splitAssign() throws IOException {
    assumingBasePackagesLoad();
    
    eval("n <- 10");
    eval("nn <- 100");
    eval("g <- factor(2+round(sin(1:(n*nn)*(pi/6))))");
    eval("x <- rep(c(6,4,3,1,9), length.out=n * nn) + sqrt(as.double(g))");
    eval("xg <- split(x, g)");
    eval("zz <- x");
    eval("lresult <- lapply(split(x, g), scale)");
    eval("split(zz, g) <- lresult");
  }
  
  @Test
  public void remove() throws IOException {
    assumingBasePackagesLoad();
    
    eval("a<-1");
    eval("remove(a)");
  }

  private void loadBasePackage() throws IOException {
    topLevelContext.init();
  }

  @Test
  public void bquote() throws IOException {
    assumingBasePackagesLoad();
    
    eval("x <- bquote(~0 + .(quote(births)))");
    eval("print(x)");

    // expected : ~0 + births 
    
    FunctionCall tildeCall = (FunctionCall) topLevelContext.getGlobalEnvironment().getVariable("x");
    assertThat(tildeCall.getFunction(), equalTo((SEXP) symbol("~")));    
    assertThat(tildeCall.getArguments().length(), equalTo(1));
    
    FunctionCall plusCall = (FunctionCall)tildeCall.getArgument(0);
    assertThat(plusCall.getFunction(), equalTo((SEXP) symbol("+")));    
  }

  @Test
  public void bquoteInternal() throws IOException {

    assumingBasePackagesLoad();

    eval("tt <- 1");
    eval("bq <- bquote( ~ 0 + . (tt) )");

    assertThat(eval("bq[[1]]"), equalTo((SEXP)Symbol.get("~")));
    assertThat(eval("bq[[2]][[1]]"), equalTo((SEXP)Symbol.get("+")));
    assertThat(eval("bq[[2]][[2]]"), equalTo(c(0)));
    assertThat(eval("bq[[2]][[3]]"), equalTo(c(1)));
    //R outputs ~0 + 1, renjin 0 + 1 ~
    // expected : ~0 + births


  }


  @Test
  public void rowSums() throws IOException {
    assumingBasePackagesLoad();
    
    eval("m <- matrix(1:12, 3)");
    
    assertThat(eval("rowsum(m, group=c(1,1,1))"), equalTo(c_i(6,15,24,33)));
    assertThat(eval("row.names(rowsum(m, group=c(1,1,1)))"), equalTo(c("1")));

    assertThat(eval("rowsum(m, group=c(3,3,1), reorder=TRUE)"), equalTo(c_i(3, 3, 6, 9, 9, 15, 12, 21)));

  }
  
  @Test
  public void rowLabelsFromFactors() throws IOException {
    assumingBasePackagesLoad();
    
    eval("x <- factor(c('Yes','No','No'))");
    eval("m <- matrix(c(1:6), 2, 3)");
    eval("rownames(m) <- unique(x)");
    assertThat(eval("rownames(m)"), equalTo(c("Yes","No")));
  }
  
  @Test
  @Ignore("todo")
  public void kendallCor() throws IOException {
    
    
    
  }
  
  @Test
  public void inOpWithNA() throws IOException {
    assumingBasePackagesLoad();
    
    assertThat( eval("NA %in% FALSE"), equalTo(c(false)));
    assertThat( eval("NA %in% TRUE"), equalTo(c(false))); 
  }
  
  @Test
  @Ignore("should actually fail - depends on stats package")
  public void summaryForDataFrame() throws IOException {
    try {
      assumingBasePackagesLoad();
      eval(" x <-as.data.frame(list(x=1:10,y=11:20)) ");

      assertThat(eval("max(x)"), equalTo(c_i(20)));
    } catch (EvalException e) {
      e.printRStackTrace(System.out);
      throw e;
    }
  }
 
  @Test
  public void emptyFactor() {
    assumingBasePackagesLoad();
    
    eval("x <- factor() ");
    assertThat(eval("class(x)"), equalTo(c("factor")));
    assertThat(eval("attr(x,'levels')"), equalTo((SEXP) StringVector.EMPTY));
    assertThat(eval("typeof(x)"), equalTo(c("integer")));
    assertThat(eval("is.factor(x)"), equalTo(c(true)));
  }

  @Test
  public void attributeOverflow() {
    assumingBasePackagesLoad();

    eval(" all.equal(list(names = NULL), list(names = NULL))");
  }
  
  @Test
  public void serialize() {
    
    assumingBasePackagesLoad();
    
    eval("x <- serialize(42, connection=NULL)");
    assertThat(eval("length(x)"), equalTo(c_i(30)));
    assertThat(eval("x[1:6]"), equalTo(raw(0x58, 0x0a, 0x00, 0x00, 0x00, 0x02)));
  }

  private SEXP raw(int... integers) {
    RawVector.Builder vector = new RawVector.Builder();
    for(int i : integers) {
      vector.add(i);
    }
    return vector.build();
  }
  
  @Test
  public void recall() {
    assumingBasePackagesLoad();
    
    eval("fib <- function(n) if(n<=2) { if(n>=0) 1 else 0 } else Recall(n-1) + Recall(n-2)");
    eval("fibonacci <- fib");
    eval("rm(fib)");
    assertThat(eval("fibonacci(10)"), equalTo(c(55)));
  }
  
  @Test
  public void mapply() {
    assumingBasePackagesLoad();
    
    assertThat(eval("mapply(rep, 1:4, 4:1)"), equalTo(list(
        c_i(1,1,1,1),
        c_i(2,2,2),
        c_i(3,3),
        c_i(4)
        )));
  }
  
  @Test
  public void assignInClosure() {
    assumingBasePackagesLoad();
    
    eval(" f <- function() { y<-66; fieldClasses <- NULL; assign('fieldClasses', 42); fieldClasses; } ");
      
    assertThat(eval("f()"), equalTo(c(42)));
    
  }

  @Test
  public void ls() {
    assumingBasePackagesLoad();
    eval("x<-41");
    eval(".Foo <- 'bar'");
    eval("print(ls(all.names=TRUE))");
  }
  
  @Test
  public void setBody() {
    assumingBasePackagesLoad();
    eval("f <- function(x,y,z) y ");
    eval("body(f) <- quote(x) ");
    assertThat(eval("f(42)"), equalTo(c(42)));
  }
  
  @Test
  public void setFormals() {
    assumingBasePackagesLoad();
    
    eval(" f <- function(x) {  .findNextFromTable(method, f, optional, envir) }");
    eval(" bd <- body(f)");
    eval(" print(typeof(if(is.null(bd) || is.list(bd)) list(bd) else bd)) ");
    eval(" value <-  alist(method=,f='<unknown>', mlist=,optional=FALSE,envir=) ");
    eval(" newf <- c(value, if(is.null(bd) || is.list(bd)) list(bd) else bd) ");
    eval(" print(newf) ");
    assertThat(eval("length(newf)"), equalTo(c_i(6)));
  }
  
  @Test
  public void isR() {
    assumingBasePackagesLoad();
    assertThat(eval("is.R()"), equalTo(c(true)));
  }

  @Test
  public void cut() {

    assertThat(eval(" cut(c(1,2,3,4,5,6), breaks=c(0,2,6))"), equalTo(c_i(1,1,2,2,2,2)));
    assertThat(eval(" cut(c(1,2,3,4,5,6), breaks=c(0,2,6), right=F)"),
            equalTo(c_i(1,2,2,2,2,IntVector.NA)));
    assertThat(eval(" cut(c(1,2,3,4,5,6), breaks=c(0,2,6), right=F, include.lowest=T)"),
            equalTo(c_i(1,2,2,2,2,2)));
  }


}
