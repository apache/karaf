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

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class test extends JFrame {

   Image image;

   public test(String filename) {
     super(filename);
     setDefaultCloseOperation(EXIT_ON_CLOSE);
     //image = getToolkit().getImage(filename);
     ImageIcon image =loadIcon("EventedStateVariable");
     //image.setImageObserver(this);
     JPanel panel = new JPanel();
     
     panel.add(new JLabel(image));
     JLabel lab = new JLabel();
     lab.setIcon(image);
     panel.add(lab);
     this.getContentPane().add(panel);
       }

   /*public void paint(Graphics g) {
     super.paint(g);
     g.drawImage(image, 25, 25, this);
   }*/

   public static void main(String args[]) {
     if (args.length > 0) {
       JFrame f = new test(args[0]);
       f.setSize(300, 300);
       f.show();
     } else {
       System.err.println(
        "You must specify an image filename to display");
     }
   }
   
   
   public  static ImageIcon loadIcon(String name)
   {
       try {
           //System.out.println("loading image ..."+path);
           URL eventIconUrl = test.class.getResource("images/" + name + ".gif");           
           return new ImageIcon(eventIconUrl,name);
       }
       catch (Exception ex){
		        System.out.println("Resource:" + name + " not found : " + ex.toString());
           return null;
       }
   }

}
