/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.mosgi.console.component;

import org.osgi.framework.BundleContext;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.util.StringTokenizer;
import java.lang.StringBuffer;
import java.util.Hashtable;

public class JtreeCellRenderer extends DefaultTreeCellRenderer {

  public static final String UNKNOWN_DATE="??/??/??";
  public static final String UNKNOWN_TIME="??:??:??:???";
  public static Hashtable ht_num2string=new Hashtable();

  private boolean isLeaf=false;
  private RemoteLogger_jtree rl_jtree=null;
  private static final Font FONT_BIG=new Font("Monospaced",Font.BOLD,14);
  private static final Font FONT_SMALL=new Font("Monospaced",Font.PLAIN,10);

  private static Hashtable ht_string2color=new Hashtable();
  private static Hashtable ht_string2icon=new Hashtable();
  private static ImageIcon iiOldLog=null;
  private static ImageIcon iiNewLog=null;
  private static ImageIcon iiNull=null;
  
  public JtreeCellRenderer(BundleContext bdlCtx, RemoteLogger_jtree rl_jtree) {
    this.rl_jtree=rl_jtree;
    
    String[] states=new String[] {
      "Uninstalled",
      "Installed  ",
      "Resolved   ",
      "Starting   ",
      "Stopping   ",
      "Active     "
    };

    Color[] colors=new Color[] {
      Color.black,
      Color.red,
      Color.orange,
      Color.gray,
      Color.gray,
      Color.green
    };
    
    this.iiOldLog=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/OLDLOG.gif")));
    this.iiNewLog=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/NEWLOG.gif")));
    this.iiNull=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/NULL.gif")));
    
    for (int i=0 ; i<states.length ; i++) {
      ht_num2string.put(new Integer((int) Math.pow(2, i)), states[i]);
      ht_string2color.put(states[i].trim(), colors[i]);
      ht_string2icon.put(states[i].trim(), new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/"+states[i].trim()+".gif"))));
    }

  }

  public Dimension getPreferredSize() {
    Dimension dim = super.getPreferredSize();
    return (isLeaf)?dim:new Dimension(800,dim.height);
  }

  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    this.isLeaf=leaf;
    //super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
    setText(value.toString());
    setOpaque(true);
    setBackground(Color.white);
    setFont(FONT_BIG);
    setToolTipText(null);
    StringTokenizer st=null;
    DefaultMutableTreeNode dmtn=(DefaultMutableTreeNode)value;

    if (rl_jtree.v_ul.contains(dmtn)) {
      setIcon(iiNewLog);
    } else {
      setIcon(iiNull);
    }

    int lvl=dmtn.getLevel(); 
    switch (lvl) {
      case 2: { // port / profilName / logLvl
        setText(value+" (log level="+rl_jtree.getLogLvl(dmtn)+")");
	break;
      }
      case 3: { // bundleId / symbolic name / children count
        st=new StringTokenizer(dmtn.getFirstChild().toString(),"|");
        if(st!=null) {	
          String date=st.nextToken().trim();
          st.nextToken();
          String state=st.nextToken().trim();
	  setBackground((Color) ht_string2color.get(state));
	  StringTokenizer st2 = new StringTokenizer(((DefaultMutableTreeNode)dmtn.getFirstChild()).toString()," | ");
	  StringBuffer ttt=new StringBuffer(
	    "<html><B>IP = </B>"+/*IP=<ip> Profil=<port>/<profil>*/dmtn.getParent().getParent()+"<B> Profil =</B>"+dmtn.getParent()+
	    "<br><B>Bundle : </B>"+/*Bundle : Id=<bundleId> : <bundleSymbolicName>*/dmtn+
	    "<br><B>Date : </B>"+/*<date> - <time>*/st2.nextToken()+" - "+st2.nextToken()+
	    "<br><B>State on last log : "+/*<bundleState>*/st2.nextToken()+
	      "<br>Event "+/*Event <eventNumber> : <logLevel> : <message>*/dmtn.getChildCount()+" : "+st2.nextToken()+" : </B><br>");
	  while (st2.hasMoreTokens()) {
	    ttt.append(st2.nextToken()+" ");
	  }
	  setToolTipText(ttt+"</html>");
	} 
        break;
      }
      case 4: { // icon / date / time / state / logLvl / msg
        st=new StringTokenizer(dmtn.toString(),"|");
	setFont(FONT_SMALL);
        if(st!=null){
          String time=st.nextToken().trim();
          st.nextToken();
	  String state=st.nextToken().trim();
	  ImageIcon ii=(ImageIcon) ht_string2icon.get(state);
	  if (time.equals(UNKNOWN_TIME)) {
	    ii=iiOldLog;
	  }
	  setIcon(ii);
	}
      break;
      }

    }	
    return this;
  }	

}

  /*
    // Introspection technique :
    java.lang.Class class_bundle=org.osgi.framework.Bundle.class;
    java.lang.reflect.Field[] fields=class_bundle.getFields();
    for (int i=0 ; i<fields.length ; i++) {
	try {
		String name=fields[i].getName();
		int value=fields[i].getInt(null);
		System.out.println("Cst # "+i+" \""+name+"\" = "+value);
	}catch (Exception oups) {
		oups.printStackTrace();
	}
  }
  */
