/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.upnp.tester;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.osgi.framework.BundleException;

import org.apache.felix.upnp.tester.discovery.DriverProxy;
import org.apache.felix.upnp.tester.discovery.RootDeviceListener;
import org.apache.felix.upnp.tester.gui.LogPanel;
import org.apache.felix.upnp.tester.gui.PropertiesViewer;
import org.apache.felix.upnp.tester.gui.TreeViewer;
import org.apache.felix.upnp.tester.gui.Util;
 
/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ControlPoint implements PopupMenuListener {
	RootDeviceListener listener;
	TreeViewer viewer;
	PropertiesViewer properties;
	JFrame frame;
	
	
	public ControlPoint() {
		frame = new JFrame("Felix UPnPDevice Tester");
       try {
            URL eventIconUrl = Util.class.getResource("IMAGES/logo.gif");           
            ImageIcon icon=  new ImageIcon(eventIconUrl,"logo");
            frame.setIconImage(icon.getImage());
       }
        catch (Exception ex){
                System.out.println("Resource: IMAGES/logo.gif not found : " + ex.toString());
        }
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				try {
					Activator.context.getBundle().stop();
				} catch (BundleException ex) {
					ex.printStackTrace();
				}
			}

		});       
		frame.setBounds(0,0,300,200);
        
        
        doMenuBar(frame);
		doControlPanel();
        Mediator.setControlPoint(this);
        Mediator.setMainFrame(frame);
		listener = new RootDeviceListener();
        Mediator.setRootDeviceListener(listener);
		listener.setDeviceNodeListener(viewer);
		
		frame.pack();
		frame.setVisible(true);
        
		DriverProxy driverProxy = new DriverProxy();
        Mediator.setDriverProxy(driverProxy);
        
		listener.activate();
	}
	
	public void close(){
		listener.deactive();
		frame.dispose();
        Mediator.getDriverProxy().close();           
	}
	
	private void doControlPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		viewer = new TreeViewer();
		viewer.setPreferredSize(new Dimension(180,450));
		properties = new PropertiesViewer();
		Mediator.setPropertiesViewer(properties);
	
		JSplitPane treeSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,viewer,properties);
		JPanel logPanel = new LogPanel();
		logPanel.setPreferredSize(new Dimension(180,100));
		JSplitPane logSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeSplitter,logPanel);
		panel.add(logSplitter);
		frame.getContentPane().add(panel);
		
		
	}
	
      /////////////////////////// MENU /////////////////////////////
    JMenu searchMenu,loggerMenu,cyberMenu;  
    public void doMenuBar(JFrame frame) {

        JMenuBar menuBar = new JMenuBar();
        
        //////////////// FILE
        JMenu file_menu = new JMenu("File");
        file_menu.setMnemonic(KeyEvent.VK_F);

        searchMenu = new JMenu("Search");
        final String ALL_DEVICE = "ssdp:all";
        final String ROOT_DEVICE = "upnp:rootdevice";
        searchMenu.setMnemonic(KeyEvent.VK_L);
        searchMenu.setEnabled(false);
        AbstractAction searchAction = new AbstractAction(){
            public void actionPerformed(ActionEvent e) {
                DriverProxy controller = Mediator.getDriverProxy();
                if (e.getActionCommand().equals(ALL_DEVICE))
                    controller.doSearch(ALL_DEVICE);
                else if (e.getActionCommand().equals(ROOT_DEVICE))
                    controller.doSearch(ROOT_DEVICE);
            }
        };
        
        JMenuItem rootDeviceItem = new JMenuItem("Root Devices");
        rootDeviceItem.setMnemonic(KeyEvent.VK_R);
        rootDeviceItem.addActionListener(searchAction);
        rootDeviceItem.setActionCommand(ROOT_DEVICE);
        searchMenu.add(rootDeviceItem);
        
        JMenuItem allDeviceItem = new JMenuItem("All Devices");
        allDeviceItem .setMnemonic(KeyEvent.VK_A);
        allDeviceItem .addActionListener(searchAction);
        allDeviceItem .setActionCommand(ALL_DEVICE);
        searchMenu.add(allDeviceItem);
        
        
        JMenuItem checkIncompleteItem = new JMenuItem("Print Pending Devices");
        checkIncompleteItem.setMnemonic(KeyEvent.VK_I);
        checkIncompleteItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Mediator.getRootDeviceListener().checkIncompleteDevice();
                }
            });
        
        JMenuItem checkErrataItem = new JMenuItem("Check Errata UPnPDevices");
        checkErrataItem.setMnemonic(KeyEvent.VK_E);
        checkErrataItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {                    
                    RootDeviceListener.checkErrataDevice();}
            });
        
        
        loggerMenu = new JMenu("Felix Logger");
        final String NO_LOGGING ="No Logging";
        final String ERROR ="Error";
        final String WARNING ="Warning";
        final String INFO ="Info";
        final String DEBUG ="Debug";

        loggerMenu.getPopupMenu().addPopupMenuListener(this);
        loggerMenu.setMnemonic(KeyEvent.VK_L);
        loggerMenu.setEnabled(false);
        AbstractAction loggerAction = new AbstractAction(){
            public void actionPerformed(ActionEvent e) {
                DriverProxy controller = Mediator.getDriverProxy();
                if (e.getActionCommand().equals(NO_LOGGING))
                    controller.setLogLevel(0);
                else if (e.getActionCommand().equals(ERROR))
                    controller.setLogLevel(1);
                else if (e.getActionCommand().equals(WARNING))
                    controller.setLogLevel(2);
                else if (e.getActionCommand().equals(INFO))
                    controller.setLogLevel(3);
                else if (e.getActionCommand().equals(DEBUG))
                    controller.setLogLevel(4);
            }
        };
       
        ButtonGroup group = new ButtonGroup();
        
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(NO_LOGGING);
        rbMenuItem.setSelected(true);
        rbMenuItem.setMnemonic(KeyEvent.VK_N);
        rbMenuItem.setActionCommand(NO_LOGGING);
        rbMenuItem.addActionListener(loggerAction);
        group.add(rbMenuItem);
        loggerMenu.add(rbMenuItem);
        loggerMenu.addSeparator();
        
        rbMenuItem = new JRadioButtonMenuItem(ERROR);
        rbMenuItem.setMnemonic(KeyEvent.VK_E);
        rbMenuItem.setActionCommand(ERROR);
        rbMenuItem.addActionListener(loggerAction);
        group.add(rbMenuItem);
        loggerMenu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(WARNING);
        rbMenuItem.setMnemonic(KeyEvent.VK_W);
        rbMenuItem.setActionCommand(WARNING);
        rbMenuItem.addActionListener(loggerAction);
        group.add(rbMenuItem);
        loggerMenu.add(rbMenuItem);
        
        rbMenuItem = new JRadioButtonMenuItem(INFO);
        rbMenuItem.setMnemonic(KeyEvent.VK_I);
        rbMenuItem.setActionCommand(INFO);
        rbMenuItem.addActionListener(loggerAction);
        group.add(rbMenuItem);
        loggerMenu.add(rbMenuItem);
        
        rbMenuItem = new JRadioButtonMenuItem(DEBUG);
        rbMenuItem.setMnemonic(KeyEvent.VK_D);
        rbMenuItem.setActionCommand(DEBUG);
        rbMenuItem.addActionListener(loggerAction);
        group.add(rbMenuItem);
        loggerMenu.add(rbMenuItem);

        final String ON ="On";
        final String OFF ="Off";
        cyberMenu = new JMenu("Cyber Debugger");
        cyberMenu.getPopupMenu().addPopupMenuListener(this);
        cyberMenu.setMnemonic(KeyEvent.VK_C);
        cyberMenu.setEnabled(false);
        AbstractAction cyberAction = new AbstractAction(){
            public void actionPerformed(ActionEvent e) {
                DriverProxy controller = Mediator.getDriverProxy();
                if (e.getActionCommand().equals(ON))
                    controller.setCyberDebug(true);
                else if (e.getActionCommand().equals(OFF))
                    controller.setCyberDebug(false);
            }
        };

       
        ButtonGroup cyberGroup = new ButtonGroup();
        rbMenuItem = new JRadioButtonMenuItem(ON);
        rbMenuItem.setSelected(true);
        rbMenuItem.setMnemonic(KeyEvent.VK_O);
        rbMenuItem.setActionCommand(ON);
        rbMenuItem.addActionListener(cyberAction);
        cyberGroup.add(rbMenuItem);
        cyberMenu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(OFF);
        rbMenuItem.setMnemonic(KeyEvent.VK_F);
        rbMenuItem.setActionCommand(OFF);
        rbMenuItem.addActionListener(cyberAction);
        cyberGroup.add(rbMenuItem);
        cyberMenu.add(rbMenuItem);
        
        /*
        JMenuItem clearSubscriptionItem = new JMenuItem("Clear Subscriptions");
        clearSubscriptionItem.setMnemonic(KeyEvent.VK_S);
        clearSubscriptionItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
// to do
                    }
            });
        */
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        Activator.context.getBundle().stop();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            });
        

        file_menu.add(searchMenu);
        file_menu.addSeparator();
        file_menu.add(loggerMenu);
        file_menu.add(cyberMenu);
        file_menu.addSeparator();
        file_menu.add(checkIncompleteItem);
        file_menu.add(checkErrataItem);
        //file_menu.addSeparator();
        //file_menu.add(clearSubscriptionItem);
        file_menu.addSeparator();
        file_menu.add(exitItem);

        menuBar.add(file_menu);                   
        frame.setJMenuBar(menuBar);

    }

 
    public void enableMenus(boolean driverAvailable,int logLevel,boolean cyberDebug) {
        searchMenu.setEnabled(driverAvailable);
        Component[] items = searchMenu.getPopupMenu().getComponents();
        for (int i=0;i < items.length;i++)
            items[i].setEnabled(driverAvailable);

        loggerMenu.setEnabled(driverAvailable);
        items = loggerMenu.getPopupMenu().getComponents();
        for (int i=0;i < items.length;i++)
            items[i].setEnabled(driverAvailable);
        if (driverAvailable){
                ((JRadioButtonMenuItem)items[logLevel>0?logLevel+1:0]).setSelected(true);
        }
        cyberMenu.setEnabled(driverAvailable);           
        items = cyberMenu.getPopupMenu().getComponents();
        for (int i=0;i < items.length;i++)
            items[i].setEnabled(driverAvailable);
        if (driverAvailable){
            if (cyberDebug)
                ((JRadioButtonMenuItem)items[0]).setSelected(true);
            else
                ((JRadioButtonMenuItem)items[1]).setSelected(true);
        }
   }

    public void popupMenuCanceled(PopupMenuEvent e) { }
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JPopupMenu loggerPopup = loggerMenu.getPopupMenu();
        JPopupMenu cyberPopup = cyberMenu.getPopupMenu();
        if (e.getSource()==loggerPopup){
            int logLevel = Mediator.getDriverProxy().getLogLevel();
            Component[] items = loggerPopup.getComponents();
            ((JRadioButtonMenuItem)items[logLevel>0?logLevel+1:0]).setSelected(true);          
        }
        else if (e.getSource()==cyberPopup){
            boolean cyberDebug = Mediator.getDriverProxy().getCyberDebug();
            Component[] items = cyberPopup.getComponents();
            if (cyberDebug)
                ((JRadioButtonMenuItem)items[0]).setSelected(true);
            else
                ((JRadioButtonMenuItem)items[1]).setSelected(true);            
        }
       
        
    }



	
}
