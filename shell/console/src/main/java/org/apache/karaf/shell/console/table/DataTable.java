/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.console.table;

import static org.apache.karaf.shell.console.table.StringUtil.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple data table.
 *
 * This class contains set of formatting code needed to print tabular data in console. There are two ways to construct
 * table.
 * 1) Provide output and number of columns
 * 2) Provide output and manually configure columns
 * First way is very fast and takes small amount of code, but you cannot configure column behavior. All columns will be
 * expandable and aligned to left side.
 * Second way takes more code but allows you to control column format.
 *
 * To avoid performance issues data table flushes output once per 10 rows. If you wish to change this - just set value
 * for flushAfter field.
 * Important notice - last column always don't have borders. It is designed to put mark last column as expandable and
 * put there longer values.
 *
 * Code samples:
 * <code>
 *      DataTable table = new DataTable(System.out);
 *
 *      table.addColumn(new Column(3, true));
 *      table.addColumn(new Column(5));
 *      table.addColumn(new Column(12));
 *      table.addColumn(new Column(12));
 *      table.addColumn(new Column(5));
 *      table.addColumn(new Column(true));
 *
 *      Row row = new Row();
 *      row.addCell(new Cell("OSGi", HAlign.center, 2));
 *      row.addCell(new Cell("Extender", HAlign.center, 2));
 *      row.addCell(new Cell("Misc", 2));
 *      table.addRow(row);
 *
 *      row = new Row();
 *      row.addCell("ID");
 *      row.addCell("State");
 *      row.addCell("Spring");
 *      row.addCell("Blueprint");
 *      row.addCell("Level");
 *      row.addCell("Name");
 *      table.addRow(row);
 *
 *      // load sample data
 *      for (int i = 0; i < 5; i++) {
 *          table.addRow(new Object[] {i, i, i, i, i, i});
 *      }
 *      table.flush();
 * </code>
 * And expected output:
 * <code>
 * [   OSGi   ][         Extender         ] Misc
 * [ID ][State][Spring      ][Blueprint   ][Level] Name
 * [0  ][0    ][0           ][0           ][0    ] 0
 * [1  ][1    ][1           ][1           ][1    ] 1
 * [2  ][2    ][2           ][2           ][2    ] 2
 * [3  ][3    ][3           ][3           ][3    ] 3
 * [4  ][4    ][4           ][4           ][4    ] 4
 * </code>
 * 
 * @author ldywicki
 */
public class DataTable extends TableElement {

    /**
     * Output destination.
     */
    private final Appendable target;

    /**
     * Table border style.
     */
    private Style borderStyle = new Style();

    private List<Column> columns = new ArrayList<Column>();
    private List<Row> rows = new ArrayList<Row>();

    /**
     * Number of rows to add before flushing rows to stream.
     */
    private int flushAfter = 10;

    public DataTable(Appendable target) {
        this.target = target;
    }

    public DataTable(PrintStream out, int colCount) {
        this(out);

        for (int i = 0; i < colCount; i++) {
            addColumn(new Column(true));
        }
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    private void printBorder(Row row, String border) {
        if (row.isBorders()) {
            append(borderStyle.apply(border));
        } else {
            append(repeat(" ", border.length()));
        }
    }

    public void flush() {
        printRows();
        rows.clear();
    }

    private void printRows() {
        for (Row row : rows) {
            List<Cell> cells = row.getCells();

            for (int i = 0, colIndex = 0, size = cells.size(); i < size; i++) {
                Cell cell = cells.get(i);
                int colSpan = cell.getColSpan();

                Column column = columns.get(colIndex);
                int colSize = 0;

                boolean first = i == 0;
                boolean last = i + 1 == size;

                if (colSpan > 1) {
                    for (int j = 0; j < colSpan; j++) {
                        colSize += columns.get(colIndex + j).getSize();
                    }
                    colSize += colSpan;
                    colIndex += colSpan;
                } else {
                    colSize = column.getSize();
                    colIndex++;
                }

                Style style = calculateStyle(column, row, cell);

                if (first) {
                    printBorder(row, "[");
                } else if (!last) {
                    printBorder(row, "][");
                } else {
                    printBorder(row, "] ");
                }

                String value = cell.getValue();

                if (value.length() > colSize) {
                    if (column.isMayGrow()) {
                        column.setSize(value.length());
                    } else {
                        value = value.substring(value.length() - 2) + "..";
                    }
                } 

                append(style.apply(calculateAlign(column, cell).position(value, colSize)));

            }
            append("\n");
        }
    }

    private HAlign calculateAlign(Column column, Cell cell) {
        if (cell.getAlign() != null) {
            return cell.getAlign();
        }
        return column.getAlign();
    }

    private Style calculateStyle(Column column, Row row, Cell cell) {
        StyleCalculator styleCalculator = column.getStyleCalculator();
        Style dynamic = null;

        if (styleCalculator != null) {
            dynamic = styleCalculator.calculate(cell.getValue());
        }

        if (!cell.getStyle().isClean()) {
            return cell.getStyle();
        } else if (dynamic != null) {
            return dynamic;
        } else if (!row.getStyle().isClean()) {
            return row.getStyle();
        } else if (!column.getStyle().isClean()) {
            return column.getStyle();
        }
        return new Style();
    }

    private void append(String string) {
        try {
            target.append(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRow(Object[] row) {
        addRow(new Row(row));
    }

    public void addRow(Row row) {
        rows.add(row);

        if ((rows.size() % flushAfter) == 0) {
            printRows();
            rows.clear();
        }
    }

    public void setBorderStyle(Style style) {
        this.borderStyle = style;
    }

    public void setFlushAfter(int flushAfter) {
        this.flushAfter = flushAfter;
    }
}
