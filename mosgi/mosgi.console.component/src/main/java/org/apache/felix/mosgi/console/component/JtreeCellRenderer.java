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
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.lang.StringBuffer;

public class JtreeCellRenderer extends DefaultTreeCellRenderer {

  private static final String[] LOG_LVL=new String[] {"", "Error", "Warning", "Info", "Debug"};

  private boolean isLeaf=false;

  private String[] states=new String[]{"ACTIVE","INSTALLED","RESOLVED","STARTING","STOPPING","UNINSTALLED"};
  private Color[] colors=new Color[]{Color.green,Color.orange,Color.red,Color.gray,Color.gray,Color.black};
  private ImageIcon[] ii=new ImageIcon[6];
  private ImageIcon iiOldLog=null;

  private RemoteLogger_jtree rl_jtree=null;

  public JtreeCellRenderer(BundleContext bdlCtx, RemoteLogger_jtree rl_jtree){
    this.rl_jtree=rl_jtree;
    for (int i=0 ; i<states.length ; i++){
      this.ii[i]=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/"+states[i]+".gif")));
    }
    this.iiOldLog=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/OLDLOG.gif")));
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
    setFont(new Font("Monospaced",Font.BOLD,14));
    setToolTipText(null);
    setIcon(null);
    StringTokenizer st=null;
    DefaultMutableTreeNode dmtn=(DefaultMutableTreeNode)value;

    int lvl=dmtn.getLevel(); 
    switch (lvl) {
      case 2: { // port / profilName / logLvl
        Integer val=rl_jtree.getTreeNodeLogLvl((DefaultMutableTreeNode) value);
        setText(value+" (log level="+LOG_LVL[val.intValue()]+")");
	break;
      }
      case 3: { // bundleId / symbolic name / children count
        // TODO : create a bundleNodeUserObject (getText, getTtt, getStateColor, setNewValues(test,ttt,stateColor))
        st=new StringTokenizer(dmtn.getFirstChild().toString()," | ");
        if(st!=null) {	
          String date=st.nextToken();
          st.nextToken();
          String state=st.nextToken();
	  if (tree.getLeadSelectionPath()!=null) {
	    for (int i=0; i<states.length ; i++) {
	      if (state.equals(states[i])) {
	        setBackground(colors[i]);
	      }
	    }
	    StringTokenizer st2 = new StringTokenizer(((DefaultMutableTreeNode)dmtn.getFirstChild()).toString()," | ");
	    StringBuffer ttt=new StringBuffer("<html><B>IP = </B>"+/*IP=<ip> Profil=<port>/<profil>*/dmtn.getParent().getParent()+"<B> Profil =</B>"+dmtn.getParent()+"<br><B>Bundle : </B>"+/*Bundle : Id=<bundleId> : <bundleSymbolicName>*/dmtn+"<br><B>Date : </B>"+/*<date> - <time>*/st2.nextToken()+" - "+st2.nextToken()+"<br><B>State : "+/*<bundleState>*/st2.nextToken()+"<br>Event "+/*Event <eventNumber> : <logLevel> : <message>*/dmtn.getChildCount()+" : "+st2.nextToken()+" : </B><br>");
	    while (st2.hasMoreTokens()) {
	      ttt.append(st2.nextToken()+" ");
	    }
	    setToolTipText(ttt+"</html>");
	  }
	} 
        break;
      }
      case 4: { // icon / date / time / state / logLvl / msg
        st=new StringTokenizer(dmtn.toString()," | ");
	setFont(new Font("Monospaced",Font.PLAIN,10));
        if(st!=null){
          String date=st.nextToken();
          st.nextToken();
	  String state=st.nextToken();
	  for (int i=0 ; i<states.length ; i++){
	    if (state.equals(states[i])){
	      if(!date.equals("??/??/??")){
	        setIcon(ii[i]);
	      } else{
	        setIcon(iiOldLog);
	      }
	    }
	  }
	}
      break;
      }
    }

    if (tree.getLeadSelectionPath()==null) {
      setForeground(Color.blue);
    } else {
      setForeground(Color.black);
    }
	
    return this;
  }	

}
