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
import javax.swing.ListSelectionModel;
import javax.swing.JFileChooser;
import javax.swing.table.JTableHeader;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
//import java.text.SimpleDateFormat;

public class RemoteLogger_jtable extends DefaultTableModel implements CommonPlugin, NotificationListener, ActionListener{

  private JTable logList;
  private JButton jb_save;
  private String[] columnToolTips=new String[]{"","","","","",
          "<html><pre> 1 UNINSTALLED black<br> 2 INSTALLED   red<br> 4 RESOLVED    orange<br> 8 STARTING    gray<br>16 STOPPING    gray<br>32 ACTIVE      green",
	  "",""};
  private JPanel jbPanel;
  private JPanel jp;
  private Hashtable nodes=new Hashtable();

  public RemoteLogger_jtable (){
    super(new String[]{"Date","Time", "Src", "Id", "Name", "State", "Lvl", "Msg"},1);

    jp=new JPanel();
    jp.setLayout(new BorderLayout());
   
    jbPanel=new JPanel();
    jbPanel.setSize(300,25);

    jb_save=new JButton("Save log on file");
    jb_save.setName("jb_save");
    jb_save.addActionListener(this);
    
    logList=new JTable(this){
      protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
          public String getToolTipText(MouseEvent e) {
            String tip = null;
            java.awt.Point p = e.getPoint();
            int index = columnModel.getColumnIndexAtX(p.x);
            int realIndex = columnModel.getColumn(index).getModelIndex();
            return columnToolTips[realIndex];
          }
        };
      }
    };
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
    logList.getTableHeader().setReorderingAllowed(true);//false);
    
    jbPanel.add(jb_save);
    jp.add(jbPanel, BorderLayout.NORTH);
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
	  //System.out.println("Ajout d'un listener " +mbs);
          ((MBeanServerConnection)e.getNewValue()).addNotificationListener(Activator.REMOTE_LOGGER_ON, this, null, e.getOldValue());
          nodes.put(mbs, "ok");
        }
      }catch(Exception ex){
        ex.printStackTrace();
      }
    }
  }

  private static final DateFormat dateF = DateFormat.getDateInstance(DateFormat.SHORT);
  private static final DateFormat timeF = DateFormat.getTimeInstance(DateFormat.MEDIUM);

  public void handleNotification(Notification notification, Object handback) {
    StringTokenizer st = new StringTokenizer(notification.getMessage(),"*");
    
    long ts=notification.getTimeStamp();
    String date=JtreeCellRenderer.UNKNOWN_DATE; //"??/??/??";
    String time=JtreeCellRenderer.UNKNOWN_TIME; //"??/??/??";
    if (ts!=0){ // means it's not an old log
      Date d=new Date(ts);
      //DateFormat dateFormat = new SimpleDateFormat("hh'h'mm dd-MM-yy");
      date = dateF.format(d);
      time = timeF.format(d);
    }
    String id=st.nextToken();
    String name=st.nextToken();
    String shortName=name.substring(name.lastIndexOf(".")+1,name.length());
    String state=st.nextToken();
    String lvl=st.nextToken();
    String msg=st.nextToken();
    Object [] event = new Object []{date,time,handback,id,shortName,state,lvl,msg};
				    
    this.insertRow(0,event); 
    this.fireTableRowsInserted(0, 0);
  }
 
  public void actionPerformed(ActionEvent e) {
    Object o=e.getSource();
    if ( o==jb_save){
      PrintStream ps=System.out;
      JFileChooser jfc=new JFileChooser();
      if (jfc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION & jfc.getSelectedFile()!=null){	
      try{
        ps=new PrintStream(new java.io.FileOutputStream(jfc.getSelectedFile()));
        System.out.println("Save remote log into \""+jfc.getSelectedFile().getName()+"\""); }
      catch (FileNotFoundException fnfe){
        System.out.println("err : "+fnfe); }
      }
      int col=this.logList.getColumnCount();
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

 
}
