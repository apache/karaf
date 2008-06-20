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
import java.beans.PropertyChangeEvent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
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
import java.text.SimpleDateFormat;
//import org.osgi.service.prefs.Preferences;

public class RemoteLogger_jtree extends DefaultTreeModel implements CommonPlugin, NotificationListener, MouseListener {

  private static final String OLDLOG_THIS_TIME     ="This time";
  private static final String OLDLOG_NOT_THIS_TIME ="Not this time";
  private static final String OLDLOG_ALWAYS        ="Always";
  private static final String OLDLOG_NEVER         ="Never";
  private static final String[] LOG_LVL=new String[] {"Error", "Warning", "Info", "Debug"};
  
  private String oldLogChoice=OLDLOG_THIS_TIME;
  private Hashtable ht_connectedGateway=new Hashtable(); // connString/mbsc
  protected Hashtable ht_logLvl=new Hashtable(); // DefaultMutableTreeNode/Integer_logLvl
  protected Vector v_ul=new Vector(); // tree node containing not visible log yet  (placer ce vecteur dans le renderer ???)

  private MyTree logTree;
  private TreePath selPath;
  private JPanel jp;
  private DefaultMutableTreeNode rootNode=new DefaultMutableTreeNode("");
  private JScrollBar jsb_horizontal=null;
  private JScrollBar jsb_vertical=null;

  public RemoteLogger_jtree (BundleContext bdlCtx){
    super(null);
    setRoot(rootNode);

    this.jp=new JPanel();
    this.jp.setLayout(new BorderLayout());
   
    this.logTree=new MyTree(this);
    JtreeCellRenderer treeCellRenderer=new JtreeCellRenderer(bdlCtx, this);
    this.logTree.setCellRenderer(treeCellRenderer);
    this.logTree.setLargeModel(true);
    this.logTree.setToggleClickCount(-1); 
    this.logTree.setScrollsOnExpand(false);
    // if I do this.logTree.setRootVisible(false) => Create an invisible tree, even if I use an "expand"
    // then need to expand after the first insert into the tree so i give up with root not visible.
    this.logTree.addMouseListener(this);

    JScrollPane jsp=new JScrollPane(logTree);
    this.jsb_horizontal=jsp.getHorizontalScrollBar();
    this.jsb_vertical=jsp.getVerticalScrollBar();
    jp.add(jsp, BorderLayout.CENTER);    
    jp.setMinimumSize(new Dimension(500,25));
  }

  /////////////////////////////////////////////////////
  //           Mouse Listener Interface              //
  /////////////////////////////////////////////////////
  public void mouseEntered(MouseEvent e){}
  public void mouseClicked(MouseEvent e) {}
  public void mouseExited(MouseEvent e){}
  public void mouseReleased(MouseEvent e){} 
  public void mousePressed(MouseEvent e) {
    final int selRow = logTree.getRowForLocation(e.getX(), e.getY());
    selPath = logTree.getPathForLocation(e.getX(), e.getY());

    if ( e.getClickCount()==1 & selRow!=-1 & e.getButton()>1 ) { // show JPopupMenu
      String nodeString="\""+((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject()+"\"";
      JPopupMenu jpopup=new JPopupMenu();
      JMenuItem jmiRemove=new JMenuItem("Remove logs \""+nodeString.substring(0,Math.min(15,nodeString.length()))+((nodeString.length()>15)?"...\"":"\""));
      jmiRemove.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e){
	  removeLog_actionPerformed(selRow);
        }	
      });
      jpopup.add(jmiRemove);
      if (selPath.getPath().length==3) {
        JMenuItem jmiLogLvl=new JMenuItem("Set log level");
        jmiLogLvl.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e){
            setLogLvl(selPath);
          }
        });
        jpopup.add(jmiLogLvl);
      }
      jpopup.show(jp, e.getX()-jsb_horizontal.getValue(), e.getY()-jsb_vertical.getValue());
    /*} else if ( e.getClickCount()==2 & selPath!=null) { // expand selected path
      if (logTree.isExpanded(selPath)) {
        logTree.collapsePath(selPath);
      } else {
        logTree.expandPath(selPath);
      } */
    } else if ( e.getClickCount()==1 & selPath!=null ) { // reload logTree and let selected path location
      reloadTree(selRow);
    }
  }
  
  private void reloadTree(int selRow) {
    int horizontal_jsb_init_value = jsb_horizontal.getValue();
    int vertical_jsb_init_value = jsb_vertical.getValue();
    int row_y_init_loc = (int) ((logTree.getRowBounds(selRow)).getY());
    Enumeration enu = logTree.getExpandedDescendants(new TreePath(rootNode));
    reload();
    if ( enu != null ) { // necessaire ce test ?
      while (enu.hasMoreElements()) {
        logTree.expandPath((TreePath) enu.nextElement());
      }
    }
    // Redefini tous les noeuds rmiport/profilName comme a jour
    this.v_ul.removeAllElements();
    // without next line if scrollbar_value=scrollbar_max it's generate a bad shift (may be vertical scrollbar height)
    logTree.scrollPathToVisible(selPath);
    int row_y_new_loc = (int) ((logTree.getRowBounds(logTree.getRowForPath(selPath))).getY());
    int vertical_jsb_new_value = vertical_jsb_init_value+(row_y_new_loc-row_y_init_loc);
    jsb_vertical.setValue(vertical_jsb_new_value);
    jsb_horizontal.setValue(horizontal_jsb_init_value);
    logTree.setSelectionPath(selPath);
    logTree.repaint();
  }

  /////////////////////////////////////
  //        Plugin Interface         //
  /////////////////////////////////////
  public String getName(){
    return "JTree Remote Logger";
  }
  public Component getGUI(){
    return this.jp;
  }

  /* a supprimer si on enleve l'heritage CommonPlugin -> Plugin */
  public String pluginLocation(){
    return null;
  }
  public void registerServicePlugin(){}
  public void unregisterServicePlugin(){}
  /* fin a supprimer */
 
  public void propertyChange(PropertyChangeEvent e){
    if (e.getPropertyName().equals(Plugin.NEW_NODE_CONNECTION)){
      try {
        MBeanServerConnection mbsc=(MBeanServerConnection)e.getNewValue();
	if ( !ht_connectedGateway.containsValue(mbsc) ) {
	  String jmxsurl = (String) e.getOldValue();
	  mbsc.addNotificationListener(Activator.REMOTE_LOGGER_ON, this, null, jmxsurl);
	  // At gateway connection time : add into the tree a "port/profileName" node under an "ip" node
	  String ref = jmxsurl.substring(jmxsurl.lastIndexOf(":")+1);
	  String ip_tmp = jmxsurl.substring(0, jmxsurl.lastIndexOf(":"));
	  String ip = ip_tmp.substring(ip_tmp.lastIndexOf("/")+1);
	  String connString = jmxsurl.substring(ip_tmp.lastIndexOf("/")+1);
	  ht_connectedGateway.put(connString, mbsc);
          DefaultMutableTreeNode dmtn_ip=createIfNeed(ip, rootNode);
          DefaultMutableTreeNode dmtn_ref=createIfNeed(ref, dmtn_ip);
          Integer lL = this.getLogLvl(connString);
          ht_logLvl.put(dmtn_ref, lL);
          // ask for old log management choice :
          if (oldLogChoice==OLDLOG_THIS_TIME | oldLogChoice==OLDLOG_NOT_THIS_TIME) {
            JOptionPane jop = new JOptionPane("Do you want old log from gateway :\n"+jmxsurl+" ?", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[] {OLDLOG_THIS_TIME, OLDLOG_NOT_THIS_TIME, OLDLOG_ALWAYS, OLDLOG_NEVER}, OLDLOG_THIS_TIME);
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
            mbsc.invoke(Activator.REMOTE_LOGGER_ON, "sendOldLog", new Object[]{}, new String[]{});
	  }
        }
      } catch(Exception ex){
        System.out.println("[RemoteLogger_jtree] error : "+ex);
      }
    }
  }
 
  private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
  private static final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
  ///////////////////////////////////////////////////
  //       NotificationListener implementation     //  
  ///////////////////////////////////////////////////
  public void handleNotification(Notification notification, Object handback) {
    String jmxsurl = handback.toString();
    String ref = jmxsurl.substring(jmxsurl.lastIndexOf(":")+1);
    String ip_tmp = jmxsurl.substring(0, jmxsurl.lastIndexOf(":"));
    String ip = ip_tmp.substring(ip_tmp.lastIndexOf("/")+1);
  
    StringTokenizer st = new StringTokenizer(notification.getMessage(), "*");
    long ts=notification.getTimeStamp();
    String time=JtreeCellRenderer.UNKNOWN_TIME;
    String date=JtreeCellRenderer.UNKNOWN_DATE;
    if (ts!=0) {
      Date timeDate=new Date(ts);
      // if I use local date format there are indentations problems
      //DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
      //DateFormat df2 = DateFormat.getDateInstance(DateFormat.SHORT);
      time=timeFormat.format(timeDate);
      date=dateFormat.format(timeDate);
    }
    String id=st.nextToken();
    String name=st.nextToken();
    String idname=new String(id+" : "+name);
    // bundle state juste after remote loger received a the log entry (in old log case do state="")
    String state=(String) JtreeCellRenderer.ht_num2string.get(new Integer((int) Integer.parseInt(st.nextToken())));
    String lvl=st.nextToken();
    String msg=st.nextToken();
    // Get and maybe create parents nodes : ip / ref / idname
    DefaultMutableTreeNode dmtn_ip=createIfNeed(ip, rootNode);
    DefaultMutableTreeNode dmtn_ref=createIfNeed(ref, dmtn_ip);
    DefaultMutableTreeNode dmtn_idname=createIfNeed(idname, dmtn_ref);
    // insert the leaf with message under id/ref/idname
    DefaultMutableTreeNode dmtn=new DefaultMutableTreeNode(time+" | "+date+" | "+state+" | "+lvl+" | "+msg,false);
    this.insertNodeInto(dmtn, dmtn_idname, 0);
    // if usefull save nodes which contains new log
    if ( !v_ul.contains(dmtn_ip) ) {
	v_ul.add(dmtn_ip);
	v_ul.add(dmtn_ref);
	v_ul.add(dmtn_idname);
    } else if ( !v_ul.contains(dmtn_ref) ) {
	v_ul.add(dmtn_ref);
	v_ul.add(dmtn_idname);
    } else if ( !v_ul.contains(dmtn_idname) ) {
	v_ul.add(dmtn_idname);
    }
    this.logTree.repaint();
  }

  //////////////////////////////////////////
  //     MBean attribute manipulation     //
  //////////////////////////////////////////
  private Integer getLogLvl(String connString) {
    Integer val=new Integer(0);
    try {
      MBeanServerConnection mb=(MBeanServerConnection) ht_connectedGateway.get(connString);
      val=(Integer) mb.getAttribute(Activator.REMOTE_LOGGER_ON, "LogLvl");
    } catch (Exception exc) {
      exc.printStackTrace();
    }
    return val;
  }

  private void setLogLvl(TreePath tp) {
    Object[] o=tp.getPath();
    String connString = o[1]+":"+o[2];
    try {
      MBeanServerConnection mb=(MBeanServerConnection) ht_connectedGateway.get(connString);
      Integer curentVal=(Integer) mb.getAttribute(Activator.REMOTE_LOGGER_ON, "LogLvl");

      int val = JOptionPane.showOptionDialog(jp, "Select a log level for \"..."+connString+"\" :", "Log level", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, LOG_LVL, LOG_LVL[curentVal.intValue()-1]);
      if ( val == JOptionPane.CLOSED_OPTION ) { return; }
      Integer newVal = new Integer(val+1);

      mb.setAttribute(Activator.REMOTE_LOGGER_ON, new Attribute("LogLvl", newVal));
      DefaultMutableTreeNode ddmmttnn=(DefaultMutableTreeNode) tp.getLastPathComponent();
      ht_logLvl.put(ddmmttnn, newVal);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(jp,"Error with \"..."+connString+"\" :\n"+ex, "Error :", JOptionPane.ERROR_MESSAGE);
      ex.printStackTrace();
    }
  }

  //////////////////////////////////////////
  //         PRIVATE TOOLS                //
  //////////////////////////////////////////
  private void removeLog_actionPerformed(int selRow) {
    //System.out.println("selected path="+this.selPath);
    Object[] o= this.selPath.getPath();
    DefaultMutableTreeNode selectedDmtn=(DefaultMutableTreeNode) this.selPath.getLastPathComponent();
    if (o.length==5) {
      // Can't remove first child of a bundle to avoid modify tree node color
      if ( ((DefaultMutableTreeNode) selectedDmtn.getParent()).getFirstChild()!=selectedDmtn ) {
        removeNodeFromParent(selectedDmtn);
      }
    } else if (o.length==4) {
      removeNodeFromParent(selectedDmtn);
    } else if (o.length==3) {
      selectedDmtn.removeAllChildren();
      reloadTree(selRow);
    } else if (o.length==2) {
      Enumeration enu_1=selectedDmtn.children();
      while (enu_1.hasMoreElements()) {
        DefaultMutableTreeNode dmtn_child=(DefaultMutableTreeNode) enu_1.nextElement();
	dmtn_child.removeAllChildren();
      }
      reloadTree(selRow);
    } else if (o.length==1) {
      Enumeration enume=rootNode.children();
      while (enume.hasMoreElements()) {
        DefaultMutableTreeNode dmtn_child=(DefaultMutableTreeNode) enume.nextElement();
	Enumeration enume2=dmtn_child.children();
        while (enume2.hasMoreElements()) {
          DefaultMutableTreeNode dmtn_child_child=(DefaultMutableTreeNode) enume2.nextElement();
	  dmtn_child_child.removeAllChildren();
	}
      }
      reloadTree(selRow); 
    }
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
      if ( parent==rootNode ) {
        v_ul.add(rootNode);
      }
    }
    
    return theNode;
  }

  protected String getLogLvl(DefaultMutableTreeNode dmtn) {
    // used by treeCellRenderer
    return LOG_LVL[ ((Integer) ht_logLvl.get(dmtn)).intValue() - 1 ];
  }
 
  protected void fireTreeNodesInserted(Object source, Object path[], int childIndices[], Object children[]) {
    // Do nothing to avoid refresh jtree after each log.
  }

}
