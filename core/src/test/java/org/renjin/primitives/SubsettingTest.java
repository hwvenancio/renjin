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

package org.renjin.primitives;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.renjin.EvalTestCase;
import org.renjin.eval.EvalException;
import org.renjin.primitives.special.DollarFunction;
import org.renjin.sexp.*;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.renjin.ExpMatchers.logicalVectorOf;


public class SubsettingTest extends EvalTestCase {

  @Test
  public void subsetDouble() {
    eval( " x <- c(91,92,93) ");
    assertThat( eval(" x[1] "), equalTo( c(91) ));
    assertThat( eval(" x[2] "), equalTo( c(92) ));
    assertThat( eval(" x[3] "), equalTo( c(93) ));
    assertThat( eval(" x[4] "), equalTo( c(DoubleVector.NA)) );
    assertThat( eval(" x[0] "), equalTo( (SEXP) new DoubleArrayVector() ));
    assertThat( eval(" x[NULL] "), equalTo( (SEXP) new DoubleArrayVector() ));
    assertThat( eval(" x[3L] "), equalTo( c(93) ));
  }
  
  @Test
  public void subsetNullPreservesNames() {
    eval(" x <- c(a=91,b=92,c=93) ");
    eval(" y <- x[NULL]");
    
    assertThat(eval("names(y)"), equalTo((SEXP)StringArrayVector.EMPTY));
  } 

  @Test
  public void subsetWithLogicals() {
    eval( " x <- c(91,92,93) ") ;
    assertThat( eval("x[c(TRUE,FALSE,TRUE)]"),equalTo( c(91,93)));
  }


  @Test
  public void listIndices() {
    eval(" x <- list('a', 3, NULL) ");

    assertThat( eval("x[0] "), equalTo( list() ));
    assertThat( eval("x[1] "), equalTo( list( c("a") )));
    assertThat( eval("x[99] "), equalTo( list( NULL )));
    assertThat( eval("x[1:2] "), equalTo( list( c("a"), c(3) )));
    assertThat(eval("x[2:5] "), equalTo(list(c(3), NULL, NULL, NULL)));
    assertThat( eval("x[-3] "), equalTo( list(c("a"), c(3))));
  }

  @Test
  public void emptyListNegativeIndices() {
    eval(" x <- list() ");

    assertThat( eval("x[4]"), equalTo(list(Null.INSTANCE)));
    assertThat( eval("x[-1L]"), equalTo(list()));
  }

  @Test
  public void subsetDoubleMultipleIndices() {
    eval( " x <- c(91,92,93) ");
    assertThat( eval(" x[2:3] "), equalTo( c(92, 93) ));
    assertThat( eval(" x[3:5] "), equalTo( c(93, DoubleVector.NA, DoubleVector.NA) ));
  }

  @Test
  public void stringSubset() {
    eval(" x <- c('a','b','c') ");

    assertThat( eval("x[0] "), equalTo( CHARACTER_0 ));
    assertThat( eval("x[1] "), equalTo( c("a") ));
    assertThat( eval("x[99] "), equalTo( c( StringVector.NA )));
    assertThat(eval("x[1:2] "), equalTo(c("a", "b")));
    assertThat( eval("x[2:5] "), equalTo( c("b", "c", StringVector.NA, StringVector.NA )));
    assertThat( eval("x[-3] "), equalTo( c("a", "b")));
  }

  @Test
  public void stringSubsetAssign() {
    eval(" x <- c('a', 'b', 'c') ");
    eval(" x[1] <- 'z' ");

    assertThat( eval(" x "), equalTo( c("z", "b", "c")));
  }

  @Test
  public void assignNarrower() {
    eval(" x <- c('a', 'b', 'c') ");
    eval(" x[4] <- 36 ");

    assertThat( eval(" x "), equalTo( c("a", "b", "c", "36")));
  }

  @Test
  public void assignWider() {
    eval(" x <- c(1,2,3) ");
    eval(" x[2] <- c('foo') ");

    assertThat( eval(" x "), equalTo( c("1", "foo", "3")));
  }

  @Test
  public void negativeIndices() {
    eval(" x <- c(91,92,93)  ");
    assertThat( eval(" x[-1] "), equalTo( c(92,93)));
    assertThat(eval(" x[-1:-2] "), equalTo(c(93)));
    assertThat(eval(" x[c(-2,-241)] "), equalTo(c(91, 93)));
    assertThat( eval(" x[c(-1,0,0)] "), equalTo( c(92,93)));
  }

  @Test
  public void negativeIndicesOnMatrix() {
    eval(" x<-1:8 ");
    eval(" dim(x) <- c(2,4)");

    assertThat( eval("x[,-4]"), equalTo( c_i(1, 2, 3, 4, 5, 6)));
  }

  @Test(expected = EvalException.class)
  public void mixedNegativeAndPos() {
    eval(" x <- c(91,92) ");
    eval(" x[c(-1,4)] ");
  }

  @Test
  public void setDoubleSubset() {
    eval(" x <- c(91, 92, 93) ");
    eval(" x[1] <- 44 ");

    assertThat(eval("x"), equalTo(c(44, 92, 93)));
  }

  @Test
  public void logicalIndices() {
    eval("x <- c(21,22,23) ");

    assertThat( eval(" x[TRUE] "), equalTo( c(21,22,23)));
    assertThat( eval(" x[FALSE] "), equalTo( DOUBLE_0 ));
    assertThat(eval(" x[NA] "), equalTo(c(DoubleVector.NA, DoubleVector.NA, DoubleVector.NA)));
    assertThat(eval(" x[c(TRUE,FALSE,TRUE)] "), equalTo(c(21, 23)));
    assertThat( eval(" x[c(TRUE,FALSE)] "), equalTo( c(21, 23) ));
  }
  
  @Test(expected = EvalException.class)
  public void logicalMatrixSubscriptsCannotBeLongerThanTheDimension() {
    eval("x <- matrix(1:4, nrow=2)");
    
    eval("x[1, c(TRUE,TRUE,TRUE)]");
  }

  @Test
  public void longLogicalSubscripts() {
    eval("x <- 1:2");
    
    assertThat(eval("x[c(TRUE,TRUE,TRUE)]"), equalTo(c_i(1, 2, IntVector.NA)));
    assertThat(eval("x[c(TRUE,TRUE,FALSE,TRUE)]"), equalTo(c_i(1, 2, IntVector.NA)));
  }

  @Test
  public void longLogicalSubscriptsReplacement() {
    eval("x <- 1:2");
    eval("x[c(TRUE,TRUE,FALSE,TRUE)]<-4L");
    
    assertThat(eval("x"), equalTo(c_i(4, 4, IntVector.NA, 4)));
  }
  
  
  @Test
  public void missingSubscript() {
    eval("x <- 41:43");

    assertThat( eval(" x[] "), equalTo( c_i(41,42,43)));
  }
  
  @Test
  public void nrowsOnZeroWidthMatrix() {
    eval("m <- 1:12" );
    eval("dim(m) <- c(3,4)");
    eval("m2 <- m[FALSE,,drop=FALSE]");
    assertThat(eval("dim(m2)[2]"), equalTo(c_i(4)));
    eval("l2 <- .Internal(vector('logical',0)) ");
    eval("fin <- m2[!l2,,drop=FALSE]");
    assertThat(eval("dim(fin)"), equalTo(c_i(0,4)));
    
  }

  @Test
  public void namedSubscripts() {
    eval("x <- c(a=3, b=4) ");

    assertThat(eval(" x['a'] "), equalTo(c(3)));
    assertThat( eval(" names(x['a']) "), equalTo( c("a")));
  }

  @Test
  public void namesPreservedCorrectly() {
    eval("x <- c(a=3, 99, b=4) ");

    assertThat( eval(" names(x[c(1,2,NA)]) "), equalTo( c( "a", "", StringVector.NA)));
  }

  @Test
  public void setDoubleRange() {
    eval(" x <- c(91, 92, 93) ");
    eval(" x[1:2] <- c(81,82) ");

    assertThat( eval("x"), equalTo( c( 81, 82, 93 )));
  }

  @Test
  public void setWithLogicalSubscripts() {
    eval(" x <- 1:3 ");
    eval(" x[c(FALSE,TRUE,FALSE)] <- 99");

    assertThat( eval("x"), equalTo( c(1,99,3)));
  }


  @Test
  public void setWithLogicalSubscripts2() {
    eval(" x <- 1:4 ");
    eval(" x[c(FALSE,TRUE)] <- c(91,92)");

    assertThat( eval("x"), equalTo( c(1,91,3,92)));
  }


  @Test
  public void setDoubleRangeMultiple() {
    eval(" x <- c(91, 92, 93) ");
    eval(" x[2:3] <- 63 ");

    assertThat( eval("x"), equalTo( c( 91, 63, 63 )));
  }

  @Test
  public void setDoubleRangeMultipleNewLength() {
    eval(" x <- c(91, 92, 93) ");
    eval(" x[2:5] <- 63 ");

    assertThat( eval("x"), equalTo( c( 91, 63, 63, 63, 63 )));
  }

  @Test
  public void subsetOfPosAndZeroIndices() {
    eval("  x<-c(91, 92, 93, 94, 95) ");

    assertThat( eval("x[c(1,0,1)]"), Matchers.equalTo((SEXP) new DoubleArrayVector(91, 91)));
  }

  @Test
  public void setNoElements() {
    eval(" x<- c(1,2,3) ");
    eval("x[FALSE]<-c()");

    assertThat( eval("x") , equalTo(c(1, 2, 3)));
  }
  
  @Test
  public void emptyLogicalVectorWithNoDimsIsAlwaysNull() {
    assertThat(eval(" c()[1]"), equalTo(NULL));
  }
  
  @Test
  public void emptyVectorWithDimsIsNotNull() {
    eval("x<-TRUE[-1]");
    eval("dim(x) <- c(0,1)");
    assertThat(eval(" x[1]"), equalTo(c(Logical.NA)));
  }
  
  @Test
  public void emptyDoubleVectorIsNotNull() {
    eval("select <- TRUE[-1]");
    eval("x <- 1[-1]");
    assertThat(eval(" x[select]"), equalTo((SEXP) DoubleVector.EMPTY));
  }
  
  @Test
  public void listElementByName() {
    eval(" p <- list(x=33, y=44) ");

    assertThat( eval("p$x"), equalTo( c(33) ));
  }

  @Test
  public void setListElementByName() {
    eval(" p <- list( x = 44 ) ");
    assertThat( eval(" names(p) "), equalTo( c("x") ));

    eval(" p$x <- 88 ");

    assertThat( eval(" p$x "), equalTo( c(88) ));
  }

  @Test
  public void replaceListElementWithList() {
    eval(" restarts <- list( list(name='foo'), list(name='zig'), list(name='zag') ) ");

    assertThat( eval("restarts[[2]]$name "), equalTo(c("zig")));

    eval(" name <- 'bar' ");
    eval(" i <- 2 ");
    eval(" restarts[[i]]$name <- name ");

    assertThat( eval("restarts[[2]]$name "), equalTo(c("bar")));
  }

  @Test
  public void replaceAllListElements() {
    eval(" x <- list(1,2,3) ");
    eval(" x[] <- list(4,5,6) ");
    
    assertThat(eval("x"), equalTo(list(4d, 5d, 6d)));
    
    // GNU R emits an error but we don't
    eval(" x[] <- list(1,2)");
    assertThat(eval("x"), equalTo(list(1d, 2d, 1d)));
  }
  
  @Test
  public void replaceAllElements() {
    eval("x <- c(1,2,3)");
    eval("x[] <- c(4,5)");
  }
  
  @Test
  public void replaceAllElementsWithList() {
    eval("x <- c(1,2,3,4)");
    eval("x[] <- list(5,6)");
    
    assertThat(eval("x"), equalTo(list(5d, 6d, 5d, 6d)));
  }
  
  @Test
  public void replaceAllElementsPreservesNames() {
    eval("x <- c(a=1,b=2)");
    eval("x[] <- 98:99");
    
    assertThat(eval("names(x)"), equalTo(c("a", "b")));
  }
  
  @Test(expected = EvalException.class)
  public void replaceElementInAtomicVectorWithNullFails() {
    eval(" x <- c(1,2,3) ");
    eval(" x[[1]] <- NULL ");
  }

      // x[1] <- NULL and x[1] <- c() both remove the first element
    // x[1] <- list() sets the first element to an empty list
    // x[[1]] <- list() throws an error

  @Test(expected = EvalException.class)
  public void replaceSingleElementInListWithEmptyListThrows() {
    eval(" x<- c(1,2,3) ");
    eval(" x[[1]] <- list() ");
  }

  @Test
  public void replaceSingleElementInListWithNullRemovesElement() {
    eval(" x <- list(1,2,3) ");
    eval(" x[[1]] <- NULL ");

    assertThat(eval("x"), equalTo(list(2d, 3d)));
  }
  
  @Test
  public void replaceSingleElementInListMatrix() {
    eval(" x<- list(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");
    
    eval(" x[[2,2]] <- 99 ");
    
    assertThat(eval("x"), equalTo(list(1d, 2d, 3d, 99d)));
    assertThat(eval("dim(x)"), equalTo(c_i(2, 2)));
  }
  
  @Test
  public void replaceSingleElementInMatrixWithLogicals() {
    eval(" x<- c(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");
    
    eval("x[[TRUE, TRUE]] <- 91");
    
    assertThat(eval("x"), equalTo(c(91, 2, 3, 4)));
  }
  
  @Test
  public void replaceSingleElementInListMatrixByName() {
    eval(" x<- list(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");

    eval(" x[['foo']] <- 99 ");

    assertThat(eval("x"), equalTo(list(1d, 2d, 3d, 4d, 99d)));

    // Dimensions should be dropped
    assertThat(eval("dim(x)"), equalTo((SEXP) Null.INSTANCE));
  }


  @Test(expected = EvalException.class)
  public void replaceSingleElementInListMatrixWithNullNotAllowed() {
    eval(" x<- list(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");

    eval(" x[[1,1]] <- NULL");
  }
  
  

  @Test(expected = EvalException.class)
  public void replaceSingleElementInPairListMatrixWithNullAllowed() {
    eval(" x<- pairlist(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");

    eval(" x[[1,1]] <- NULL");
    
    assertThat(eval("x[[1,1]]"), equalTo((SEXP) Null.INSTANCE));
    assertThat(eval("typeof(x)"), equalTo(c("pairlist")));
  }  
  
  @Test
  public void replaceSingleElementInMatrixByName() {
    eval(" x<- c(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");

    eval(" x[['foo']] <- 99 ");

    assertThat(eval("x"), equalTo(c(1, 2, 3, 4, 99)));

    // Dimensions should be dropped
    assertThat(eval("dim(x)"), equalTo((SEXP) Null.INSTANCE));
  }

  @Test
  public void replaceSingleElementInMatrixByIndex() {
    eval(" x<- c(1,2,3,4) ");
    eval(" dim(x) <- c(2,2) ");

    // Replacing an element should preserve dims
    eval(" x[1] <- 91 ");
    assertThat(eval("x"), equalTo(c(91, 2, 3, 4)));
    assertThat(eval("dim(x)"), equalTo(c_i(2, 2)));

    // Growing the vector through replacement should drop dims
    eval(" x[5] <- 99 ");

    assertThat(eval("x"), equalTo(c(91, 2, 3, 4, 99)));
    assertThat(eval("dim(x)"), equalTo((SEXP)Null.INSTANCE));
  }
  
  @Test
  public void replaceSingleElementOnNull() {
    eval(" x <- NULL ");
    eval(" x[[1]] <- 1:3 ");
    
    assertThat(eval("x"), equalTo(list(c_i(1,2,3))));
  }



  @Test
  public void replaceElementsInListWithNullRemovesElement() {
    eval(" x <- list(1,2,3) ");
    eval(" x[1:2] <- NULL ");

    assertThat( eval("x"), equalTo(list(3d)));
  }
  
  @Test
  public void replaceElementInEnv() {
   
  }


  @Test
  public void replaceElementInListWithNullRemovesElement() {
    eval(" x <- list(1,2,3) ");
    eval(" x[1] <- NULL ");

    assertThat( eval("x"), equalTo(list(2d, 3d)));
  }

  @Test
  public void setNewListElementByName() {
    eval(" p <- list( x = 22, y = 33 ) ");
    eval(" p$z <- 44 ");

    assertThat(eval(" p$x "), equalTo(c(22)));
    assertThat(eval(" p$y "), equalTo(c(33)));
    assertThat( eval(" p$z "), equalTo( c(44) ));
  }

  @Test
  public void partialListMatch() {
    eval(" x <- list(alligator=33, aardvark=44) ");

    assertThat(eval("x$a"), equalTo(NULL));
    assertThat( eval("x$all"), equalTo( c(33) ));
  }

  @Test
  public void exactMatch() {
    eval(" x <- list(a=1, aa=2) ");

    assertThat( eval(" x$a "), equalTo( c(1)));
  }

  @Test
  public void pairListPartial() {

    PairList list = PairList.Node.newBuilder()
        .add(symbol("alligator"), c(1))
        .add(symbol("aardvark"), c(3))
        .build();

    SEXP result = DollarFunction.fromPairList(list, "all");
    assertThat(result, equalTo((SEXP) c(1)));
  }

  @Test(expected = EvalException.class)
  public void listIndexOutOfBounds() {
    eval(" x <- list(1,2) ");
    eval(" x[[3]] ");
  }

  @Test
  public void assignListToListElement() {
    eval(" x<- list() ");
    eval(" x[['foo']] <- list(a=1,b=2,c=3)");

    assertThat(eval(" x[['foo']] "), equalTo(list(1d, 2d, 3d)));
    assertThat(eval(" names(x[['foo']]) "), equalTo(c("a", "b", "c")));

  }


  @Test
  public void indexOnNull() {
    eval(" x<- NULL ");
    assertThat(eval("x[[1]]"), equalTo(NULL));
  }
  
  @Test
  public void buildListByName() {
    eval(" x<-list()");
    eval(" x[['a']] <- 1");
    eval(" x[['b']] <- 2");
    eval(" x[['c']] <- 3");
    
    assertThat(eval("x"), equalTo(list(1d, 2d, 3d)));
    assertThat(eval("names(x)"), equalTo(c("a", "b", "c")));
    
  }

  @Test
  public void columnIndex() {
    eval(" x <- 1:8 ");
    eval(" dim(x) <- c(4,2) ");

    assertThat(eval("x[,2]"), equalTo(c_i(5, 6, 7, 8)));
    assertThat(eval("dim(x[,2])"), equalTo(NULL));
    assertThat(eval("dim(x[,2,drop=TRUE])"), equalTo(NULL));
    assertThat( eval("dim(x[,2,drop=FALSE])"), equalTo( c_i(4, 1) ));
  }

  @Test
  public void rows() {
    eval(" x <- 1:8 ");
    eval(" dim(x) <- c(4,2) ");

    assertThat(eval("x[3:4,]"), equalTo(c_i(3, 4, 7, 8)));
    assertThat( eval("dim(x[3:4,])"), equalTo( c_i(2, 2) ));
  }

  @Test
  public void byNamedCol() {
    eval(" x <- .Internal(rbind(1, c(a=1,b=2))) ");

    assertThat( eval(" x[,'b'] "), equalTo( c(2) ));
  }

  @Test
  public void arrayDimsCorrectlyPreserved() {
    eval(" x<- 1:8 ");
    eval(" dim(x) <- 8");

    assertThat(eval(" dim(x[1:4]) "), equalTo(c_i(4)));
    assertThat(eval(" dim(x[1])"), equalTo(NULL));
    assertThat( eval(" dim(x[1,drop=FALSE]) "), equalTo( c_i(1) ));
  }

  @Test
  public void matrixDimsPreserved() {
    eval(" x<-1:4 ");
    eval(" dim(x) <- c(2,2) ");
    eval(" x[1,1] <- 9");

    assertThat( eval("dim(x)"), equalTo( c_i(2, 2)));
  }
  
  @Test
  public void matrixDimsPreserved2() {
    eval(" x<-.Internal(rep.int(0,29*29)) ");
    eval(" dim(x) <- c(29,29) ");
    
    eval(" y<-c(134L,33L,2L,46L)");
    eval(" dim(y) <- c(2,2) ");
    
    eval(" x[c(1,2), c(3,4)] <- y");

    assertThat( eval("dim(x)"), equalTo( c_i(29, 29)));
  }
  
  @Test
  public void matrices() {
    eval(" x<-1:12");
    eval(" dim(x) <- c(3,4)");
    
    assertThat(eval("x[2,3]"), equalTo(c_i(8)));
    assertThat(eval("x[1,NULL]"), equalTo((SEXP) IntVector.EMPTY)); 
    assertThat( eval("dim(x[1,NULL])"), equalTo(NULL));
  }
    
  @Test
  public void matrixSubscriptWithZeros() {
    eval(" x<-1:12");
    eval(" dim(x) <- c(3,4)");
    
    assertThat(eval("x[0,0]"), equalTo(c_i()));
    assertThat(eval("dim(x[0,0])"), equalTo(c_i(0, 0)));

    assertThat(eval("x[0,1]"), equalTo(c_i()));
    assertThat(eval("dim(x[0,1])"), equalTo((SEXP) Null.INSTANCE));


    assertThat(eval("x[1,0]"), equalTo(c_i()));
    assertThat(eval("dim(x[1,0])"), equalTo((SEXP) Null.INSTANCE));
  }
  
  @Test(expected = EvalException.class)
  public void assigningNullThrowsAnError() {
    eval("x <- 1:12");
    
    eval("x[1:12] <- NULL");
  }
  
  @Test
  public void assigningNullIsOkWhenNothingIsSelectedForReplacement() {
    eval("x <- 1:3");
    
    eval("x[0] <- NULL");
    eval("x[c(0,0,0)] <- NULL");
    eval("x[FALSE] <- NULL");
    eval("x[NULL] <- NULL");
  }
  
  @Test
  public void replacingNull() {
    eval("x <- NULL");
    eval("x[5] <- 9");
    assertThat(eval("x"), equalTo(c(DoubleVector.NA, DoubleVector.NA, DoubleVector.NA, DoubleVector.NA, 9)));
  }
  
  @Test
  public void replaceNullWithLogical() {
    eval("x <- NULL");
    eval("x[TRUE] <- 3");
    
    assertThat(eval("x"), equalTo(c(3)));
  }
  
  @Test
  public void replaceNullWithCompleteSubscript() {
    eval("x <- NULL");
    eval("x[] <- 1:3");
    
    assertThat(eval("x"), equalTo(c_i()));
  }
  
  @Test
  public void replaceEmptyVectorWithCompleteSubscript() {
    eval("x <- logical(0)");
    eval("x[] <- 3");
    
    assertThat(eval("x"), equalTo((SEXP)DoubleVector.EMPTY));
  }
  
  @Test
  public void replaceEmptyVectorWithEmptyRhsDoesNotChangeType() {
    eval("x <- logical(0)");
    eval("x[] <- list()");
    
    assertThat(eval("typeof(x)"), equalTo(c("logical")));
  }
  
  @Test
  public void assigningToNull() {
    eval("x <- NULL");
    
    // should all have no effect
    eval("x[1] <- NULL");
    assertThat(eval("x"), equalTo((SEXP) Null.INSTANCE));
    
    eval("x[0] <- NULL");
    assertThat(eval("x"), equalTo((SEXP) Null.INSTANCE));

    eval("x[1:12] <- NULL");
    assertThat(eval("x"), equalTo((SEXP) Null.INSTANCE));
  }
  
  @Test
  public void subscriptsOnNull() {
    eval(" x <- NULL ");

    assertThat(eval(" x[1] "), equalTo(NULL));
    assertThat(eval(" x[c(TRUE,FALSE)] "), equalTo(NULL));
    assertThat(eval(" x[c(1,2,3)] "), equalTo(NULL));
    assertThat(eval(" x[-1] "), equalTo(NULL));
    assertThat( eval(" x[] "), equalTo( NULL ));
  }

  @Test
  public void integerIndex() {
    eval(" x<- FALSE ");
    assertThat( eval(" x[1L] "), equalTo( c(false)));
  }

  @Test
  public void replaceListItem() {
    eval(" x<- list(91, 'foo', NULL) ");
    eval(" x[[3]] <- 41 ");

    assertThat( eval("x"), equalTo( list(91d, "foo", 41d)) );
  }
  
  @Test
  public void replaceMatrixElements() {
    
    eval("x <- c(40, 1, 87, 6, 2, 8, 0, 28, 0, 43)");
    eval("dim(x) <- c(5,2)");
    
    eval("A <- .Internal(rep.int(0,9*9))");
    eval("dim(A) <- c(9,9)");
    
    eval("A[5:9,1:2] <- x");
    
    eval("print(A)");

    assertThat(eval("A[5,1]"), equalTo(c(40)));
    assertThat( eval("A[5,2]"), equalTo(c(8))); 
  }

  @Test
  public void replaceVectorItemWithWidening() {
    eval(" x<- c(91,92) ");
    eval(" x[[2]] <- 'foo' ");

    assertThat( eval("x"), equalTo( c("91", "foo")) );
  }

  @Test
  public void addNewListItemViaReplaceSingleItem() {
    eval(" x<-list() ");
    eval(" x[[1]] <- 'foo' ");

    assertThat( eval("x"), equalTo( list("foo"))) ;
  }
  
  @Test
  public void replaceSingleElementInEnvironment() {
    eval("x <- globalenv()");
    eval("x[['foo']] <- 42");
    
    assertThat(eval("foo"), equalTo(c(42)));
  }

  @Test
   public void addNewListItemByNameViaReplaceSingleItem() {
     eval(" x<- list() ");
     eval(" x[['foo']] <- 'bar'");

     assertThat(eval("x"), equalTo(list("bar")));
     assertThat( eval("names(x)"), equalTo( c("foo")));
   }


  @Test
  public void replaceColumn() {
    eval("     a<-1:30");
    eval(" dim(a) <- c(10,3) ");
    eval(" T<-TRUE");
    eval(" a[ c(T,T,T,T,T,T,T,T,T,T), 3] <- 51:60 ");

    assertThat( eval("a").length(), equalTo(30));

  }

  @Test
  public void pairListNotConverted() {
    eval(" p <- .Internal(as.vector(list(a=1, b=2, 3, 4), 'pairlist'))");
    assertThat( eval("p[1:2]"), equalTo(list(1d,2d)));
    assertThat(eval("names(p[TRUE])"), equalTo(c("a", "b", "", "")));
    assertThat(eval("p[['b']]"), equalTo(c(2)));
   
    eval("p[[1]]<-99");
    assertThat(eval(".Internal(typeof(p))"), equalTo(c("pairlist")));
    assertThat( eval("p$a"), equalTo(c(99)));
   
  }

  @Test
  public void pairListSingleByName() {
    eval(" p <- .Internal(as.vector(list(hello=1, b=2, 3, 4), 'pairlist'))");

    assertThat(eval("p[['h']]"), equalTo(NULL));
    assertThat(eval("p[['hello']]"), equalTo(c(1)));
    assertThat( eval("p[['h', exact=FALSE]]"), equalTo(c(1)));
  }
  
  @Test
  public void pairListReplaceByName() {
    eval(" x <- .Internal(as.vector(list(a=1, z=4), 'pairlist'))");
    eval(" x$b<-2");
    eval(" x$a<-4");
    eval(" x$z<-NULL");
    
    assertThat(eval("length(x)"), equalTo(c_i(2)));
    assertThat( eval("x$a"), equalTo(c(4)));
    assertThat( eval("x$b"), equalTo(c(2)));

  }
  
  @Test
  public void subsettingFunctionCalls() {
    eval("x <- quote(a+b)");
    assertThat(eval("x[]"), equalTo(eval("x")));
    assertThat(eval("x[1]"), equalTo((SEXP) FunctionCall.newCall(Symbol.get("+"))));
    assertThat(eval("x[1:2]"), equalTo((SEXP) FunctionCall.newCall(Symbol.get("+"), Symbol.get("a"))));
  }
  
  @Test
  public void subsettingFunctionCallsByName() {
    eval("x <- quote(c(a=1,b=2))");
    assertThat(eval("x['a']"), equalTo((SEXP) FunctionCall.newCall(new DoubleArrayVector(1))));
    assertThat(eval("names(x['a'])"), equalTo(c("a")));

    assertThat(eval("x['foo']"), equalTo((SEXP) FunctionCall.newCall(Null.INSTANCE)));
    assertThat(eval("x[ NA_character_ ]"), equalTo((SEXP) FunctionCall.newCall(Null.INSTANCE)));

  }
  
  
  @Test
  public void emptyLogicalIndex() {
    eval(" x <- 1:12 ");
    eval(" dim(x) <- 3:4 ");
    eval(" y <- x[ c(), , drop=FALSE] ");
    assertThat(eval(".Internal(typeof(y))"), equalTo(c("integer")));
    assertThat(eval("dim(y)"), equalTo(c_i(0, 4)));
    
  }
  
  @Test
  public void pairListElipses() {
    eval(" x <- .Internal(as.vector(list(a=1, z=4), 'pairlist'))");
    eval(" x$... <- 4");
    assertThat( eval("x$..."), equalTo(c(4)));
  }

  @Test
  public void indexingCharacter() {
    eval("vars <- quote(list(weighta))");
    assertThat(eval("vars[[2]]"), Matchers.equalTo(symbol("weighta")));
  }

  @Test
  public void assignSymbol() {
    eval("k<-list(1,2,3)");
    eval("k[[2]]<-quote(foo)");
  }
  
  @Test
  public void coordinateMatrices() {
    
    eval("x<-1:12");
    eval("dim(x) <- c(3,4) ");
    
    // define a matrix with coordinates in rows
    // 1 3
    // 3 4
    eval("coords <- c(1,3,3,4)");
    eval("dim(coords) <- c(2,2)");
    
    assertThat(eval("x[coords]"), equalTo(c_i(7, 12)));
    
    // logical matrices should NEVER be treated as coordinate
    // matrices, regardless of their dimension
    assertThat(eval("x[coords == 1]"), equalTo(c_i(1, 5, 9)));
  }
  
  @Test(expected = EvalException.class)
  public void coordinateMatricesCannotBeUsedInSingleSelect() {
    eval("x<-1:12");
    eval("dim(x) <- c(3,4) ");

    // define a matrix with coordinates in ONE rows
    // 1 3
    eval("coords <- c(1,3,3,4)");
    eval("dim(coords) <- c(2,2)");
    
    // You might think this would select a single element, 
    // but coordinate matrices are not accepted in single select mode
    // so the following should throw an error
    eval("print(x[[coords]])");
    
  }
  
  @Test
  public void environmentSymbol() throws IOException{
    assumingBasePackagesLoad();
    
    eval(".testEnv<-new.env()");
    eval("assign(\"key\",1,.testEnv)");
    eval("assign(\"value\",\"foo\",.testEnv)");
    assertThat(eval("if(.testEnv[[\"key\"]]==1) TRUE else FALSE"), logicalVectorOf(Logical.TRUE));
    assertThat(eval("if(.testEnv[[\"value\"]]==\"foo\") TRUE else FALSE"), logicalVectorOf(Logical.TRUE));
  }
  

  @Test
  public void emptyLogical() {
    eval("x <- 1:10");
    eval("emptyLogical <- TRUE[-1]");
    assertThat(eval("x[emptyLogical]"), equalTo((SEXP) IntVector.EMPTY));
  }
  
  @Test
  public void dimNamesToNamesWhenDrop() {
    eval("x <- 1:12");
    eval("dim(x) <- c(3,4)");
    eval("dimnames(x) <- list(c('A','B','C'), NULL)");
    
    eval("y <- x[,1L]");
    assertThat(eval("dim(y)"), equalTo((SEXP) Null.INSTANCE));
    assertThat(eval("names(y)"), equalTo(c("A", "B", "C")));
    
  }

  @Test(expected = EvalException.class)
  public void absetDimNamesThrowsEvalException() {
    eval("x <- c(1,2,3,4)");
    eval("dim(x) <- c(1,4)");
    eval("dimnames(x) <- list(NULL, c('a','b','c','d'))");
    eval("y <- x[,c('a','b', 'x')]");
    assertThat(eval("y"), equalTo(c(1, 2)));

  }

  @Test
  public void subsetLang() {
    eval("formula <- y ~ x");
    assertThat(eval("print(formula[[2]])"), equalTo(symbol("y")));
  }
  
  @Test
  public void matrixColumn() {
    eval("`[.foo` <- function(x,i,j) { class(x) <- 'matrix'; NextMethod('[') }");
    
    eval("x <- as.double(1:12)");
    eval("dim(x) <- c(6,2)");
    eval("class(x) <- 'foo'");
    
    assertThat(eval("x[,2]"), equalTo(c(7, 8, 9, 10, 11, 12)));
  }
  
  @Test
  public void dollarAssignToNull() {
    eval("x <- NULL");
    eval("x$a <- 41");
    
    assertThat(eval("x"), equalTo(list(41d)));
    assertThat(eval("names(x)"), equalTo(c("a")));

  }
  
  @Test
  public void dollarAssignToAtomicIsCoercedToList() {
    eval("x <- c(a=91,b=92)");
    eval("x$a <- 99");
    assertThat(eval("x"), equalTo(list(99d, 92d)));
    assertThat(eval("names(x)"), equalTo(c("a", "b")));
  }

  @Test
  public void outOfBounds() {
    eval("x <- c(X=1,a=2)");
    eval("x[c('a','X','a','b')] <- list(3,TRUE,FALSE)");
  }
  
  @Test
  public void genericDollar() {
    eval("`$.foo` <- function(obj, name) name");
    eval("x <- 1");
    eval("class(x) <- 'foo'");
    assertThat(eval("x$bar"), equalTo(c("bar")));
  }
  
  @Test
  public void deferredDoubleMask() {
    
    eval("x <- as.double(1:1e6)");
    eval("x[c(TRUE,FALSE)] <- 99");
    
    eval("print(sum(x))");
  }
  
  @Test
  public void replacePairListElementNameByName() {
    eval("x <- pairlist(a=1,b=2,c=3)");
    eval("x[['a']] <- 99");
    eval("stopifnot(identical(x, pairlist(a=99,b=2,c=3)))");
  }
  
  @Test
  public void replacePairListWithNullRemovesElement() {
    eval("x <- pairlist(a=1,b=2,3)");
    eval("x[['a']] <- NULL");
    eval("stopifnot(identical(x, pairlist(b=2,3)))");
    
    eval("x[['foo']] <- NULL");
    eval("stopifnot(identical(x, pairlist(b=2,3)))");
  }

  @Test
  public void replacePairListWithNewElement() {
    eval("x <- pairlist(a=1,b=2,3)");
    eval("x[['foo']] <- list(99,98)");
    eval("stopifnot(identical(x, pairlist(a=1,b=2,3,foo=list(99,98))))");
  }
  
  @Test
  public void expressionVector() {
    eval("x <- parse(text='a; b; c;')");
    
    assertThat(eval("typeof(x)"), equalTo(c("expression")));
    assertThat(eval("typeof(x[1:2])"), equalTo(c("expression")));
  
    eval("x[[1]] <- quote(x)");
    assertThat(eval("typeof(x)"), equalTo(c("expression")));
  
  }
  
  @Test
  public void matrixSelectionFromList() {
    eval("x <- list(1,2,3,4)");
    eval("dim(x) <- c(2,2)");
    
    assertThat(eval("x[[1,2]]"), equalTo(c(3)));
  }
  
  @Test
  public void matrixSelectionFromVector() {
    eval("x <- c(1,2,3,4)");
    eval("dim(x) <- c(2,2)");

    assertThat(eval("x[[1,2]]"), equalTo(c(3)));
  }
  
  @Test(expected = EvalException.class)
  public void matrixSingleSelectionWithZeroIsError() {
    eval("x <- c(1,2,3,4)");
    eval("dim(x) <- c(2,2)");

    eval("x[[1,0]]");
  }
  
  @Test(expected = EvalException.class)
  public void matrixSingleSelectionWithNAIsError() {
    eval("x <- c(1,2,3,4)");
    eval("dim(x) <- c(2,2)");

    eval("x[[1,NA]]");
  }
  
  @Test
  public void matrixSelectionWithNA() {
    eval("x <- 1:12");
    eval("dim(x) <- c(3,4)");
    
    assertThat(eval("x[c(1,NA), 4]"), equalTo(c_i(10, IntVector.NA)));
  }

  @Test
  public void matrixSelectionWithFirstNA() {
    eval("x <- 1:12");
    eval("dim(x) <- c(3,4)");

    assertThat(eval("x[c(NA), c(3:4)]"), equalTo(c_i(
        IntVector.NA, IntVector.NA,
        IntVector.NA, IntVector.NA,
        IntVector.NA, IntVector.NA)));
  }
  
  @Test
  public void singleMatrixSelectionByName() {
    eval("x <- 1:6");
    eval("dim(x) <- 2:3");
    eval("rownames(x) <- c('a','b')");
    
    assertThat(eval("x[['a', 3]]"), equalTo(c_i(5)));
  }

  @Test(expected = EvalException.class)
  public void singleMatrixSelectionByNaNameIsError() {
    eval("x <- 1:6");
    eval("dim(x) <- 2:3");
    eval("rownames(x) <- c('a','b')");

    eval("print(x[[NA_character_, 3]])");
  }
  
  @Test
  public void replaceMatrixElementsInList() {
    eval("x <- list(1,2,3,4,5,6)");
    eval("dim(x) <- c(2,3) ");
    eval("x[,1] <- 91:92");
    
    assertThat(eval("x"), equalTo(list(91, 92, 3d, 4d, 5d, 6d)));
  }
  
  @Test
  public void replaceSingleMatrixListElement() {
    eval("b <- c(1/0,-3/0,0/0,4)");
    eval("dim(b) <- c(2,1,2)");
    eval("b[[2,1,1]] <- list(111)");

    assertThat(eval("typeof(b)"), equalTo(c("list")));
    assertThat(eval("typeof(b[[2]])"), equalTo(c("list")));
  }
  
  @Test
  public void replaceListElementsWithNullUsingLogicalSubscripts() {
    eval("x <- list(1,2)");
    eval("x[c(TRUE, FALSE, TRUE, TRUE)] <- NULL");
    
    assertThat(eval("x"), equalTo(list(2d)));
  }
  
  @Test
  public void dropListElementWithNull() {
    eval("x <- list(a=1,b=2)");
    eval("x['a'] <- NULL");

    assertThat(eval("x"), equalTo(list(2d)));
  }
  
  @Test
  public void dimNamesPreservedOnArrays() {
    eval("x <- array(NA, 3)");
    eval("names(x) <- letters[1:3]");
    eval("x[1] <- 3");
    
    assertThat(eval("dimnames(x)[[1]]"), equalTo(c("a", "b", "c")));
    
  }
  
  @Test
  public void replaceWithCoordinateMatrix() {
    eval("x <- matrix(0, ncol=2, nrow=3)");
    eval("i <- rbind(c(1,2),c(3,2))");
    eval("x[i] <- c(4, 9)");
    
    assertThat(eval("x"), equalTo(c(0, 0, 0, 4, 0, 9)));
  }

  @Test(expected = EvalException.class)
  public void emptyReplacementWithCoordinateMatrix() {
    eval("x <- matrix(0, ncol=2, nrow=3)");
    eval("i <- rbind(c(1,2),c(3,2))");
    eval("x[i] <- numeric(0)");

    assertThat(eval("x"), equalTo(c(0, 0, 0, 4, 0, 9)));
  }
  

  @Test(expected = EvalException.class)
  public void matrixReplacementOnlyInBounds() {
    eval("m <- matrix(1:12, nrow=3)");
    eval("m[99, 99] <- 4");
  }


  @Test(expected = EvalException.class)
  public void matrixExtractionOnlyInBounds() {
    eval("m <- matrix(1:12, nrow=3)");
    eval("m[99, 99]");
  }

  @Test
  public void extendArray() {
    eval("a <- 1:12");
    eval("dim(a) <- 12");
    
    eval("a[13] <- 99");
    
    assertThat(eval("dim(a)"), equalTo(NULL));
  }
  
  @Test
  public void arrayDimPreserved() {
    eval("a <- 1:3");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");
    
    assertThat(eval("dimnames(a)[[1]]"), equalTo(c("a", "b", "c")));
    assertThat(eval("names(a)"), equalTo(c("a", "b", "c")));
    
    assertThat(eval("dim(a[1:2])"), equalTo(c_i(2)));
    assertThat(eval("dimnames(a[1:2])[[1]]"), equalTo(c("a", "b")));

    // Dim attributes ARE dropped if the result is of length 1
    assertThat(eval("dim(a[1])"), equalTo(NULL));
    assertThat(eval("dimnames(a[1])"), equalTo(NULL));
    assertThat(eval("names(a[1])"), equalTo(c("a")));

    // AND if the length 0 
    assertThat(eval("dim(a[0])"), equalTo(NULL));
    assertThat(eval("dimnames(a[0])"), equalTo(NULL));
    assertThat(eval("names(a[0])"), equalTo((SEXP)StringArrayVector.EMPTY));

    assertThat(eval("dim(a[100:101])"), equalTo(c_i(2)));
    assertThat(eval("dimnames(a[100:101])[[1]]"), equalTo((SEXP)new StringArrayVector(StringVector.NA, StringVector.NA)));
  }

  @Test
  public void arrayDimPreservedOnSelectByName() {
    eval("a <- 1:3");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");

    assertThat(eval("dim(a[c('a', 'b')])"), equalTo(c_i(2)));
    assertThat(eval("dimnames(a[c('a', 'b')])[[1]]"), equalTo(c("a", "b")));

    // Dim attributes ARE dropped if the result is of length 1
    assertThat(eval("dim(a['a'])"), equalTo(NULL));
    assertThat(eval("dimnames(a['a'])"), equalTo(NULL));
    assertThat(eval("names(a['a'])"), equalTo(c("a")));

    // AND if the length 0 
    assertThat(eval("dim(a['zz'])"), equalTo(NULL));
    assertThat(eval("dimnames(a['zz'])"), equalTo(NULL));
    assertThat(eval("names(a['zz'])"), equalTo(c(StringVector.NA)));
    
    // unless drop = FALSE
    assertThat(eval("dim(a['zz', drop = FALSE])"), equalTo(c_i(1)));
    assertThat(eval("dim(a['a', drop = FALSE])"), equalTo(c_i(1)));

  }
  
  @Test
  public void arrayDimPreservedOnLogicalSelection() {
    eval("a <- 1:3");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");
    
    assertThat(eval("dim(a[TRUE])"), equalTo(c_i(3)));
    assertThat(eval("dimnames(a[c(TRUE, FALSE, TRUE)])[[1]]"), equalTo(c("a", "c")));

    assertThat(eval("dim(a[c(TRUE, FALSE, FALSE)])"), equalTo(NULL));
    assertThat(eval("names(a[c(TRUE, FALSE, FALSE)])"), equalTo(c("a")));

    assertThat(eval("dim(a[c(TRUE, FALSE, FALSE, TRUE)])"), equalTo(c_i(2)));
    assertThat(eval("dimnames(a[c(TRUE, FALSE, FALSE, TRUE)])[[1]]"), equalTo(c("a", StringVector.NA)));
  }
  
  @Test
  public void arrayDimDroppedOnLogicalExtendedReplacement() {
    eval("a <- 1:3");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");

    eval("a[c(TRUE, FALSE, TRUE, TRUE, FALSE, NA)] <- 99");
    
    assertThat(eval("dim(a)"), equalTo(NULL));
    assertThat(eval("names(a)"), equalTo(c("a", "b", "c", "", "", "")));
  }

  @Test(expected = EvalException.class)
  public void extendLogicalSelectNotAllowedInMatrixSelection() {
    eval("m <- matrix(1:12, nrow=3)");
    eval("dim(m) <- 3:4");
    
    eval("m[c(TRUE,TRUE,TRUE, TRUE), ]");
  }

  @Test
  public void arrayDimDroppedOnReplace() {
    eval("a <- 1:3");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");

    eval("replace <- function(a, i, x) { a[i] <- x; a; }");
    
    // If the array is not extended, then we keep the dim attributes
    assertThat(eval("dim(replace(a, 1, 99))"), equalTo(c_i(3)));
    
    // If the array gets extended, then drop the dim attribute,
    // but keep the dimnames if any
    assertThat(eval("names(replace(a, 4, 98))"), equalTo(c("a", "b", "c", "")));
  }
  
  
  @Test
  public void listArrayDimDroppedOnSingleAssignNull() {
    
    eval("a <- list(1,2,3)");
    eval("dim(a) <- 3");
    eval("names(a) <- letters[1:3]");
    
    eval("a['z'] <- NULL");
    
    assertThat(eval("dim(a)"), equalTo(NULL));
    assertThat(eval("dimnames(a)"), equalTo(NULL));
    assertThat(eval("names(a)"), equalTo(c("a", "b", "c")));
  }

  @Test
  public void unnamedlistArrayDimDroppedOnSingleAssignNull() {

    eval("a <- list(1,2,3)");
    eval("dim(a) <- 3");

    eval("a['z'] <- NULL");

    assertThat(eval("dim(a)"), equalTo(NULL));
    assertThat(eval("dimnames(a)"), equalTo(NULL));
    assertThat(eval("names(a)"), equalTo(NULL));
  }
  
  @Test
  public void dollarSignOnUnnamedList() {
    eval("x <- c(1,2,3)");
    eval("x$foo <- 99");
  }
}