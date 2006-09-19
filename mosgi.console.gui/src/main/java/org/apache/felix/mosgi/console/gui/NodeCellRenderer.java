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
package org.apache.felix.mosgi.console.gui;

import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.osgi.framework.BundleContext;

public class NodeCellRenderer extends DefaultTreeCellRenderer {
  private ImageIcon iconConnected;
  private ImageIcon iconStopped;
  private NodesTree nodesTree;

  public NodeCellRenderer(BundleContext m_context, NodesTree nt){
    this.iconConnected=new ImageIcon(Toolkit.getDefaultToolkit().getImage(m_context.getBundle().getResource("images/ServerRunning.gif")));
    this.iconStopped=new ImageIcon(Toolkit.getDefaultToolkit().getImage(m_context.getBundle().getResource("images/ServerBlocked.gif")));
    this.nodesTree=nt;
  }

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);
    String val=value.toString();
    if (!val.equals(NodesTree.TOP_NAME)){ 
      if (nodesTree.isNodeConnected(val)){
        setIcon(iconConnected);
      }else{
        setIcon(iconStopped);
      }
    }
    return this;
  }
}

