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

import org.osgi.framework.Bundle;

import java.util.Hashtable;
import java.awt.Color;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class JtableCellRenderer extends JLabel implements TableCellRenderer {

  private Hashtable eventName=new Hashtable();

  public JtableCellRenderer() {
    super();
    eventName.put(new Integer( Bundle.UNINSTALLED ), Color.black  );
    eventName.put(new Integer( Bundle.INSTALLED   ), Color.red    );
    eventName.put(new Integer( Bundle.RESOLVED    ), Color.orange );
    eventName.put(new Integer( Bundle.STARTING    ), Color.gray   );
    eventName.put(new Integer( Bundle.STOPPING    ), Color.gray   );
    eventName.put(new Integer( Bundle.ACTIVE      ), Color.green  );
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    setOpaque(true);
    if (column==0){
      Integer state;
      try{
        state=new Integer(Integer.parseInt((String) table.getValueAt(row,5)));
      }catch (NumberFormatException nfe) {
        state=new Integer(-1);
      }
      if (value!=null) {
        if (((String) value).equals(JtreeCellRenderer.UNKNOWN_DATE)) {
	  setBackground(Color.white);
        } else{
	  setBackground((Color) eventName.get(state));
        }
      }
    }
    setText((String) value);

    return this;
  }	

}
