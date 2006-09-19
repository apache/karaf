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
package org.apache.felix.mosgi.managedelements.bundlesprobes.tab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.management.MBeanServerConnection;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import org.apache.felix.mosgi.console.ifc.Plugin;

import java.beans.PropertyChangeEvent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class BundlesProbesTabUI extends JPanel implements BundleActivator, ActionListener, Plugin {
  private JTable table;

  private JTextField urlTextField;
  private JButton installButton;
  private JButton startButton;
  private JButton stopButton;
  private JButton updateButton;
  private JButton refreshButton;
  private JButton uninstallButton;

  private BundlesProbesModelTabUI panelModel = null;
  private BundleContext m_context=null;
  private ServiceRegistration sreg = null;
  private MBeanServerConnection mbsc=null;

  ///////////////////////////////////////////
  //           BundleActivator             //
  ///////////////////////////////////////////
  public void start(BundleContext context) {
    m_context = context;
    this.registerServicePlugin();
  }
  
  public void stop(BundleContext context) {
  }

  ///////////////////////////////////////////////
  //               Plugin                      //
  ///////////////////////////////////////////////
  public void registerServicePlugin(){
    sreg = m_context.registerService(Plugin.class.getName(), this, null);
  }

  public void unregisterServicePlugin(){
    sreg.unregister();
  }

  public String pluginLocation(){
    return m_context.getBundle().getLocation();
  }
  public String getName(){ return "Bundles List"; }
  public Component getGUI(){ return this; }
  public void propertyChange(PropertyChangeEvent ee){
    String action=ee.getPropertyName();
    if (action.equals(Plugin.NEW_NODE_READY)){
      this.mbsc=(MBeanServerConnection)ee.getNewValue();
    }else if(action.equals(Plugin.EMPTY_NODE)){
      panelModel.emptyPanel();
      this.mbsc=null;
    }else if(action.equals(Plugin.PLUGIN_ACTIVATED) && ee.getNewValue().equals(this.getName())){
      try {
        panelModel.createBundleList(this.mbsc);
        invalidate();
        validate();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public BundlesProbesTabUI() throws Exception {
    panelModel = new BundlesProbesModelTabUI();
    setLayout(new BorderLayout());
    add(createInstallPanel(), BorderLayout.NORTH);
    add(createTablePanel(), BorderLayout.CENTER);
    add(createMgmntButtonsPanel(), BorderLayout.SOUTH);
  }

  private void initColumnSizes(JTable table) {
    TableColumn column = null;
    for (int i = 0; i < 3; i++) {
      column = table.getColumnModel().getColumn(i);
      if ((i == 0) || (i == 1))
        column.setPreferredWidth(5);
      if (i == 2)
        column.setPreferredWidth(200);
    }
  }

  private JPanel createInstallPanel() {
    JPanel installPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    installPanel.setBorder(BorderFactory.createEtchedBorder());
    installPanel.add(new JLabel("URL: "), BorderLayout.WEST);

    urlTextField = new JTextField(35);
    urlTextField.addActionListener(this);
    installPanel.add(urlTextField, BorderLayout.CENTER);

    installButton = new JButton("Install");
    installButton.setMnemonic('I');
    installButton.addActionListener(this);
    installPanel.add(installButton, BorderLayout.EAST);
    return installPanel;
  }

  private JScrollPane createTablePanel() {
    table = new JTable(panelModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getTableHeader().setReorderingAllowed(false);
    initColumnSizes(table);
    JScrollPane tablePanel = new JScrollPane(table);
    tablePanel.setBorder(BorderFactory.createEtchedBorder());
    return tablePanel;
  }

  private JPanel createMgmntButtonsPanel() {
    JPanel mgmntButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    mgmntButtonsPanel.setBorder(BorderFactory.createEtchedBorder());
    startButton = new MyButton('S', "Start", this, mgmntButtonsPanel);
    stopButton = new MyButton('p', "Stop", this, mgmntButtonsPanel);
    updateButton = new MyButton('a', "Update", this, mgmntButtonsPanel);
    refreshButton = new MyButton('R', "Refresh", this, mgmntButtonsPanel);
    uninstallButton = new MyButton('U', "Uninstall", this, mgmntButtonsPanel);
    return mgmntButtonsPanel;
  }

  public void actionPerformed(ActionEvent e) {
    Object object = e.getSource();
    if ((object == installButton) || (object == urlTextField)) {
      try {
        String jarPath = urlTextField.getText();
        jarPath = jarPath.trim();
        panelModel.installButtonAction(jarPath);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Install Error", JOptionPane.ERROR_MESSAGE);
      }
    } else if (object == startButton) {
      try {
        panelModel.startButtonAction((Long) (table.getValueAt(table
            .getSelectedRow(), 0)));
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Start Error", JOptionPane.ERROR_MESSAGE);
      }
    } else if (object == stopButton) {
      try {
        panelModel.stopButtonAction((Long) (table.getValueAt(table
            .getSelectedRow(), 0)));
      } catch (Exception ex) {
        /*
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Stop Error", JOptionPane.ERROR_MESSAGE);
            */
        ex.printStackTrace();
      }
    } else if (object == updateButton) {
      try {
        panelModel.updateButtonAction((Long) (table.getValueAt(table
            .getSelectedRow(), 0)));
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Update Error", JOptionPane.ERROR_MESSAGE);
      }
    } else if (object == refreshButton) {
      try {
        panelModel.refreshButtonAction();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Refresh Error", JOptionPane.ERROR_MESSAGE);
      }
    } else if (object == uninstallButton) {
      try {
        panelModel.uninstallButtonAction((Long) (table.getValueAt(table
            .getSelectedRow(), 0)));
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getClass().getName(),
            "Uninstall Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

static class MyButton extends JButton {
    private int W = 85;
    private int H = 25;

    public MyButton(char c, String name, BundlesProbesTabUI listener, JPanel panel) {
      super.setText(name);
      super.setMnemonic(c);
      super.setMinimumSize(new Dimension(W, H));
      super.setPreferredSize(new Dimension(W, H));
      super.setMaximumSize(new Dimension(W, H));
      super.addActionListener(listener);
      panel.add(this);
    }
  }
}
