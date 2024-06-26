/*	Copyright 2011 Brown University -- Steven P. Reiss		*/

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

package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.exec.IvyExec;

import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.junit.Test;


public class BoardTest {


public BoardTest() { }



@Test
public void testStartup()
{
   try {
      new IvyExec("'C:\\Program Files\\Eclipse\\eclipse' -data 'C:\\Program Files\\Eclipse'");
      // ex.waitFor();
    }
   catch (IOException e) {
      System.err.println("PROBLEM RUNNING: " + e);
    }

   String msg = "mailto:?subject=sub&body=";
   String body = "test message\r\n";
   body += "--------------060900050201030405060805\n";
   body += "Content-Type: text/xml;\n";
   body += "Content-Transfer-Encoding: 7bit\n";
   body += "Content-Disposition: inline;\r\n";
   body += "\r\n";
   body += "<XMLTEST/>\r\n";
   body += "\r\n";
   body += "--------------060900050201030405060805--\r\n";

   try {
      body = URLEncoder.encode(body,"UTF-8");
      body = body.replace("+","%20");
    }
   catch (Throwable t) {
      System.err.println("PROBLEM ENCODING: " + t);
    }

   msg += body;

   try {
      URI u = new URI(msg);
      Desktop.getDesktop().mail(u);
    }
   catch (Throwable t) {
      System.err.println("PROBLEM SENDING: " + t);
    }

   System.err.println("CHECK " + Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));

   try {
      Desktop.getDesktop().browse(new URI("http://conifer2.cs.brown.edu/s6"));
    }
   catch (Throwable t) {
      System.err.println("PROBLEM OPENING: " + t);
      t.printStackTrace();
    }
}


@Test
public void testInvertColors()
{
   Color rc1 = BoardColors.invertColorW3(new Color(255,255,128));
   System.err.println("RESULT IS " + rc1);
   Color rc2 = BoardColors.invertColorW3(Color.RED);
   System.err.println("RESULT IS " + rc2);
   Color rc3 = BoardColors.invertColorW3(Color.GREEN);
   System.err.println("RESULT IS " + rc3);
   Color rc4 = BoardColors.invertColorW3(Color.BLUE);
   System.err.println("RESULT IS " + rc4);
   Color rc5 = BoardColors.invertColorW3(Color.YELLOW);
   System.err.println("RESULT IS " + rc5);
   Color rc6 = BoardColors.invertColorW3(new Color(64,0,0));
   System.err.println("RESULT IS " + rc6);
   
}




}	// end of BoardTest
