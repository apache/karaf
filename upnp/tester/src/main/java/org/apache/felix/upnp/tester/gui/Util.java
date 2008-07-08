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

package org.apache.felix.upnp.tester.gui;

import java.awt.GridBagConstraints;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class Util {
	final static GridBagConstraints constrains = new GridBagConstraints();

	public  static GridBagConstraints setConstrains(int x,int y,int w,int h,int wx, int wy)
    {
        constrains.insets.left=5;
        constrains.insets.right=5;
        constrains.insets.bottom=3;
        constrains.fill=GridBagConstraints.BOTH;
        constrains.anchor=GridBagConstraints.WEST;
        constrains.gridx=x;
        constrains.gridy=y;
        constrains.gridwidth=w;
        constrains.gridheight=h;
        constrains.weightx=wx;
        constrains.weighty=wy;
        return constrains;
    }
    
    public static String justString(Object obj){
        if (obj == null) return "";
        else if (obj instanceof String[]){
            String[] items = (String[])obj;
            String tmp = "";
            for (int i = 0; i < items.length; i++) {
                tmp+=items[i]+"; ";
            }
            return tmp;
        }
        return obj.toString();
    }

    public static void openUrl(String url) {
        try {
            if (url == null) return;
            String os=System.getProperty("os.name","").toLowerCase();
            Process p = null;
            if(os.indexOf("windows")!=-1){
                String cmd = null;
                cmd = "cmd.exe /C start "+url;
                LogPanel.log("[Executing cmd] " +cmd);
                p = Runtime.getRuntime().exec(cmd);
                
            }else if(os.indexOf("linux")!=-1){
            	String[] cmd = new String[]{
                		"/bin/sh",
                		"-c",
                		"( $BROWSER " + url + " || mozilla-firefox '" + url + "' || firefox '" + url 
                		+ "' || mozilla '" + url + "' || konqueror '" + url + "' || opera '" + url + "' )"
                };
            	StringBuffer sb = new StringBuffer();
            	for (int i = 0; i < cmd.length; i++) {
					sb.append(" ").append(cmd[i]);
					
				}
                LogPanel.log("[Executing cmd] " +sb.toString());
                p = Runtime.getRuntime().exec(cmd);
            }            
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));            
            while (true) {
            	while(err.ready()) System.err.println(err.readLine());
            	while(out.ready()) System.out.println(out.readLine());            	
            	try{
            		p.exitValue();
            		break;
            	}catch (IllegalThreadStateException e) {
				}
			}
        } catch (Exception ex){
            System.out.println(ex);
        }
    }

}
