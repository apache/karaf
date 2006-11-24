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
package org.apache.felix.mosgi.console.component;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.TreePath;

public class MyTree extends JTree{

  public MyTree(DefaultTreeModel dtm){
    super(dtm);
    //setRowHeight(0); // so DefaultTreeModel.getPreferedSize() is always call
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setInitialDelay(700);
    ToolTipManager.sharedInstance().setDismissDelay(7000);
    //ToolTipManager.setReshownDelay();
    //ToolTipManager.setEnabled();
  }

  public String convertValueToText(Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus){
    DefaultMutableTreeNode dmtn=(DefaultMutableTreeNode) value;
      if (dmtn.getLevel()==3) {
        int childNumber=dmtn.getChildCount();
        //String lastMsg=new String((childNumber==0)?"":dmtn.getFirstChild().toString());
        return value.toString()+" ("+dmtn.getChildCount()+" events)";//+lastMsg;
      }else{
        return value.toString();
      }
  }

}
