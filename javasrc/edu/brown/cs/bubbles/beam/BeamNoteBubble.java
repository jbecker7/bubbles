/********************************************************************************/
/*										*/
/*		BeamNoteBubble.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items note bubble		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.swing.SwingColorButton;
import edu.brown.cs.ivy.swing.SwingColorRangeChooser;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTMLEditorKit;



import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BeamNoteBubble extends BudaBubble implements BeamConstants,
	BudaConstants.BudaBubbleOutputer, BudaConstants.Scalable
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum HtmlState {
  TEXT, TAG, STRING1, STRING2, LINE
}


private NoteArea		note_area;
private String			note_name;
private BeamNoteAnnotation	note_annot;
private Color			top_color;
private Color			bottom_color;

private static BoardProperties	beam_properties = BoardProperties.getProperties("Beam");

private static Map<String,Document> file_documents;


private static final String	MENU_KEY = "menu";
private static final String	menu_keyname;
private static SimpleDateFormat file_dateformat = new SimpleDateFormat("yyMMddHHmmss");

private static final String COLOR_OUTPUT = "<!-- COLORS: TOP=%1 BOTTOM=%2 -->";
private static final String COLOR_PATTERN_S = "^<!-- COLORS: TOP=(\\p{XDigit}+) BOTTOM=(\\p{XDigit}+) -->$";
private static final Pattern COLOR_PATTERN = Pattern.compile(COLOR_PATTERN_S,Pattern.MULTILINE);


private static Color		default_top_color;
private static Color		default_bottom_color;


static {
   int mask = SwingText.getMenuShortcutKeyMaskEx();
   if (mask == InputEvent.META_DOWN_MASK)
      menu_keyname = "meta";
   else if (mask == InputEvent.CTRL_DOWN_MASK)
      menu_keyname = "ctrl";
   else menu_keyname = "ctrl";

   file_documents = new HashMap<String,Document>();

   default_top_color = BoardColors.getColor(NOTE_TOP_COLOR_PROP);
   default_bottom_color = BoardColors.getColor(NOTE_BOTTOM_COLOR_PROP,default_top_color);
   if (default_bottom_color.getRGB() == default_top_color.getRGB())
      default_bottom_color = default_top_color;
}




private static final long serialVersionUID = 1;
private static final Pattern temp_name = Pattern.compile("Note_\\d{12}_\\d{1,4}.html");




/********************************************************************************/
/*										*/
/*	Keymap for notes							*/
/*										*/
/********************************************************************************/

private static Keymap		note_keymap;

private static final KeyItem [] key_defs = new KeyItem[] {

   new KeyItem("ctrl B","font-bold"),
      new KeyItem("meta I","font-italic"),
      new KeyItem("meta U","font-underline"),
      new KeyItem("meta MINUS","font-strikethrough"),
      new KeyItem("ctrl 1","font-size-10"),
      new KeyItem("ctrl 2","font-size-12"),
      new KeyItem("ctrl 3","font-size-14"),
      new KeyItem("ctrl 4","font-size-18"),
      new KeyItem("ctrl 5","font-size-24"),
      new KeyItem("ctrl shift 1","foreground-black"),
      new KeyItem("ctrl shift 2","foreground-red"),
      new KeyItem("ctrl shift 3","foreground-green"),
      new KeyItem("ctrl shift 4","foreground-blue"),
      new KeyItem("ctrl shift 5","left-justify"),
      new KeyItem("ctrl shift 6","center-justify"),
      new KeyItem("ctrl shift 7","right-justify"),
      new KeyItem("ctrl shift 8","text-justify"),
      new KeyItem("menu Z","Undo"),
      new KeyItem("menu Y","Redo"),
      new KeyItem("menu S","Save")
};


private static Action [] default_actions = new Action [] {
      new StrikeThruAction(),
      new NoteEditorKit.NoteColorAction("foreground-black",Color.BLACK),
      new NoteEditorKit.NoteColorAction("foreground-red",Color.RED),
      new NoteEditorKit.NoteColorAction("foreground-green",Color.GREEN),
      new NoteEditorKit.NoteColorAction("foreground-blue",Color.BLUE),
      new NoteEditorKit.NoteFontSizeAction("font-size-10",10),
      new NoteEditorKit.NoteFontSizeAction("font-size-12",12),
      new NoteEditorKit.NoteFontSizeAction("font-size-14",14),
      new NoteEditorKit.NoteFontSizeAction("font-size-18",18),
      new NoteEditorKit.NoteFontSizeAction("font-size-24",24),
      new NoteEditorKit.AlignmentAction("text-justify",StyleConstants.ALIGN_JUSTIFIED),
      BurpHistory.getUndoAction(),
      BurpHistory.getRedoAction(),
      new SaveAction(),
};


static {
   Keymap dflt = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
   SwingText.fixKeyBindings(dflt);
   note_keymap = JTextComponent.addKeymap("NOTE",dflt);
   NoteEditorKit nek = new NoteEditorKit();
   Action [] noteacts = nek.getActions();
   for (KeyItem ka : key_defs) {
      ka.addToKeyMap(note_keymap,noteacts);
    }
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamNoteBubble()
{
   this(null,null,null);
}


BeamNoteBubble(String name,String cnts,BeamNoteAnnotation annot)
{
   super(null,BudaBorder.RECTANGLE);

   top_color = default_top_color;
   bottom_color = default_bottom_color;
   if (bottom_color.getRGB() == top_color.getRGB()) bottom_color = top_color;

   Document d = null;
   if (name != null) d = file_documents.get(name);

   if (name != null) note_name = name;
   else createFileName();

   note_area = null;
   if (d != null) {
      note_area = new NoteArea(d);
      file_documents.put(note_name,note_area.getDocument());
    }
   else {
      note_area = new NoteArea(cnts);
      loadNote(false);
      file_documents.put(note_name,note_area.getDocument());
    }

   if (annot != null && annot.getDocumentOffset() < 0) annot = null;
   note_annot = annot;
   if (annot != null) {
      BaleFactory.getFactory().addAnnotation(annot);
      annot.setAnnotationFile(note_name);
    }

   addComponentListener(new ComponentHandler());

   // if contents are null, then set the header part of the html with information about
   // the source of this bubble, date, dlm, title, etc.

   JScrollPane jsp = new JScrollPane(note_area);

   setContentPane(jsp,note_area);
}



@Override protected void localDispose()
{
   if (note_name != null) {
      if (note_area.getText().length() == 0 || note_annot == null) {
	 if (!isUserName()) deleteNote();
       }
      note_annot = null;
      note_name = null;
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public BudaContentNameType getContentType()
{
   return BudaContentNameType.NONE;
}


@Override public File getContentFile()
{
   return getNoteFile();
}


@Override public String getContentName()
{
   return note_name;
}



/********************************************************************************/
/*										*/
/*	Saved Note Creation							*/
/*										*/
/********************************************************************************/

static BudaBubble createSavedNoteBubble()
{
   List<String> names = getSavedNoteNames();

   Object [] namearr = names.toArray();

   Object sel = JOptionPane.showInputDialog(null,"Choose Saved Note",
	 "Saved Note Selector",JOptionPane.QUESTION_MESSAGE,null,
	 namearr,null);
   if (sel == null) return null;

   String name = sel.toString() + ".html";
   BeamNoteBubble bbl = new BeamNoteBubble(name,null,null);

   return bbl;
}



/********************************************************************************/
/*										*/
/*	Set default note color							*/
/*										*/
/********************************************************************************/

static void setDefaultNoteColor(BudaBubbleArea bba)
{
   SwingGridPanel pnl = new SwingGridPanel();
   SwingColorRangeChooser crc = null;
   SwingColorButton cc = null;
   if (default_top_color == default_bottom_color) {
      cc = pnl.addColorField("Default Note Color",default_top_color,null);
    }
   else {
      crc = pnl.addColorRangeField("Default Note Color",default_top_color,default_bottom_color,null);
    }
   JCheckBox cbx = pnl.addBoolean("Make this Permanent",false,null);
   int sts = JOptionPane.showOptionDialog(bba,pnl,"Choose Note Default Color",JOptionPane.OK_CANCEL_OPTION,
	 JOptionPane.QUESTION_MESSAGE,null,null,null);
   if (sts == JOptionPane.OK_OPTION) {
      if (crc !=  null) {
	 default_top_color = crc.getFirstColor();
	 default_bottom_color = crc.getSecondColor();
	 if (default_bottom_color.getRGB() == default_top_color.getRGB())
	    default_bottom_color = default_top_color;
       }
      else {
	 default_top_color = cc.getColor();
	 default_bottom_color = default_top_color;
       }
      if (cbx.isSelected()) {
	 beam_properties.setProperty(NOTE_TOP_COLOR_PROP,default_top_color);
	 beam_properties.setProperty(NOTE_BOTTOM_COLOR_PROP,default_bottom_color);
	 try {
	    beam_properties.save();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BEAM","Problem saving properties",e);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(new NameNote());
   menu.add(new SetColorAction());
   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}


/********************************************************************************/
/*										*/
/*	Color actions								*/
/*										*/
/********************************************************************************/

private class SetColorAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SetColorAction() {
      super("Set Note Color");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      SwingGridPanel pnl = new SwingGridPanel();
      SwingColorRangeChooser crc = null;
      SwingColorButton cc = null;
      if (default_top_color == default_bottom_color) {
	 cc = pnl.addColorField("Note Color",top_color,null);
       }
      else {
	 crc = pnl.addColorRangeField("Note Color",top_color,bottom_color,null);
       }
      int sts = JOptionPane.showOptionDialog(BeamNoteBubble.this,pnl,"Choose Color for this Note",
	    JOptionPane.OK_CANCEL_OPTION,
	    JOptionPane.QUESTION_MESSAGE,null,null,null);
      if (sts == JOptionPane.OK_OPTION) {
	 Color tc, bc;
	 if (crc !=  null) {
	    tc = crc.getFirstColor();
	    bc = crc.getSecondColor();
	  }
	 else {
	    tc = cc.getColor();
	    bc = tc;
	  }
	 setNoteColor(tc,bc);
       }
    }

}	// end of inner class SetColorAction



void setNoteColor(Color top,Color bot)
{
   top_color = top;
   bottom_color = bot;
   if (bottom_color.getRGB() == top_color.getRGB()) {
      top_color = bottom_color;
      note_area.setBackground(top_color);
    }
   else {
      note_area.setBackground(BoardColors.transparent());
    }
   note_area.repaint();
}

Color getTopColor()			{ return top_color; }

Color getBottomColor()			{ return bottom_color; }




/********************************************************************************/
/*										*/
/*	Scaling requests							*/
/*										*/
/********************************************************************************/

@Override public void setScaleFactor(double sf)
{
   Font ft = beam_properties.getFont(NOTE_FONT_PROP,NOTE_FONT);
   float sz = ft.getSize2D();
   if (sf != 1.0) {
      sz *= sf;
      ft = ft.deriveFont(sz);
    }
   note_area.setFont(ft);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/*********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();

   g.setColor(BoardColors.getColor(NOTE_OVERVIEW_COLOR_PROP));
   g.fillRect(0,0,sz.width,sz.height);
}



/********************************************************************************/
/*										*/
/*	Save interface								*/
/*										*/
/********************************************************************************/

@Override public void handleSaveRequest()
{
   saveNote();
}



@Override public void handleCheckpointRequest()
{
   saveNote();
}



/********************************************************************************/
/*										*/
/*	Annotation interface							*/
/*										*/
/********************************************************************************/

void clearAnnotation(BeamNoteAnnotation na)
{
   if (na != null && na == note_annot) note_annot = null;
}



/********************************************************************************/
/*										*/
/*	Methods for loading and saving notes					*/
/*										*/
/********************************************************************************/

static void updateNote(String name,String cnts)
{
   Document d = file_documents.get(name);
   if (d == null) return;

   int len = d.getLength();
   try {
      String txt = d.getText(0,len);
      if (txt.equals(cnts)) return;
      EditorKit ek = new NoteEditorKit();
      StringReader sr = new StringReader(cnts);
      ek.read(sr,d,0);
      // d.remove(0,len);
      // d.insertString(0,cnts,null);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BEAM","Problem updating note",e);
    }
   catch (IOException e) {
      BoardLog.logE("BEAM","Problem updating node",e);
    }
}




private synchronized void loadNote(boolean force)
{
   if (note_name == null) createFileName();

   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 BumpClient bc = BumpClient.getBump();
	 try {
	    File tmp = File.createTempFile("BUBBLES_NOTE_",".html");
	    File f = bc.getRemoteFile(tmp,"NOTE",new File(note_name));
	    if (f != null) loadNoteFromFile(f,force);
	    tmp.delete();
	  }
	 catch (IOException e) { }
	 break;
      case SERVER :
      case NORMAL :
	 loadNoteFromFile(getNoteFile(),force);
	 break;
    }
}



private void loadNoteFromFile(File f,boolean force)
{
   String cnts = "";

   try {
      FileReader fr = new FileReader(getNoteFile());
      StringBuffer cbuf = new StringBuffer();
      char [] buf = new char[1024];
      for ( ; ; ) {
	 int ln = fr.read(buf);
	 if (ln < 0) break;
	 cbuf.append(buf,0,ln);
       }
      cnts = cbuf.toString();
      fr.close();
    }
   catch (IOException e) {
      if (force) BoardLog.logE("BEAM","Problem reading note file",e);
      else return;
    }

   Matcher matcher = COLOR_PATTERN.matcher(cnts);
   if (matcher.find()) {
      cnts = cnts.substring(0,matcher.start());
      String c1s = matcher.group(1);
      String c2s = matcher.group(2);
      try {
	 int v1 = Integer.parseInt(c1s,16);
	 int v2 = Integer.parseInt(c2s,16);
	 boolean fg = (v1 & 0xff000000) != 0;
	 Color c1 = new Color(v1,fg);
	 Color c2 = new Color(v2,fg);
	 setNoteColor(c1,c2);
       }
      catch (NumberFormatException e) { }
    }

   note_area.setText(cnts);
}



private synchronized void saveNote()
{
   if (note_name == null) createFileName();

   String txt = note_area.getText();
   txt = fixupHtmlText(txt);
   if (top_color != default_top_color || bottom_color != default_bottom_color) {
      String cinfo = COLOR_OUTPUT;
      cinfo = cinfo.replace("%1",Integer.toHexString(top_color.getRGB()));
      cinfo = cinfo.replace("%2",Integer.toHexString(bottom_color.getRGB()));
      if (!txt.endsWith("\n")) txt += "\n";
      txt += cinfo + "\n";
    }

   BoardSetup bs = BoardSetup.getSetup();

   switch (bs.getRunMode()) {
      case CLIENT :
	 break;
      case SERVER :
      case NORMAL :
	 try {
	    FileWriter fw = new FileWriter(getNoteFile());
	    fw.write(txt);
	    fw.close();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BEAM","Problem writing note file",e);
	  }
	 break;
    }

   switch (bs.getRunMode()) {
      case CLIENT :
      case NORMAL :
	 MintControl mc = bs.getMintControl();
	 IvyXmlWriter xw = new IvyXmlWriter();
	 xw.begin("BEAM");
	 xw.field("TYPE","NOTE");
	 xw.field("NAME",note_name);
	 xw.cdataElement("TEXT",txt);
	 xw.end("BEAM");
	 mc.send(xw.toString());
	 break;
      case SERVER :
	 break;
    }
}



private String fixupHtmlText(String txt)
{
   StringBuffer buf = new StringBuffer();
   HtmlState state = HtmlState.LINE;
   int indent = 0;
   int pos = 0;

   for (int i = 0; i < txt.length(); ++i) {
      char c = txt.charAt(i);
      switch (state) {
	 case LINE :
	    if (c == ' ') {
	       if (pos <= indent) break;
	       else state = HtmlState.TEXT;
	     }
	    else if (c == '\n') state = HtmlState.LINE;
	    else {
	       i -= 1;
	       state = HtmlState.TEXT;
	       continue;
	     }
	    break;
	 case TAG :
	    if (c == '>') {
	       if (txt.charAt(i-1) == '/') indent -= 2;
	       state = HtmlState.TEXT;
	     }
	    else if (c == '"') state = HtmlState.STRING2;
	    else if (c == '\'') state = HtmlState.STRING1;
	    break;
	 case STRING1 :
	    if (c == '\'') state = HtmlState.TAG;
	    break;
	 case STRING2 :
	    if (c == '"') state = HtmlState.TAG;
	    break;
	 case TEXT :
	    if (c == '\n') state = HtmlState.LINE;
	    else if (c == '<') {
	       if (i+1 <= txt.length() && txt.charAt(i+1) == '/') indent -= 2;
	       else indent += 2;
	       state = HtmlState.TAG;
	     }
	    else if (c == ' ' && pos > indent) {
	       buf.append("&nbsp;");
	       c = 0;
	     }
	    break;
       }

      if (c == 0) continue;
      buf.append(c);
      if (c == '\n') pos = 0;
      else pos = pos+1;
    }

   return buf.toString();
}


private synchronized void deleteNote()
{
   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 BumpClient bc = BumpClient.getBump();
	 bc.deleteRemoteFile("NOTE",new File(note_name));
	 break;
      case SERVER :
      case NORMAL :
	 File f = getNoteFile();
	 f.delete();
	 break;
    }
}


File getNoteFile()
{
   if (note_name == null) createFileName();
   File dir = BoardSetup.getBubblesWorkingDirectory();
   return new File(dir,note_name);
}


private void createFileName()
{
   if (note_name != null) return;

   String rid = Integer.toString((int)(Math.random() * 10000));
   String fnm = "Note_" + file_dateformat.format(new Date()) + "_" + rid + ".html";
   note_name = fnm;
}


/********************************************************************************/
/*										*/
/*	File management for saved notes 					*/
/*										*/
/********************************************************************************/

private boolean isUserName()
{
   return isUserName(note_name);
}

private static boolean isUserName(String name)
{
   Matcher m = temp_name.matcher(name);
   if (m.matches()) return false;
   return true;
}



private static List<String> getSavedNoteNames()
{
   List<String> rslt = new ArrayList<>();
   switch (BoardSetup.getSetup().getRunMode()) {
      case SERVER :
      case NORMAL :
	 File dir = BoardSetup.getBubblesWorkingDirectory();
	 for (File f : dir.listFiles()) {
	    if (f.isDirectory()) continue;
	    if (!f.canRead()) continue;
	    if (!f.getName().endsWith(".html")) continue;
	    if (!isUserName(f.getName())) continue;
	    String s = f.getName();
	    int idx = s.lastIndexOf(".html");
	    s = s.substring(0,idx);
	    rslt.add(s);
	  }
	 break;
      case CLIENT :
	 try {
	    BumpClient bc = BumpClient.getBump();
	    File tmp = File.createTempFile("BUBBLES_NOTE_",".html");
	    File f = bc.remoteFileAction("LIST",tmp,"NOTE",null);
	    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
	       for ( ; ; ) {
		  String s = br.readLine();
		  if (s == null) break;
		  if (!isUserName(s)) continue;
		  if (!s.endsWith(".html")) continue;
		  int idx = s.lastIndexOf(".html");
		  s = s.substring(0,idx);
		  rslt.add(s);
		}
	     }
	    tmp.delete();
	  }
	 catch (IOException e) {
	    BoardLog.logE("BEAM","Problem loading remote saved note list",e);
	 }
	 break;
    }

   return rslt;
}


private void setNoteName(String nm)
{
   if (nm == null && !isUserName()) return;
   if (nm != null && nm.equals(note_name)) return;
   deleteNote();
   if (nm != null && !nm.endsWith(".html")) nm = nm + ".html";
   note_name = nm;
   saveNote();
}




/********************************************************************************/
/*										*/
/*	Configurator interface							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()		{ return "BEAM"; }


@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","NOTE");
   if (note_name != null) xw.field("NAME",note_name);
   if (top_color != default_top_color || bottom_color != default_bottom_color) {
      xw.field("TOPCOLOR",top_color);
      xw.field("BOTTOMCOLOR",bottom_color);
    }
   xw.cdataElement("TEXT",note_area.getText());
   if (note_annot != null) note_annot.saveAnnotation(xw);
}



/********************************************************************************/
/*										*/
/*	Note area implementation						*/
/*										*/
/********************************************************************************/

private class NoteArea extends JEditorPane
{
   private static final long serialVersionUID = 1;

   NoteArea(String cnts) {
      super("text/html",cnts);
      initialize(cnts);
      setText(cnts);
      setSelectionStart(0);
      setSelectionEnd(0);
      setCaretPosition(0);
    }

   NoteArea(Document d) {
      setContentType("text/html");
      initialize(null);
      setDocument(d);
    }

   private void initialize(String cnts) {
      NoteEditorKit nek = new NoteEditorKit();
      setEditorKit(nek);
      Keymap km = note_keymap;
      setKeymap(km);
      Dimension d = new Dimension(beam_properties.getInt(NOTE_WIDTH),beam_properties.getInt(NOTE_HEIGHT));
      if (cnts != null) {
	 JLabel lbl = new JLabel(cnts);
	 Dimension d1 = lbl.getPreferredSize();
	 d1.width = Math.min(d1.width,900);
	 d1.height = Math.min(d1.height,400);
	 d.width = Math.max(d.width, d1.width);
	 d.height = Math.max(d1.height, d1.height);
      }
      setPreferredSize(d);
      setSize(d);
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setFont(beam_properties.getFont(NOTE_FONT_PROP,NOTE_FONT));
      addMouseListener(new BudaConstants.FocusOnEntry());
      addMouseListener(new LinkListener());
      addHyperlinkListener(new HyperListener());

      if (top_color == bottom_color) {
	 setBackground(top_color);
       }
      else setBackground(BoardColors.transparent());

      BurpHistory.getHistory().addEditor(this);
    }



   @Override protected void paintComponent(Graphics g0) {
      if (top_color != bottom_color) {
	 Graphics2D g2 = (Graphics2D) g0.create();
	 Dimension sz = getSize();
	 Paint p = new GradientPaint(0f,0f,top_color,0f,sz.height,bottom_color);
	 Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
	 g2.setPaint(p);
	 g2.fill(r);
       }
      super.paintComponent(g0);
    }

}	// end of inner class NoteArea



/********************************************************************************/
/*										*/
/*	Actions for renaming notes						*/
/*										*/
/********************************************************************************/

private class NameNote extends AbstractAction {

   private static final long serialVersionUID = 1;

   NameNote() {
      super("Name this Note");
    }

   @Override public void actionPerformed(ActionEvent e) {
      NamePanel pnl =  new NamePanel();
      int sts = JOptionPane.showOptionDialog(BeamNoteBubble.this,pnl,
	    "Set Note Name",JOptionPane.OK_CANCEL_OPTION,
	    JOptionPane.PLAIN_MESSAGE,null,null,null);
      if (sts == JOptionPane.OK_OPTION) {
	 String nm = pnl.getNoteName();
	 setNoteName(nm);
       }
    }

}	// end of inner class NameNode



private class NamePanel extends SwingGridPanel
	implements ActionListener, UndoableEditListener {

   private JTextField name_field;
   private JCheckBox remove_button;

   private static final long serialVersionUID = 1;

   NamePanel() {
      String nm = null;
      if (isUserName()) nm = note_name;
      beginLayout();
      name_field = addTextField("Note Name",nm,null,null);
      remove_button = addBoolean("Remove Note Name",false,this);
    }

   String getNoteName() {
      String nm = name_field.getText();
      if (remove_button.isSelected()) {
	 if (nm == null || nm.length() == 0 || nm.equals(note_name)) {
	    return null;
	  }
       }
      if (nm == null || nm.length() == 0) return note_name;
      else return nm;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() == remove_button) {
	 if (remove_button.isSelected()) {
	    name_field.setText("");
	  }
	 else if (isUserName()) name_field.setText(note_name);
       }
    }

   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      if (name_field.getText().length() == 0) remove_button.setSelected(true);
      else remove_button.setSelected(false);
    }

}	// end of inner class NamePanel




/********************************************************************************/
/*										*/
/*	Editor Kit for notes							*/
/*										*/
/********************************************************************************/

private static class NoteEditorKit extends HTMLEditorKit
{

   private static final long serialVersionUID = 1;

   NoteEditorKit() {
      setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

  @Override public Action [] getActions() {
     return TextAction.augmentList(super.getActions(),default_actions);
   }

  private static class NoteFontSizeAction extends FontSizeAction {

     private static final long serialVersionUID = 1;

     NoteFontSizeAction(String nm,int sz) {
	super(nm,sz);
	putValue(ACTION_COMMAND_KEY,nm);
      }

   }	// end of inner class NoteFontSizeAction


   private static class NoteColorAction extends ForegroundAction {

      private static final long serialVersionUID = 1;

      NoteColorAction(String nm,Color c) {
	 super(nm,c);
	 putValue(ACTION_COMMAND_KEY,nm);
       }

    }	// end of inner class NoteColorAction

}	// end of inner class NoteEditorKit


private static class StrikeThruAction extends StyledEditorKit.StyledTextAction {

   private static final long serialVersionUID = 1;

   StrikeThruAction() {
      super("font-strikethrough");
      putValue(ACTION_COMMAND_KEY,"font-strikethrough");
    }

   @Override public void actionPerformed(ActionEvent e) {
      JEditorPane editor = getEditor(e);
      if (editor != null) {
	 StyledEditorKit kit = getStyledEditorKit(editor);
	 MutableAttributeSet attr = kit.getInputAttributes();
	 boolean on = (StyleConstants.isStrikeThrough(attr));
	 boolean val = (on ? false : true);
	 SimpleAttributeSet sas = new SimpleAttributeSet();
	 StyleConstants.setStrikeThrough(sas,val);
	 setCharacterAttributes(editor,sas,false);
       }
    }

}	// end of inner class StrikeThruAction



private static class SaveAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveAction() {
      super("Save");
    }

   @Override public void actionPerformed(ActionEvent e) {
      Component c = (Component) e.getSource();
      BeamNoteBubble bb = (BeamNoteBubble) BudaRoot.findBudaBubble(c);
      if (bb == null) return;
      bb.saveNote();
    }

}	// end of inner class SaveAction



/********************************************************************************/
/*										*/
/*	Key designator class							*/
/*										*/
/********************************************************************************/

private static class KeyItem {

   private KeyStroke key_stroke;
   private String key_action;

   KeyItem(String key,String a) {
      key = fixKey(key);
      key_stroke = KeyStroke.getKeyStroke(key);
      if (key_stroke == null) BoardLog.logE("BEAM","Bad key definition: " + key);
      key_action = a;
    }

   void addToKeyMap(Keymap kmp,Action[] acts) {
      if (key_stroke != null && key_action != null) {
	 for (Action a : acts) {
	    Object nm = a.getValue(Action.NAME);
	    if (nm.toString().equals(key_action)) {
	       kmp.addActionForKeyStroke(key_stroke,a);
	       return;
	     }
	  }
	 BoardLog.logE("BEAM","Action " + key_action + " not found");
       }
    }

   private String fixKey(String key) {
      return key.replace(MENU_KEY,menu_keyname);
    }

}	// end of inner class KeyItem



/********************************************************************************/
/*										*/
/*	Callbacks to handle automatic saving					*/
/*										*/
/********************************************************************************/

private class ComponentHandler extends ComponentAdapter {

   @Override public void componentHidden(ComponentEvent e) {
      if (note_name != null && note_area != null) {
	 if (note_area.getText().length() > 0) saveNote();
       }
    }

}	// end of inner class ComponentHandler



/********************************************************************************/
/*										*/
/*	Link listener								*/
/*										*/
/********************************************************************************/

private static class LinkListener extends HTMLEditorKit.LinkController {

   private static final long serialVersionUID = 1;


   @Override public void mouseClicked(MouseEvent e) {
      JEditorPane editor = (JEditorPane) e.getSource();
      if (!editor.isEditable()) return;
      if (!SwingUtilities.isLeftMouseButton(e)) return;
      int mods = e.getModifiersEx();
      if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
	 Point pt = new Point(e.getX(),e.getY());
	 int pos = SwingText.viewToModel2D(editor,pt);
	 if (pos >= 0) {
	    activateLink(pos,editor);
	  }
       }
    }

}	// end of inner class LinkListener



private static class HyperListener implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	 URL u = e.getURL();
	 try {
	    BeamFactory.showBrowser(u.toURI());
	  }
	 catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class HyperListener



}	// end of class BeamNoteBubble



/* end of BeamNoteBubble.java */
