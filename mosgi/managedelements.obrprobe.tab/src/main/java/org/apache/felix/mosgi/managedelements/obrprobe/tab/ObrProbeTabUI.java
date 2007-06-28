/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.managedelements.obrprobe.tab;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Vector;
import java.util.StringTokenizer;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.PrintStream;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.InvalidSyntaxException;

//import org.apache.felix.bundlerepository.BundleRepository;
//import org.apache.felix.bundlerepository.BundleRecord;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.Resource;


import javax.management.openmbean.ArrayType;
import javax.management.openmbean.SimpleType;

import org.apache.felix.mosgi.console.ifc.Plugin;

public class ObrProbeTabUI extends JPanel implements Plugin, BundleActivator {
  private static final String OSGI_ON="TabUI:name=ObrProbe";

  private MBeanServerConnection mbsc=null;

  private BundleContext m_context = null;
  private ServiceReference m_brsRef = null;
  private RepositoryAdmin m_brs = null;

  private JTextField m_urlField = null;
  private JButton m_refreshButton = null;
  private JTable m_bundleTable = null;
  private JButton m_startAllButton = null;
  private JButton m_infoButton = null;

  private ObjectName osgiON=null;

  private PrintStream m_out=System.out;
  private ServiceRegistration sreg=null;

  ///////////////////////////////////////////
  //           BundleActivator             //
  ///////////////////////////////////////////
  public void start(BundleContext context) throws BundleException {
    m_context = context;
    try {
      this.init();
    }catch(Exception e){
      e.printStackTrace();
      throw new BundleException("ObrTabUIException "+e.getMessage());
    }
    this.registerServicePlugin();
  }

  public void stop(BundleContext context) {
  }

  

  ///////////////////////////////////////////
  //           Plugin                      //
  //////////////////////////////////////////
  public void registerServicePlugin(){
    sreg = m_context.registerService(Plugin.class.getName(), this, null);
  }
  
  public void unregisterServicePlugin(){
    sreg.unregister();   
  }
  
  public String pluginLocation(){
    return m_context.getBundle().getLocation();
  }
  
  public String getName(){return "Remote OBR";}

  public Component getGUI(){return this;}

  public void propertyChange(PropertyChangeEvent e){
//System.out.println("TEST"+e.getPropertyName());	
   if (e.getPropertyName().equals(Plugin.NEW_NODE_READY)){
     this.mbsc=(MBeanServerConnection)e.getNewValue();
//System.out.println("TESTE"+this.mbsc);	
   }else if(e.getPropertyName().equals(Plugin.EMPTY_NODE)){
     this.mbsc=null;
   }
  }

  /////////////////////
  // Plugin elements //
  /////////////////////
  private void init() throws Exception {
    this.osgiON=new ObjectName(OSGI_ON);
    ServiceListener sl = new ServiceListener() {
      public void serviceChanged(ServiceEvent event) {
        synchronized (ObrProbeTabUI.this) {
          // Ignore additional services if we already have one.
          if ((event.getType() == ServiceEvent.REGISTERED)
              && (m_brsRef != null)) {
            return;
          }
          // Initialize the service if we don't have one.
          else if ((event.getType() == ServiceEvent.REGISTERED)
            && (m_brsRef == null)) {
            initializeService();
          }
          // Unget the service if it is unregistering.
          else if ((event.getType() == ServiceEvent.UNREGISTERING)
            && event.getServiceReference().equals(m_brsRef)) {
            m_context.ungetService(m_brsRef);
            m_brsRef = null;
            m_brs = null;
            // Try to get another service.
            initializeService();
          }
        }
      }
    };
    try {
      m_context.addServiceListener(sl, "(objectClass=" + RepositoryAdmin.class.getName() + ")");
    } catch (InvalidSyntaxException ex) {
      System.err.println("OBRPlugin: " + ex);
    }

    // Create the gui.
    createUserInterface();

    // Now try to manually initialize the shell service
    // since one might already be available.
    initializeService();
  }

  private synchronized void initializeService() {
    if (m_brs != null) {
      return;
    }
    m_brsRef = m_context.getServiceReference(RepositoryAdmin.class.getName());
    if (m_brsRef == null) {
      m_urlField.setText("");
    } else {
      m_brs = (RepositoryAdmin) m_context.getService(m_brsRef);
      m_urlField.setText(convertArrayToString(m_brs.listRepositories()));
    }
    // Update the table.
    ((SimpleTableModel) m_bundleTable.getModel()).update();
  }

  private void createUserInterface() {
    setLayout(new BorderLayout());
    add(createBRUrlPanel(), BorderLayout.NORTH);
    add(createTable(), BorderLayout.CENTER);
    add(createButtonPanel(), BorderLayout.SOUTH);
    createEventListeners();
  }

  private JPanel createBRUrlPanel(){
    JPanel panel=new JPanel(new FlowLayout());
    panel.add(new JLabel("URL(s)"));
    panel.add(m_urlField = new JTextField(20));
    panel.add(m_refreshButton = new JButton("Refresh"));
    m_refreshButton.setMnemonic('I');
    return panel;
  }

  private JScrollPane createTable() {
    JScrollPane scroll = new JScrollPane(m_bundleTable = new JTable());
    scroll.setPreferredSize(new Dimension(100, 100));
    m_bundleTable.setMinimumSize(new Dimension(0, 0));
    m_bundleTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    m_bundleTable.setModel(new SimpleTableModel());
    m_bundleTable.getColumnModel().getColumn(0).setPreferredWidth(75);
    m_bundleTable.getColumnModel().getColumn(1).setPreferredWidth(75);
    m_bundleTable.getColumnModel().getColumn(2).setPreferredWidth(200);
    return scroll;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout());
    panel.add(m_startAllButton = new JButton("Start all"));
    panel.add(m_infoButton = new JButton("Info"));
    m_startAllButton.setMnemonic('S');
    m_infoButton.setMnemonic('I');
    return panel;
  }

  private void createEventListeners() {
    // Create action listeners.
    m_refreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
    /*
        synchronized (ObrProbeTabUI.this) {
          if (m_brs == null) {
            return;
          } else if (m_urlField.getText().length() > 0) {
            m_brs.setRepositoryURLs(convertStringToArray(m_urlField.getText()));
          } else {
            m_urlField.setText(convertArrayToString(m_brs.getRepositoryURLs()));
          }
          // Update the table.
          ((SimpleTableModel) m_bundleTable.getModel()).update();
        }
    */
      }
    });

    m_startAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        synchronized (ObrProbeTabUI.this) {
         if (mbsc == null) {
System.out.println("coucou");    
           return;
         }
         int[] rows = m_bundleTable.getSelectedRows();
         for (int i = 0; i < rows.length; i++) {
       //Object [] tabo=new Object[]{(String) m_brs.getBundleRecord(rows[i]).getAttribute(BundleRecord.BUNDLE_UPDATELOCATION)};
           try{
         /*
             BundleRecord[] m_records=m_brs.getBundleRecords();
             Object [] tabo=new Object[]{(String) m_records[rows[i]].getAttribute(BundleRecord.BUNDLE_SYMBOLICNAME), parseVersionString((String) m_records[rows[i]].getAttribute(BundleRecord.BUNDLE_VERSION))};
             String [] tabc=new String[]{String.class.getName(),new ArrayType(1, SimpleType.INTEGER).getTypeName()};
         */
             Object [] tabo=new Object[]{(String)m_bundleTable.getValueAt(rows[i], 0), (String)m_bundleTable.getValueAt(rows[i], 1)};
         String [] tabc=new String[]{String.class.getName(),String.class.getName()};
System.out.println("====>"+m_bundleTable.getValueAt(rows[i], 0));
System.out.println("====>"+m_bundleTable.getValueAt(rows[i], 1));
             mbsc.invoke(osgiON, "deploy", tabo, tabc);
       }catch(Exception e){
         e.printStackTrace();
       }
         }
         m_out.println("");
        }
      }
      });
   

      m_infoButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
      /*
          synchronized (ObrProbeTabUI.this) {
            if (m_brs == null) {
              return;
            }
            int[] rows = m_bundleTable.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
              if (i != 0) {
                m_out.println("");
              }
              BundleRecord br = m_brs.getBundleRecords()[i];
              br.printAttributes(m_out);
            }
            m_out.println("");
          }
    */
        }
      });
  }

    private String[] convertStringToArray(String str) {
      String[] strs = null;
      StringTokenizer st = new StringTokenizer(str);
      if (st.countTokens() > 0) {
        strs = new String[st.countTokens()];
        for (int i = 0; (strs != null) && (i < strs.length); i++) {
          strs[i] = st.nextToken();
        }
      }
      return strs;
    }

    private String convertArrayToString(Repository[] strs) {
      StringBuffer str = new StringBuffer();
      for (int i = 0; (strs != null) && (i < strs.length); i++) {
        // Add space between URLs.
        if (i != 0) {
          str = str.append(" ");
        }
        str.append(strs[i].getURL());
      }
      return str.toString();
    }

    public static Integer[] parseVersionString(String s) {
      Integer[] version = new Integer[] { new Integer(0), new Integer(0), new Integer(0) };
      if (s != null) {
        StringTokenizer st = new StringTokenizer(s, ".");
        if (st.hasMoreTokens()) {
          try {
            version[0] = new Integer(st.nextToken());
            if (st.hasMoreTokens()) {
              version[1] = new Integer(st.nextToken());
              if (st.hasMoreTokens()) {
                version[2] = new Integer(st.nextToken());
              }
            }
            return version;
          } catch (NumberFormatException ex) {
            throw new IllegalArgumentException( "Improper version number.");
          }
        }
      }
     return version;
    }

    private class SimpleTableModel extends AbstractTableModel {
      public int getRowCount() { 
        if (m_brs==null){
          return 0;
    }else{
      try {
        Resource[] resources = m_brs.discoverResources("(|(presentationname=*)(symbolicname=*))");
            return resources.length;
      }catch(Exception e){
        System.out.println("OBR not working, network problem ?");
        //e.printStackTrace();
        return 0;
      }
    }
      }

      public int getColumnCount() {
        return 3;
      }

      public String getColumnName(int column) {
        if (column == 0) {
          return "Name";
        } else if (column == 1) {
          return "Version";
        } else if (column == 2) {
          return "Description";
        }    
        return "";
      }

      public Class getColumnClass(int column) {
        return String.class;
      }

      public boolean isCellEditable(int row, int column) {
        return false;
      }

      public Object getValueAt(int row, int column) {
        Resource br = null;
        synchronized (ObrProbeTabUI.this) {
          if (m_brs != null) {
        br=m_brs.discoverResources("(|(presentationname=*)(symbolicname=*))")[row];
          }
        }
        if (br != null) {
          if (column == 0) {
            return br.getPresentationName();
          } else if (column == 1) {
            return br.getVersion().toString();
          } else if (column == 2) {
            return br.getSymbolicName();
          }
        }
        return null;
      }
                
      public void update() {
        fireTableDataChanged();
      }
    }
  }
