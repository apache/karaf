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
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPException;
import org.osgi.service.upnp.UPnPStateVariable;

import org.apache.felix.upnp.basedriver.util.Converter;
import org.apache.felix.upnp.tester.Mediator;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ActionPanel extends JPanel {
	
	UPnPAction action;
	ArgumentsModel argsModel;
	MyTable table; 
	JPanel buttonPanel;
	
	OutputArgumentsModel outArgsModel; 
	JTable outTable; 
	
	/**
	 * 
	 */
	public ActionPanel() {
		super(new GridBagLayout());
		buildButtonPanel();
		buildTable();
		add(new JScrollPane(table),Util.setConstrains(0,0,1,1,100,100));
		add(new JScrollPane(outTable),Util.setConstrains(0,1,1,1,100,100)); 
		add(buttonPanel,Util.setConstrains(1,0,1,1,1,1));
	}

	private void buildButtonPanel(){
		buttonPanel = new JPanel();
		JButton doAction = new JButton("Do Action");
		doAction.addActionListener(new AbstractAction(){
			public void actionPerformed(ActionEvent e) {
				outArgsModel.clearData(); 

				Dictionary params = null;
				Dictionary result = null;
				if (argsModel.getRowCount()!= 0){
					if (table.isEditing()) table.getCellEditor().stopCellEditing();
					params = new Hashtable();
					for (int i =0;i<argsModel.getRowCount();i++){
						String name = (String) argsModel.getValueAt(i,0);
						String value = (String)argsModel.getValueAt(i,3);
						try {
                            params.put(
                            	name,
                                Converter.parseString(
                                    value,
                                    action.getStateVariable(name).getUPnPDataType()));
                        } catch (Exception ex) {
                        	LogPanel.log("Error invoking action (bad parameter)");
                         	return ;
                        }
					}
				}
				try {result = action.invoke(params);}
                catch (UPnPException ex){
                    String error =  
                        "===== Action Failed =====" +
                        "\nUPnP Error Code::\n    " +ex.getUPnPError_Code() +
                        "\nUPnP Error Description::\n    " +ex.getMessage();
                    printReport(params,error);
                    JOptionPane.showMessageDialog(
                        Mediator.getPropertiesViewer(),error);                   
                }
                catch (Exception ex){
                    printReport(params,ex.getMessage());
					JOptionPane.showMessageDialog(
						Mediator.getPropertiesViewer(),
						ex.getMessage()
					);
				}
				if (result != null) {
                    printReport(params,result);
					outArgsModel.setData(action,result);
					JOptionPane.showMessageDialog(
						Mediator.getPropertiesViewer(),
						"Action invoked!"
					);
				
				}
			}
            private void printReport(Dictionary params,Object result) {
                String input = "";
                String output = "";
                if (params != null) input = params.toString();
                if (output != null) output = result.toString();
                String method = action.getName();
                String report = "\n==== Action:: " + method + " ====\n"
                                + input 
                                + "\n----------- result ----------\n"
                                + output
                                + "\n-----------------------------";
               LogPanel.log(report);
            }
		});		
	    buttonPanel.add(doAction);
	}
	
	private void buildTable(){
		argsModel = new ArgumentsModel();
	    table = new MyTable(argsModel); 
		argsModel.setTable(table); 		

		outArgsModel = new OutputArgumentsModel();
	    outTable = new JTable(outArgsModel); 
	}

	public void setArguments(UPnPAction action){
		this.action = action;
		argsModel.setData(action);		
		outArgsModel.clearData(); 
	}
	
}

//thanks to Thomas Wagner 18/10/2005
class OutputArgumentsModel extends ArgumentsModel { 

	public OutputArgumentsModel() {
		super();
        header = new String[]{"output arg name","related Var","Java \\ UpnP type","value"};
	}

	public void clearData() {
		names = new String[]{""};
        related = new String[]{""};
    	types = new String[]{""};
    	values = new String[]{""};
		size=0;
		this.fireTableChanged(new TableModelEvent(this));
	}

    public void setData(UPnPAction  action, Object result){

		Hashtable res = (Hashtable) result;

        String[] names = action.getOutputArgumentNames();
   	    size = 0;
    	this.names = names;
    	if (names!=null){
	    	values = new String[names.length];
            types = new String[names.length];
            related = new String[names.length];
	    	for (int i=0;i<names.length;i++) {
                UPnPStateVariable currentStateVar = action.getStateVariable(names[i]);

				Object value = res.get(names[i]);
                values[i]=value.toString();
                related[i] = currentStateVar.getName();
                
                String javaType =currentStateVar.getJavaDataType().toString();
                javaType = javaType.substring(javaType.lastIndexOf('.')+1);
                String upnpType = currentStateVar.getUPnPDataType();
                types[i] = javaType + " \\ " + upnpType;
            }   	
	    	size = names.length;
    	}
		this.fireTableChanged(new TableModelEvent(this));
    }
}

//thanks to Thomas Wagner 12/10/2005
class ArgumentsModel extends  AbstractTableModel {
	int size = 0;
	private MyTable table;	
	String[] names = new String[]{""};
    String[] related = new String[]{""};
    String[] types = new String[]{""};
    String[] values = new String[]{""};
	String[] header = new String[]{"arg name","related Var","Java \\ UpnP type","value"};
   
	public void setTable(MyTable table) { 
		this.table = table;
	}
 
	public int getColumnCount() { return 4; }
    public int getRowCount() { return size;}
    public String getColumnName(int col) { return header[col]; }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
    	return (columnIndex ==3);
    }
    public Object getValueAt(int row, int col) { 
    	if (col == 0) return names[row];
        else if (col == 1) return related[row];
        else if (col == 2) return types[row];
        else if (col == 3) return values[row];
    	return null;
	}   
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    	values[rowIndex]= (String) aValue; 
    }

    public void setData(UPnPAction  action){

		table.deleteMyCellEditors();
        

        String[] names = action.getInputArgumentNames();
   	    size = 0;
    	this.names = names;
    	if (names!=null){
	    	values = new String[names.length];
            related = new String[names.length];
            types = new String[names.length];
	    	for (int i=0;i<names.length;i++) {
                values[i]="";
                UPnPStateVariable currentStateVar = action.getStateVariable(names[i]);
                related[i] = currentStateVar.getName();
                String javaType = currentStateVar.getJavaDataType().toString();
                javaType = javaType.substring(javaType.lastIndexOf('.')+1);
                String upnpType = currentStateVar.getUPnPDataType();
                types[i] = javaType + " \\ " + upnpType;

				//handle allowed value list
				if ( currentStateVar.getAllowedValues() != null) {
					String av[] = currentStateVar.getAllowedValues();
					JComboBox comboBox = new JComboBox();
					for (int j = 0; j < av.length; j++) {
						comboBox.addItem(av[j]);
					}
					values[i] = av[0]; //preset the first value from list
					table.setMyCellEditor(new DefaultCellEditor(comboBox),i);
				}

				//handle default value
				if (currentStateVar.getDefaultValue() != null) {
					String val = currentStateVar.getDefaultValue().toString();
					if(val.length() > 0)
						values[i] = val;
				}
				
               //handle range values
               if ((currentStateVar.getMaximum()!= null)
                   &&(currentStateVar.getMinimum()!= null)){
                   int max = currentStateVar.getMaximum().intValue();
                   int min = currentStateVar.getMinimum().intValue();
                   int value = min;
                   try { value = Integer.parseInt(values[i]);}
                   catch (NumberFormatException ignored){}
                   table.setMyCellEditor(new SliderEditor(min,max,value),i);
               }

            }   	
	    	size = names.length;
    	}
		this.fireTableChanged(new TableModelEvent(this));
        this.fireTableStructureChanged();
    }
}

// thanks to Thomas Wagner 12/10/2005
class MyTable extends JTable { 

	private Hashtable cellEditors;
	
	public MyTable(TableModel dm) {
		super(dm);
		cellEditors = new Hashtable();
	}

	public void setMyCellEditor(TableCellEditor editor,int col){
		cellEditors.put(new Integer(col),editor);
	}

	public void deleteMyCellEditors(){
		cellEditors.clear();
	}

	//overwritten JTable method	
	public TableCellEditor getCellEditor(int row,int col) {
		TableCellEditor tce = (TableCellEditor) cellEditors.get(new Integer(row));
		if( tce != null) {
			return tce;
		} else {
			return super.getCellEditor(row,col);
		}
	}
}

class SliderEditor extends AbstractCellEditor implements TableCellEditor
{
    JPanel editor;
    JTextField text;
    JSlider slider;
    public SliderEditor(int min, int max, int value){
         editor = new JPanel(new BorderLayout());
         //editor.setBorder(new EmptyBorder(1,0,1,0));
         text = new JTextField(Integer.toString(max).length());
         slider = new JSlider(SwingConstants.HORIZONTAL,min,max,value);
         editor.add(text,BorderLayout.WEST);
         editor.add(slider);
         slider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                //if (! replSlider.getValueIsAdjusting()){
                text.setText(new Integer(slider.getValue()).toString());
                //}
            }
         });

    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        text.setText((String) value);
        try {
            slider.setValue(Integer.parseInt((String)value));
        } catch (NumberFormatException ignored) {}
        return editor;
    }

    public Object getCellEditorValue() {
        return text.getText();
    }
}

