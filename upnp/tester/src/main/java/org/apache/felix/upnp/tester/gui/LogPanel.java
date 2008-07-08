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

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class LogPanel extends JPanel {
    private static JTextArea logArea = new JTextArea(4,50);
    private static JPanel statusBar = new JPanel(new BorderLayout());
    private static JLabel statusText = new JLabel();
    
    private static JScrollPane scroll = new JScrollPane(logArea);
    
    public LogPanel() {
        super(new BorderLayout());
        logArea.setText("");
        //add(new JScrollPane(new JTextArea(4,80)));
        add(scroll);
        statusBar.add(statusText,BorderLayout.EAST);
        add(statusBar,BorderLayout.SOUTH);
        }

    /* public static void log(String msg) {
        logArea.append(msg + "\n\r");
		JScrollBar scrBar = scroll.getVerticalScrollBar();
		int maxPos = scrBar.getMaximum();
		scrBar.setValue(maxPos);
   }*/
    
    public static void log(final String msg){ 
        synchronized (logArea) {
            logArea.append(msg );
            logArea.append("\n\r");
        }
        JScrollBar scrBar = scroll.getVerticalScrollBar();
        int maxPos = scrBar.getMaximum();
        scrBar.setValue(maxPos);
    }
    
    public static void status(final String msg){ 
        statusText.setText(msg);
     }

   public static void clear(String msg) {
        logArea.setText("");
   }

}
