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
//import org.apache.felix.mosgi.console.component.JtableCellRenderer;

import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.swing.ListSelectionModel;

import java.util.Hashtable;
import java.util.StringTokenizer;

import java.beans.PropertyChangeEvent;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;

import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.JFileChooser;
		
import java.util.Date;
import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

public class RemoteLogger_jtable extends DefaultTableModel implements CommonPlugin, NotificationListener{

  private JTable logList;
  private JButton jb_save;
  private JPanel jbPanel;
  private JPanel jp;
  private Hashtable nodes=new Hashtable();

  public void jb_actionPerformed(ActionEvent e) {
    String compoName=((Component) e.getSource()).getName();
	
    if (compoName.equals("jb_save")){
      PrintStream ps=System.out;
      JFileChooser jfc=new JFileChooser();
      if (jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION & jfc.getSelectedFile()!=null){	
      try{
        ps=new PrintStream(jfc.getSelectedFile());
        System.out.println("Save remote log into \""+jfc.getSelectedFile().getName()+"\""); }
      catch (FileNotFoundException fnfe){
        System.out.println("err : "+fnfe); }
      }
      int col=this.logList.getColumnCount(); // TODO : try this.getColumnCount()
      int line=this.logList.getRowCount() - 1; // last line always empty
      //ps.print(col+" "+line);
      Vector tableData=new Vector();
      tableData=this.getDataVector();
      for (int i=0 ; i<line ; i++){
        ps.print(i+" : ");
        for (int j=0 ; j<col ; j++){
	  ps.print((String) ( ((Vector) (tableData.elementAt(i))).elementAt(j) )+" | ");	
        }
        ps.print("\n");
      }
    }
  }

  public RemoteLogger_jtable (){
    super(new String[]{"Date","Time", "Src", "Id", "Name", "State", "Lvl", "Msg"},1);
    System.out.println("JTable Remote logger");
   
    this.jp=new JPanel();
    this.jp.setLayout(new BorderLayout());
   
    this.jbPanel=new JPanel();
    this.jbPanel.setSize(300,25);

    this.jb_save=new JButton("Save log on file");
    this.jb_save.setName("jb_save");

    ActionListener al = new ActionListener(){
        public void actionPerformed(ActionEvent e){
            jb_actionPerformed(e);
        }
    };
    this.jb_save.addActionListener(al);

    logList=new JTable(this);
    JtableCellRenderer cellRenderer=new JtableCellRenderer();
    logList.setDefaultRenderer(Object.class,cellRenderer);

    logList.setPreferredScrollableViewportSize(new java.awt.Dimension(600, 70));
    
    logList.getColumnModel().getColumn(0).setPreferredWidth(50);
    logList.getColumnModel().getColumn(1).setPreferredWidth(40);
    logList.getColumnModel().getColumn(2).setPreferredWidth(120);
    logList.getColumnModel().getColumn(3).setPreferredWidth(15);
    logList.getColumnModel().getColumn(4).setPreferredWidth(70);
    logList.getColumnModel().getColumn(5).setPreferredWidth(15);
    logList.getColumnModel().getColumn(6).setPreferredWidth(40);
    logList.getColumnModel().getColumn(7).setPreferredWidth(180);    

    logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    logList.getTableHeader().setReorderingAllowed(true);//false); // true c'est sympa pourtant... ?
    
    this.jbPanel.add(jb_save);
    jp.add(this.jbPanel, BorderLayout.NORTH);
    jp.add(new JScrollPane(logList), BorderLayout.CENTER);    
  }

  /////////////////////////////////////
  //  Plugin Interface ////////////////
  /////////////////////////////////////
  public String getName(){ return "JTable Remote Logger";}
  public Component getGUI(){return this.jp;}

/* a supprimer si on enleve l'heritage CommonPlugin -> Plugin */
  public String pluginLocation(){
    return null;
  }
  public void registerServicePlugin(){}
  public void unregisterServicePlugin(){}
/* fin a supprimer */

  
  public void propertyChange(PropertyChangeEvent e){
    if (e.getPropertyName().equals(Plugin.NEW_NODE_CONNECTION)){
      try{
        MBeanServerConnection mbs=(MBeanServerConnection)e.getNewValue();
        if (nodes.get(mbs)==null){
	  System.out.println("Ajout d'un listener " +mbs);
          ((MBeanServerConnection)e.getNewValue()).addNotificationListener(new ObjectName("OSGI:name=Remote Logger"), this, null, e.getOldValue());
          nodes.put(mbs, "ok");
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }

  public void handleNotification(Notification notification, Object handback) {
    StringTokenizer st = new StringTokenizer(notification.getMessage(),":");
    Date d=new Date(notification.getTimeStamp());
    //DateFormat dateFormat = new SimpleDateFormat("hh'h'mm dd-MM-yy");
    DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM); // utilise le format de date local
    DateFormat df2 = DateFormat.getDateInstance(DateFormat.SHORT);
    String id=st.nextToken();
    String name=st.nextToken();
    String shortName=name.substring(name.lastIndexOf(".")+1,name.length());
    String state=st.nextToken();
    String lvl=st.nextToken();
    String msg=st.nextToken();
    Object [] event = new Object []{df2.format(d),df.format(d),handback,id,shortName,state,lvl,msg};
				    
    this.insertRow(0,event); 
    this.fireTableRowsInserted(0, 0);
  }
  
}
