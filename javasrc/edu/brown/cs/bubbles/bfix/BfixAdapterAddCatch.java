/********************************************************************************/
/*                                                                              */
/*              BfixAdapterAddCatch.java                                        */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




class BfixAdapterAddCatch extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Pattern          empty_throw_pattern;
private static Pattern          missing_catch_pattern;

static {
   empty_throw_pattern = Pattern.compile("Syntax error. insert \"Finally\" to complete TryStatement");
   missing_catch_pattern = Pattern.compile("Unhandled exception type ([A-Za-z0-9_$.]+)");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixAdapterAddCatch()
{
   super("Insert Catch Clauses");
}



/********************************************************************************/
/*                                                                              */
/*      Abstract method implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslt)
{
   String ctype = getCatchType(corr,bp);
   if (ctype == null) return;
   
   CatchFixer fixer = new CatchFixer(corr,bp,ctype);
   rslt.add(fixer);
}




@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String ctyp = getCatchType(corr,bp);
   if (ctyp != null && !ctyp.equals("*")) return "Add Catch";
   return null;
}



private String getCatchType(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   Matcher m = empty_throw_pattern.matcher(bp.getMessage());
   if (m.find()) return "*";
   
   m = missing_catch_pattern.matcher(bp.getMessage());
   if (!m.matches()) return null;
   
   String rtyp = m.group(1);
   
   return rtyp;
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private static BaleWindowElement findTryStatement(BaleWindowDocument doc,int soff)
{
   BaleWindowElement elt = doc.getCharacterElement(soff);
   BaleWindowElement pelt = elt;
   while (pelt != null) {
      if (pelt.getName().equals("SplitStatement")) {
         BaleWindowElement celt = doc.getCharacterElement(pelt.getStartOffset());
         while (celt.getName().equals("Indent")) {
            celt = celt.getNextCharacterElement();
          }
         if (celt.getName().equals("Keyword")) {
            BaleTokenType tt = celt.getTokenType();
            if (tt == BaleTokenType.TRY) break;
          }
       }
      pelt = pelt.getBaleParent();
    }
   
   return pelt;
}




/********************************************************************************/
/*                                                                              */
/*      Fixing code                                                             */
/*                                                                              */
/********************************************************************************/

private static class CatchFixer extends BfixFixer {
   
   private String for_type;
   private BaleWindowDocument for_document;
   private long initial_time;
   
   CatchFixer(BfixCorrector corr,BumpProblem bp,String typ) {
      super(corr,bp);
      for_type = typ;
      if (typ != null && typ.equals("*")) for_type = null;
      for_document = corr.getEditor().getWindowDocument();
      initial_time = corr.getStartTime();
    }
   
   @Override protected RunnableFix findFix() {
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement pelt = findTryStatement(for_document,soff);
      if (pelt == null) return null;
      if (for_type == null) {
         findTypeForCatch(pelt);
         if (for_type == null) return null;
       }
      if (!checkCatchInsert(pelt)) return null;
      if (for_corrector.getStartTime() != initial_time) return null;
      BoardLog.logD("BFIX","CATCH " + for_type);
      BoardMetrics.noteCommand("BFIX","CATCHFIX");
      CatchDoer cd = new CatchDoer(for_corrector,for_document,for_problem,pelt,for_type,initial_time);
      return cd; 
    }
   
   private void findTypeForCatch(BaleWindowElement pelt) {
      BumpClient bc = BumpClient.getBump();
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      String pid = bc.createPrivateBuffer(proj, filename, null);
      int foff = pelt.getStartOffset();
      int eoff = pelt.getEndOffset();
      try {
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
         if (probs == null) return;
         if (!checkProblemPresent(for_problem,probs)) return;
         int inspos = for_document.mapOffsetToEclipse(eoff);
         int spos = for_document.mapOffsetToEclipse(foff);
         bc.beginPrivateEdit(filename, pid);
         bc.editPrivateFile(proj, file, pid, inspos, inspos, " finally { }");
         probs = bc.getPrivateProblems(filename,pid);
         for (BumpProblem bp : probs) {
            if (bp.getStart() >= spos && bp.getStart() <= inspos) {
               Matcher m = missing_catch_pattern.matcher(bp.getMessage());
               if (m.matches()) {
                  for_type = m.group(1);
                  return;
                }
             }
          }
       }
      finally {
         bc.removePrivateBuffer(proj, filename, pid);
       }
    }
   
   private boolean checkCatchInsert(BaleWindowElement pelt) {
      BumpClient bc = BumpClient.getBump();
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      String pid = bc.createPrivateBuffer(proj, filename, null);
      int foff = pelt.getStartOffset();
      int eoff = pelt.getEndOffset();
      String insert = " catch (" + for_type + " ___x___) { }";
      try {
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
         if (probs == null) return false;
         int probct = getErrorCount(probs);
         if (!checkProblemPresent(for_problem,probs)) return false;
         int inspos = for_document.mapOffsetToEclipse(eoff);
         bc.beginPrivateEdit(filename, pid);
         bc.editPrivateFile(proj,file,pid,inspos,inspos,insert);
         probs = bc.getPrivateProblems(filename,pid);
         if (probs == null || getErrorCount(probs) > probct) return false;
         if (checkAnyProblemPresent(for_problem,probs,foff,eoff)) return false;
         return true;
       }
      finally {
         bc.removePrivateBuffer(proj,filename,pid);
       }
    }
   
}       // end of inner class CatchFixer




/********************************************************************************/
/*                                                                              */
/*      Code to actually add the catch statement                                */
/*                                                                              */
/********************************************************************************/

private static class CatchDoer implements RunnableFix {
   
   private BfixCorrector for_corrector;
   private BaleWindowDocument for_document;
   private BumpProblem for_problem;
   private String catch_class;
   private long initial_time;
   
   CatchDoer(BfixCorrector corr,BaleWindowDocument doc,BumpProblem bp,
         BaleWindowElement stmt,String cls,long time) {
      for_corrector = corr;
      for_document = doc;
      for_problem = bp;
      catch_class = cls;
      initial_time = time;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (for_corrector.getStartTime() != initial_time) return;
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement trystmt = findTryStatement(for_document,soff);
      if (trystmt == null) return;
      int eoff = trystmt.getEndOffset();
      if (!checkSafePosition(for_corrector,eoff-1,eoff+1)) return;
      
      // might want to find better insertion point here
      String insert = "catch (" + catch_class + " _ex) {\n}\n";
      BoardMetrics.noteCommand("BFIX","AddCatch_" + for_corrector.getBubbleId());
      for_document.replace(eoff,0,insert,true,true);
      BoardMetrics.noteCommand("BFIX","DoneAddCatch_" + for_corrector.getBubbleId());
    }

   @Override public double getPriority()                        { return 0; }
   
}       // end of inner class CatchDoer





}       // end of class BfixAdapterAddCatch




/* end of BfixAdapterAddCatch.java */

