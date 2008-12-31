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
package org.apache.felix.ipojo.junit4osgi.command;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Test result renderer.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResultCellRenderer extends DefaultTableCellRenderer {

    /**
     * UUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Renderer method.
     * @param table the table
     * @param value the value
     * @param isSelected is the cell selected
     * @param hasFocus has the cell the focus
     * @param row the cell row
     * @param column the cell column
     * @return the resulting component
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        ResultTableModel results = (ResultTableModel) table.getModel();
        String status = (String) results.getValueAt(row, column);
        if (status.equals(ResultTableModel.SUCCESS)) {
            c.setForeground(Color.GREEN);
            setToolTipText(results.getMessage(row, column));
        }
        if (status.equals(ResultTableModel.FAILURE)) {
            c.setForeground(Color.ORANGE);
            setToolTipText(results.getMessage(row, column));
        }
        if (status.equals(ResultTableModel.ERROR)) {
            c.setForeground(Color.RED);
            setToolTipText(results.getMessage(row, column));
        }

        return c;
    }
}
