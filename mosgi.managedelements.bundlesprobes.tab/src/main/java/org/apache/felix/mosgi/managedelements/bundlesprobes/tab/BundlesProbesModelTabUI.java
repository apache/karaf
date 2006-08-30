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
package org.apache.felix.mosgi.managedelements.bundlesprobes.tab;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;
import javax.management.Notification;
import javax.management.NotificationListener;

import javax.swing.table.DefaultTableModel;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public class BundlesProbesModelTabUI extends DefaultTableModel implements NotificationListener  { 
  private static final String OSGI_ON="TabUI:name=BundlesProbes";

  private MBeanServerConnection mbsc = null;
  private Hashtable eventName=new Hashtable();
  private ObjectName osgiON=null;

  public BundlesProbesModelTabUI() throws Exception{
    super(new String[]{"Id", "State", "Location"}, 1);
    eventName.put(new Integer(Bundle.UNINSTALLED), "UNINSTALLED");
    eventName.put(new Integer(Bundle.INSTALLED), "INSTALLED");
    eventName.put(new Integer(Bundle.RESOLVED), "RESOLVED");
    eventName.put(new Integer(Bundle.STARTING), "STARTING");
    eventName.put(new Integer(Bundle.STOPPING), "STOPPING");
    eventName.put(new Integer(Bundle.ACTIVE), "ACTIVE");
    osgiON=new ObjectName(OSGI_ON);
  }

  ///////////////////////////////
  //   DefaultTableModel       //
  ///////////////////////////////
  public boolean isCellEditable(int row, int col){
    return false;
  }

  private void removeAll(){
    int size=this.getRowCount();
    if (size!=0){
      for (int i=0; i<size;i++){
        super.removeRow(0);
      }
      super.fireTableRowsDeleted(0, size-1);
    }
  }

  private void insertRows() throws Exception{
    Vector bundleList= (Vector) mbsc.invoke(this.osgiON, "bundleList", null, null);
    Iterator iterator = bundleList.iterator();
    this.removeAll();
    int i=0;
    while (iterator.hasNext()) {
      Vector vector = (Vector) iterator.next();
      this.addRow(vector);
      i++;
    }
    super.fireTableRowsInserted(0, i-1);
  }

  /////////////////////////////////////////////
  //        NotificationListener             //
  /////////////////////////////////////////////
  public void handleNotification(Notification notification, Object handback) {
    boolean found=false;
    StringTokenizer st=new StringTokenizer(notification.getMessage(), ":");
    String eventClassName=st.nextToken();
    long id=Long.parseLong(st.nextToken());
    int type=Integer.parseInt(st.nextToken());
    String bundleName=st.nextToken();

System.out.println("))"+id+":"+type+":"+ this.eventName.get(new Integer(type)));
    if (eventClassName.equals(BundleEvent.class.getName())){
      int row=0;
      for (; row<this.getRowCount(); row++) {
        if (((Long)this.getValueAt(row, 0)).longValue()==id) {
          found=true;
          break;
        }
      }
      if (type==Bundle.INSTALLED && !found){
        super.addRow(new Object[]{new Long(id), this.eventName.get(new Integer(type)), bundleName});
        super.fireTableRowsInserted(row, row);
      }else {
        super.setValueAt(this.eventName.get(new Integer(type)), row, 1);
        super.fireTableRowsUpdated(row, row);

      }

    }
  }

  ////////////////////////////////////////////////
  //          Main Class                       //
  ////////////////////////////////////////////////
  public void createBundleList(MBeanServerConnection conn) throws Exception {
    this.mbsc=conn;
    mbsc.addNotificationListener(this.osgiON, this, null, null);
    this.insertRows();
  }

  public void emptyPanel(){
    if (this.mbsc!=null){
      try{
        mbsc.removeNotificationListener(this.osgiON, this);
        this.mbsc=null;
        this.removeAll();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

	public void installButtonAction(String text) throws Exception {
System.out.println("=>"+text);
		mbsc.invoke(this.osgiON, "install", new Object[] {text}, new String[]{String.class.getName()});
	}

	public void startButtonAction(Long id) throws Exception {
		mbsc.invoke(this.osgiON, "startService", new Object[] {new Long [] {id}} , new String[]{new ArrayType(1, SimpleType.LONG).getTypeName()});
	}

	public void stopButtonAction(Long id) throws Exception {
		mbsc.invoke(this.osgiON, "stopService",new Object[] {new Long [] {id}} , new String[]{new ArrayType(1, SimpleType.LONG).getTypeName()});
	}

	public void updateButtonAction(Long id) throws Exception {
		mbsc.invoke(this.osgiON, "update", new Object[] {new Long [] {id}} , new String[]{new ArrayType(1, SimpleType.LONG).getTypeName()});
	}

	public void refreshButtonAction() throws Exception {
    this.insertRows();
	}

	public void uninstallButtonAction(Long id) throws Exception {
		mbsc.invoke(this.osgiON, "uninstall", new Object[] {new Long [] {id}} , new String[]{new ArrayType(1, SimpleType.LONG).getTypeName()});
	}

}
