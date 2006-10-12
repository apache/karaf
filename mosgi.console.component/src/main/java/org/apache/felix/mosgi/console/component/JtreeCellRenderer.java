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
import javax.swing.JTree;
import javax.swing.JLabel;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Graphics;
import java.util.NoSuchElementException;
import java.awt.Font;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

public class JtreeCellRenderer extends DefaultTreeCellRenderer {

  private boolean isLeaf=false;

  private String[] states=new String[]{"ACTIVE","INSTALLED","RESOLVED","STARTING","STOPPING","UNINSTALLED"};
  private Color[] colors=new Color[]{Color.green,Color.orange,Color.red,Color.white,Color.white,Color.black};
  private ImageIcon[] ii=new ImageIcon[6];
  private ImageIcon iiOldLog=null;

  public JtreeCellRenderer(BundleContext bdlCtx){
    for (int i=0 ; i<states.length ; i++){
      this.ii[i]=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/"+states[i]+".gif")));
    }
    this.iiOldLog=new ImageIcon(Toolkit.getDefaultToolkit().getImage(bdlCtx.getBundle().getResource("icons/OLDLOG.gif")));

  }

  public Dimension getPreferredSize() {
    Dimension dim = super.getPreferredSize();
    //maxL=(isLeaf)?Math.max(maxL,dim.width):maxL;
    //
    return (isLeaf)?dim:new Dimension(800/*maxL*/,dim.height);
  }

  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    this.isLeaf=leaf;

    super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
    setOpaque(true);
    setBackground(Color.white);
    setFont(new Font("Monospaced",Font.BOLD,12));
    setToolTipText(null);

    StringTokenizer st=null;
    DefaultMutableTreeNode dmtn=(DefaultMutableTreeNode)value;
    int lvl=dmtn.getLevel(); 
    if(lvl==3 & dmtn.getChildCount()>0){
      st=new StringTokenizer(dmtn.getFirstChild().toString()," | ");
    }
    if (lvl==4){
      st=new StringTokenizer(dmtn.toString()," | ");
    }

    if(st!=null){
      String date=st.nextToken();
      st.nextToken();
      String state=st.nextToken();
      for (int i=0 ; i<states.length ; i++){
        if (state.equals(states[i])){
          if (lvl==4){
	    if(!date.equals("??/??/??")){
	      setIcon(ii[i]);
	    }else{
	      setIcon(iiOldLog);
	    }
	    setFont(new Font("Monospaced",Font.PLAIN,10));
          }else{ 
            StringTokenizer st2 = new StringTokenizer(((DefaultMutableTreeNode)dmtn.getFirstChild()).toString()," | ");
            setToolTipText("<html><B>IP = </B>"+/*IP=<ip> Profil=<port>/<profil>*/dmtn.getParent().getParent()+"<B> Profil =</B>"+dmtn.getParent()+"<br><B>Bundle : </B>"+/*Bundle : Id=<bundleId> : <bundleSymbolicName>*/dmtn+"<br><B>Date : </B>"+/*<date> - <time>*/st2.nextToken()+" - "+st2.nextToken()+"<br><B>State : "+/*<bundleState>*/st2.nextToken()+"<br>Event "+/*Event <eventNumber> : <logLevel> : <message>*/dmtn.getChildCount()+" : "+st2.nextToken()+" : "+st2.nextToken()+"</B></html>");
            setBackground(colors[i]);
          }
	break;
        }
      }
    }	
	
    return this;
  }	

}
