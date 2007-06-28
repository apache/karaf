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

import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import org.apache.felix.mosgi.console.ifc.Plugin;
import org.apache.felix.mosgi.console.component.MyTree;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import java.beans.PropertyChangeEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.management.Attribute;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;
import java.util.Enumeration;
import java.text.DateFormat;
//import org.osgi.service.prefs.Preferences;

public class RemoteLogger_jtree extends DefaultTreeModel implements CommonPlugin, NotificationListener, MouseListener {

  private static final String OLDLOG_THIS_TIME     ="This time";
  private static final String OLDLOG_NOT_THIS_TIME ="Not this time";
  private static final String OLDLOG_ALWAYS        ="Always";
  private static final String OLDLOG_NEVER         ="Never";
  private String oldLogChoice=OLDLOG_THIS_TIME;
  
  private static final String[] LOG_LVL=new String[] {"Error", "Warning", "Info", "Debug"};
  private Hashtable logLvlHt=new Hashtable(); // treeNode/logLvl

  private MyTree logTree;
  private TreePath selPath;
  private JPanel jp;
  private DefaultMutableTreeNode rootNode=new DefaultMutableTreeNode("root");
  private Hashtable eventName=new Hashtable();

  private Hashtable nodes=new Hashtable();    // connString/mbsc

  public RemoteLogger_jtree (BundleContext bdlCtx){
    //super(rootNode);
    super(null);
    setRoot(rootNode);
    System.out.println("JTree Remote logger"); 

    this.jp=new JPanel();
    this.jp.setLayout(new BorderLayout());
   
    this.logTree=new MyTree(this);
    JtreeCellRenderer treeCellRenderer=new JtreeCellRenderer(bdlCtx, this);
    this.logTree.setCellRenderer(treeCellRenderer);
    this.logTree.setLargeModel(true);
    this.logTree.setToggleClickCount(-1); 
    this.logTree.setScrollsOnExpand(false);
    //this.jp.setToolTipText("Select a node to refresh"); // not visible
    this.logTree.setRootVisible(false);
    // this create an invisible tree, even if I use "expand" so...
    // I use expand after the first insert into the tree
   
    this.logTree.addMouseListener(this);

    jp.add(new JScrollPane(logTree), BorderLayout.CENTER);    
    jp.setMinimumSize(new Dimension(500,25));

    eventName.put(new Integer(Bundle.ACTIVE),     "ACTIVE     ");
    eventName.put(new Integer(Bundle.INSTALLED),  "INSTALLED  ");
    eventName.put(new Integer(Bundle.RESOLVED),   "RESOLVED   ");
    eventName.put(new Integer(Bundle.STARTING),   "STARTING   ");
    eventName.put(new Integer(Bundle.STOPPING),   "STOPPING   ");
    eventName.put(new Integer(Bundle.UNINSTALLED),"UNINSTALLED");
  }

  /////////////////////////////////////////////////////
  //           Mouse Listener Interface              //
  /////////////////////////////////////////////////////
  public void mouseEntered(MouseEvent e){}
  public void mouseClicked(MouseEvent e) {}
  public void mouseExited(MouseEvent e){}
  public void mouseReleased(MouseEvent e){} 
  public void mousePressed(MouseEvent e) {
    int selRow = logTree.getRowForLocation(e.getX(), e.getY());
    selPath = logTree.getPathForLocation(e.getX(), e.getY());
    JScrollPane jsp_tmp=(JScrollPane) logTree.getParent().getParent();
    JScrollBar horizontalJsb=jsp_tmp.getHorizontalScrollBar();
    JScrollBar verticalJsb=jsp_tmp.getVerticalScrollBar();

    if ( e.getClickCount()==1 & selRow!=-1 & e.getButton()>1 ) { // show JPopupMenu
      String nodeString="\""+((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject()+"\"";
      JPopupMenu jpopup=new JPopupMenu();
      JMenuItem jmiRemove=new JMenuItem("Remove logs \""+nodeString.substring(0,Math.min(15,nodeString.length()))+((nodeString.length()>15)?"...\"":"\""));
      jmiRemove.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e){
	  removeLog_actionPerformed();
        }	
      });
      jpopup.add(jmiRemove);
      if (selPath.getPath().length==3) {
        JMenuItem jmiLogLvl=new JMenuItem("Set log lvl");
        jmiLogLvl.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e){
            setLogLvl(selPath);
          }
        });
        jpopup.add(jmiLogLvl);
      }
      jpopup.show(jp, e.getX()-horizontalJsb.getValue(), e.getY()-verticalJsb.getValue());
    } else if ( e.getClickCount()==2 & selPath!=null) { // expand selected path
      if (logTree.isExpanded(selPath)) {
        logTree.collapsePath(selPath);
      } else {
        logTree.expandPath(selPath);
      }
    } else if ( e.getClickCount()==1 & selPath!=null ) { // reload logTree and let selected path location
      int horizontal_jsb_init_value=horizontalJsb.getValue();
      int vertical_jsb_init_value=verticalJsb.getValue();
      int row_y_init_loc=(int) ((logTree.getRowBounds(selRow)).getY());
      Enumeration enu=logTree.getExpandedDescendants(new TreePath(rootNode));
      reload();
      if (enu!=null) {
        while (enu.hasMoreElements()) {
	  logTree.expandPath((TreePath) enu.nextElement());
        }
      }
      // without next line if scrollbar_value=scrollbar_max it's generate a bad shift (may be vertical scrollbar height)
      logTree.scrollPathToVisible(selPath);
      int row_y_new_loc=(int) ((logTree.getRowBounds(logTree.getRowForPath(selPath))).getY());
      int vertical_jsb_new_value=vertical_jsb_init_value+(row_y_new_loc-row_y_init_loc);
      verticalJsb.setValue(vertical_jsb_new_value);
      horizontalJsb.setValue(horizontal_jsb_init_value);
      logTree.setSelectionPath(selPath);
      logTree.repaint();
    }
  }

  /////////////////////////////////////
  //        Plugin Interface         //
  /////////////////////////////////////
  public String getName(){ return "JTree Remote Logger";}
  public Component getGUI(){return this.jp;}

  /* a supprimer si on enleve l'heritage CommonPlugin -> Plugin */
  public String pluginLocation(){
    return null;
  }
  public void registerServicePlugin(){}
  public void unregisterServicePlugin(){}
  /* fin a supprimer */
 
  public void propertyChange(PropertyChangeEvent e){
    // TODO : DEBUG
    // Sometimes ???
    //   *1)  when stay with "return" key pressed => JoptionPane miss getValue()
    //   *2)  when commonPanel started after a new_node_connection event.
    //        Slow or slowed (by a key pressed for exemple) computer.
    //        => gui.NodesTree fireNewNodeConnection after each PCE_common_plugin_added for each connected nodes
    if (e.getPropertyName().equals(Plugin.NEW_NODE_CONNECTION)){
      try{
        MBeanServerConnection mbsc=(MBeanServerConnection)e.getNewValue();
	if ( !nodes.containsValue(mbsc) ) {
	  String connString=(String) e.getOldValue();
	  mbsc.addNotificationListener(new ObjectName("OSGI:name=Remote Logger"), this, null, connString);
	  nodes.put(connString, mbsc);
	  this.addNodesIpRef(connString);
          if (oldLogChoice==OLDLOG_THIS_TIME | oldLogChoice==OLDLOG_NOT_THIS_TIME) {
            JOptionPane jop = new JOptionPane("Do you want old log from gateway :\n"+connString+" ?", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[] {OLDLOG_THIS_TIME, OLDLOG_NOT_THIS_TIME, OLDLOG_ALWAYS, OLDLOG_NEVER}, OLDLOG_THIS_TIME);
            JDialog dialog = jop.createDialog(jp, "Old log management");
	    //dialog.setModal(true);
	    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	    dialog.show();
	    oldLogChoice = (String) jop.getValue();
	    if (oldLogChoice==JOptionPane.UNINITIALIZED_VALUE) {
              oldLogChoice=OLDLOG_THIS_TIME; // *1)
	    }
	  } 
          if (oldLogChoice==OLDLOG_THIS_TIME | oldLogChoice==OLDLOG_ALWAYS) {
            mbsc.invoke(new ObjectName("OSGI:name=Remote Logger"), "sendOldLog", new Object[]{}, new String[]{});
	  }
        }
      } catch(Exception ex){
        System.out.println("[RemoteLogger_jtree] error : "+ex);
      }
    }
  }
 
  ///////////////////////////////////////////////////
  //       NotificationListener implementation     //  
  ///////////////////////////////////////////////////
  public void handleNotification(Notification notification, Object handback) {
    TreePath treeP=logTree.getLeadSelectionPath();
    StringTokenizer st=new StringTokenizer(handback.toString(),":");
    boolean isOldLog=false;
    String ip=st.nextToken();
    String ref=st.nextToken();
  
    st = new StringTokenizer(notification.getMessage(),"*");
    long ts=notification.getTimeStamp();
    String time="??:??:??";
    String date="??/??/??";
    if (ts==0) {
      isOldLog=true;
    } else {
      Date timeDate=new Date(ts);
      //DateFormat dateFormat = new SimpleDateFormat("hh'h'mm dd-MM-yy");
      DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM); // use local date format
      DateFormat df2 = DateFormat.getDateInstance(DateFormat.SHORT);
      time=df.format(timeDate);
      date=df2.format(timeDate);
    }
    String id=st.nextToken();
    String name=st.nextToken();
    String idname=new String(id+" : "+name);
    String state=""+eventName.get(new Integer((int) Integer.parseInt(st.nextToken())));
    String lvl=st.nextToken();
    String msg=st.nextToken();
    // Get and maybe create parents nodes : ip / ref / idname
    DefaultMutableTreeNode dmtn_ip=createIfNeed(ip, rootNode);
    DefaultMutableTreeNode dmtn_ref=createIfNeed(ref, dmtn_ip);
    DefaultMutableTreeNode dmtn_idname=createIfNeed(idname, dmtn_ref);
    // insert the leaf with message under id/ref/idname
    DefaultMutableTreeNode dmtn=new DefaultMutableTreeNode(date+" | "+time+" | "+state+" | "+lvl+" | "+msg,false);
    this.insertNodeInto(dmtn, dmtn_idname, 0);

    TreePath selectedPath=this.logTree.getLeadSelectionPath();
    if (selectedPath!=null) {
      this.logTree.setSelectionRow(-1);
      this.logTree.repaint();
    }
  }

  //////////////////////////////////////////
  //     MBean attribute manipulation     //
  //////////////////////////////////////////
  private Integer getLogLvl(String connString) {
    Integer val=new Integer(0);
    try {
      MBeanServerConnection mb=(MBeanServerConnection) nodes.get(connString);
      val=(Integer) mb.getAttribute(new ObjectName("OSGI:name=Remote Logger"), "LogLvl");
    } catch (Exception exc) {
      System.out.println("errrrror : "+exc);
    }
    return val;
  }

  private void setLogLvl(TreePath tp) {
    Object[] o=tp.getPath();
    String connS=""+o[1]+":"+o[2];
    MBeanServerConnection mb=(MBeanServerConnection) nodes.get(connS);
    try {
      Integer curentVal=(Integer) mb.getAttribute(new ObjectName("OSGI:name=Remote Logger"), "LogLvl");

      JOptionPane jop = new JOptionPane("Select a log level for \""+connS+"\" :", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, LOG_LVL, LOG_LVL[curentVal.intValue()-1]);
      JDialog dialog = jop.createDialog(jp, "Log level");
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.show();
      String choice = (String) jop.getValue();
      Integer newVal=new Integer(4);
      if (choice.equals("Error")) {newVal=new Integer(1);}
      else if (choice.equals("Warning")) {newVal=new Integer(2);}
      else if (choice.equals("Info")) {newVal=new Integer(3);}
      else if (choice.equals("Debug")) {newVal=new Integer(4);}

      mb.setAttribute(new ObjectName("OSGI:name=Remote Logger"), new Attribute("LogLvl", newVal));
      DefaultMutableTreeNode ddmmttnn=(DefaultMutableTreeNode) tp.getLastPathComponent();
      logLvlHt.put(ddmmttnn, newVal);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(jp,"Error with \""+connS+"\" :\n"+ex, "Error :", JOptionPane.ERROR_MESSAGE);
    }
  }

  //////////////////////////////////////////
  //         PRIVATE TOOLS                //
  //////////////////////////////////////////
  private void removeLog_actionPerformed() {
    //System.out.println("selected path="+this.selPath);
    Object[] o= this.selPath.getPath();
    DefaultMutableTreeNode selectedDmtn=(DefaultMutableTreeNode) this.selPath.getLastPathComponent();
    // Select root to avoid remove the selection and generate select=-1 which generate blue font
    logTree.setSelectionRow(0);
    if (o.length==5) {
      // Can't remove first child of a bundle to avoid modify tree node color
      if ( ((DefaultMutableTreeNode) selectedDmtn.getParent()).getFirstChild()!=selectedDmtn ) {
        removeNodeFromParent(selectedDmtn);
      }
    } else if (o.length==4) {
      removeNodeFromParent(selectedDmtn);
    } else if (o.length==3) {
      Enumeration enume=selectedDmtn.children();
      Vector v=new Vector();
      while (enume.hasMoreElements()) {
        v.add(enume.nextElement()); // modification on an enumeration element destroy the enumeration
      }
      for (int i=0; i<v.size() ; i++) {
        DefaultMutableTreeNode ddmtn_tmp=(DefaultMutableTreeNode) v.elementAt(i);
        removeNodeFromParent(ddmtn_tmp);
      }
    } else if (o.length==2) {
      Enumeration enu_1=selectedDmtn.children();
      while (enu_1.hasMoreElements()) {
        DefaultMutableTreeNode dmtn_child=(DefaultMutableTreeNode) enu_1.nextElement();
        Enumeration enu_2=dmtn_child.children();
        Vector v=new Vector();
        while (enu_2.hasMoreElements()) {
          v.add(enu_2.nextElement());
        }
        for (int i=0; i<v.size() ; i++) {
          DefaultMutableTreeNode ddmtn_tmp=(DefaultMutableTreeNode) v.elementAt(i);
          removeNodeFromParent(ddmtn_tmp);
        }
      }
    }
  }

  private void addNodesIpRef(String connString) {
    String ip=connString.split(":")[0];
    String ref=connString.split(":")[1];
    DefaultMutableTreeNode dmtn_ip=createIfNeed(ip, rootNode);
    DefaultMutableTreeNode dmtn_ref=createIfNeed(ref, dmtn_ip);
    Integer lL=this.getLogLvl(connString);
    logLvlHt.put(dmtn_ref, lL);
    logTree.setSelectionRow(-1);
    // Unable to set tree expand whithout a first node so :
    logTree.expandPath(new TreePath(rootNode.getPath()));
    logTree.repaint();
  }

  protected Integer getTreeNodeLogLvl(DefaultMutableTreeNode dmtn) {
    // used by treeCellRenderer
    return (Integer) logLvlHt.get(dmtn);
  }

  private DefaultMutableTreeNode createIfNeed(String nodeToCreateAndGet, DefaultMutableTreeNode parent) {
    int childNumber=this.getChildCount(parent);
    DefaultMutableTreeNode theNode=null;
    for (int i=0 ; i<childNumber ; i++){ // is node even exist ?
      String string_pool=((DefaultMutableTreeNode)(this.getChild(parent, i))).toString();
      if (string_pool.equals(nodeToCreateAndGet)){
        theNode=(DefaultMutableTreeNode) (this.getChild(parent, i));
        break;
      }
    }
    if (theNode==null){ // create the node
      theNode=new DefaultMutableTreeNode(nodeToCreateAndGet);
      this.insertNodeInto(theNode, parent, 0);
    }
    return theNode;
  }
 
  protected void fireTreeNodesInserted(Object source, Object path[], int childIndices[], Object children[]) {
    // Do nothing to avoid refresh jtree after each log.
  }

}
