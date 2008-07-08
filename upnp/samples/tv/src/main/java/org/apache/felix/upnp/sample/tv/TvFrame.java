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

package org.apache.felix.upnp.sample.tv;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.osgi.framework.BundleException;

public class TvFrame extends JFrame implements Runnable 
{
	private final static String TITLE = "Felix UpnP TV";
	
	private TvDevice tvDev;
	private TvPane tvPane;
	
	public TvFrame()
	{
		super(TITLE);

		tvDev = new TvDevice();

		getContentPane().setLayout(new BorderLayout());

		tvPane = new TvPane();
		tvDev.setComponent(tvPane);
		tvPane.setDevice(tvDev);
		getContentPane().add(tvPane, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) 
			{
				try {
					Activator.context.getBundle().stop();
				} catch (BundleException ex) {
					ex.printStackTrace();
				}
			}
		});			
		
	       try {
	            URL eventIconUrl = TvFrame.class.getResource("images/logo.gif");           
	            ImageIcon icon=  new ImageIcon(eventIconUrl,"logo");
	            setIconImage(icon.getImage());
	       }
	        catch (Exception ex){
	                System.out.println("Resource: IMAGES/logo.gif not found : " + ex.toString());
	        }
	        
		pack();
		setVisible(true);
	}

	public TvPane getTvPanel()
	{
		return tvPane;
	}

	public TvDevice getTvDevice()
	{
		return tvDev;
	}
		
	////////////////////////////////////////////////
	//	run	
	////////////////////////////////////////////////

	private Thread timerThread = null;
		
	public void run()
	{
		Thread thisThread = Thread.currentThread();

		while (timerThread == thisThread) {
			tvDev.setMessage("");
			tvPane.repaint();
			try {
				Thread.sleep(1000*5);
			}
			catch(InterruptedException e) {}
		}
	}
	
	public void start()
	{
		tvDev.start();
		
		timerThread = new Thread(this,"upnp.sample.tv.TVFrame");
		timerThread.start();
	}
	
	public void stop()
	{
		tvDev.stop();
		timerThread = null;
		dispose();
	}


}

