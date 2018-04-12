/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.table;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated use {@link org.apache.karaf.shell.support.table.ShellTable} instead
 */
@Deprecated
public class ShellTable {

    private List<Col> cols = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    boolean showHeaders = true;
    private String separator = " | ";
    private int size;
    private String emptyTableText;
    
    public ShellTable() {
        
    }
    
    public ShellTable noHeaders() {
        this.showHeaders = false;
        return this;
    }
    
    public ShellTable separator(String separator) {
        this.separator = separator;
        return this;
    }
    
    public ShellTable size(int size) {
        this.size = size;
        return this;
    }
    
    public ShellTable column(Col colunmn) {
        cols.add(colunmn);
        return this;
    }
    
    public Col column(String header) {
        Col col = new Col(header);
        cols.add(col);
        return col;
    }
    
    public Row addRow() {
        Row row = new Row();
        rows.add(row);
        return row;
    }
    
    /**
     * Set text to display if there are no rows in the table.
     *
     * @param text The text to display when the table is empty.
     * @return The shell table.
     */
    public ShellTable emptyTableText(String text) {
        this.emptyTableText = text;
        return this;
    }

    public void print(PrintStream out) {
        print(out, true);
    }

    public void print(PrintStream out, boolean format)  {

        // "normal" table rendering, with borders
        Row headerRow = new Row(cols);
        headerRow.formatContent(cols);
        for (Row row : rows) {
            row.formatContent(cols);
        }
        
        if (size > 0) {
            tryGrowToMaxSize();
        }

        if (format && showHeaders) {
            String headerLine = headerRow.getContent(cols, separator);
            out.println(headerLine);
            for (Col col : cols) {
                out.print(underline(col.getSize()));
            }
            out.println(underline((cols.size() - 1) * 3));
        }

        for (Row row : rows) {
            if (!format) {
                if (separator == null || separator.equals(" | "))
                    out.println(row.getContent(cols, "\t"));
                else out.println(row.getContent(cols, separator));
            } else {
                out.println(row.getContent(cols, separator));
            }
        }

        if (format && rows.size() == 0 && emptyTableText != null) {
            out.println(emptyTableText);
        }
    }

    private void tryGrowToMaxSize() {
        int currentSize = 0;
        for (Col col : cols) {
            currentSize += col.size + separator.length();
        }
        currentSize -= separator.length();
        int sizeToGrow = size - currentSize;

        for (Col col : cols) {
            if (col.maxSize == -1) {
                col.size = Math.max(0, col.size + sizeToGrow);
                return;
            }
        }

    }

    private String underline(int length) {
        char[] exmarks = new char[length];
        Arrays.fill(exmarks, '-');
        return new String(exmarks);
    }

}
