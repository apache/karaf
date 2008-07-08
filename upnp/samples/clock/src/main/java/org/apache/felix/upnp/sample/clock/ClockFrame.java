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

package org.apache.felix.upnp.sample.clock;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ClockFrame extends JFrame implements Runnable 
{
	private final static String TITLE = "Felix UPnP Clock";
	private ClockDevice clockDev;
	private ClockPane clockPane;
	
	public ClockFrame(final BundleContext context)
	{
		super(TITLE);
		try {
			clockDev = new ClockDevice( context);
		}
		catch (Exception e) {
			System.out.println(e);
		}
				
		getContentPane().setLayout(new BorderLayout());

		clockPane = new ClockPane();		
		getContentPane().add(clockPane, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) 
			{
				try {
					context.getBundle().stop();
				} catch (BundleException ex) {
					ex.printStackTrace();
				}
			}
		});			
	       try {
	            URL eventIconUrl = ClockFrame.class.getResource("images/logo.gif");           
	            ImageIcon icon=  new ImageIcon(eventIconUrl,"logo");
	            setIconImage(icon.getImage());
	       }
	        catch (Exception ex){
	                System.out.println("Resource: IMAGES/logo.gif not found : " + ex.toString());
	        }
		
		pack();
		setVisible(true);
	}

	public ClockPane getClockPanel()
	{
		return clockPane;
	}

	public ClockDevice getClockDevice()
	{
		return clockDev;
	}
		
	////////////////////////////////////////////////
	//	run	
	////////////////////////////////////////////////

	private Thread timerThread = null;
		
	public void run()
	{
		Thread thisThread = Thread.currentThread();

		while (timerThread == thisThread) {
			getClockDevice().update();
			getClockPanel().repaint();
			try {
				Thread.sleep(1000);
			}
			catch(InterruptedException e) {}
		}
	}
	
	public void start()
	{
		clockDev.start();
		
		timerThread = new Thread(this,"upnp.sample.clock.ClockFrame");
		timerThread.start();
	}
	
	public void stop()
	{
		clockDev.stop();
		timerThread = null;
		dispose();
	}

}

