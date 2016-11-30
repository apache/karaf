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
package org.apache.karaf.shell.support.table;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShellTable {

    private static final char SEP_HORIZONTAL = '─';
    private static final char SEP_VERTICAL = '│';
    private static final char SEP_CROSS = '┼';

    private static final char SEP_HORIZONTAL_ASCII = '-';
    private static final char SEP_VERTICAL_ASCII = '|';
    private static final char SEP_CROSS_ASCII = '+';

    private static final String DEFAULT_SEPARATOR = " " + SEP_VERTICAL + " ";
    private static final String DEFAULT_SEPARATOR_ASCII = " " + SEP_VERTICAL_ASCII + " ";
    private static final String DEFAULT_SEPARATOR_NO_FORMAT = "\t";

    private List<Col> cols = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private boolean showHeaders = true;
    private String separator = DEFAULT_SEPARATOR;
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
     * @param text the text to display when the table is empty.
     * @return the shell table.
     */
    public ShellTable emptyTableText(String text) {
        this.emptyTableText = text;
        return this;
    }

    public void print(PrintStream out) {
        print(out, true);
    }

    public void print(PrintStream out, boolean format)  {
        boolean supported = false;
        String encoding = getEncoding(out);
        if (encoding != null) {
            CharsetEncoder encoder = Charset.forName(encoding).newEncoder();
            supported = encoder.canEncode(separator)
                    && encoder.canEncode(SEP_HORIZONTAL)
                    && encoder.canEncode(SEP_CROSS);
        }
        String separator = supported ? this.separator : DEFAULT_SEPARATOR_ASCII;

        // "normal" table rendering, with borders
        Row headerRow = new Row(cols);
        headerRow.formatContent(cols);
        for (Row row : rows) {
            row.formatContent(cols);
        }

        if (size > 0) {
            adjustSize();
        }

        if (format && showHeaders) {
            String headerLine = headerRow.getContent(cols, separator);
            out.println(headerLine);
            int iCol = 0;
            for (Col col : cols) {
                if (iCol++ == 0) {
                    out.print(underline(col.getSize(), false, supported));
                } else {
                    out.print(underline(col.getSize() + 3, true, supported));
                }
                iCol++;
            }
            out.println();
        }

        for (Row row : rows) {
            if (!format) {
                if (separator == null || separator.equals(DEFAULT_SEPARATOR))
                    out.println(row.getContent(cols, DEFAULT_SEPARATOR_NO_FORMAT));
                else out.println(row.getContent(cols, separator));
            } else {
                out.println(row.getContent(cols, separator));
            }
        }

        if (format && rows.size() == 0 && emptyTableText != null) {
            out.println(emptyTableText);
        }
    }

    private String getEncoding(PrintStream ps) {
        if (ps.getClass().getName().equals("org.apache.felix.gogo.runtime.threadio.ThreadPrintStream")) {
            try {
                ps = (PrintStream) ps.getClass().getMethod("getCurrent").invoke(ps);
            } catch (Throwable t) {
                // ignore
            }
        }
        try {
            Field f = ps.getClass().getDeclaredField("charOut");
            f.setAccessible(true);
            OutputStreamWriter osw = (OutputStreamWriter) f.get(ps);
            return osw.getEncoding();
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private void adjustSize() {
        int currentSize = 0;
        for (Col col : cols) {
            currentSize += col.size + separator.length();
        }
        currentSize -= separator.length();
        int sizeToGrow = size - currentSize;

        for (int i = cols.size() - 1; i >= 0; i--) {
            Col col = cols.get(i);
            if (col.maxSize == -1) {
                col.size = Math.max(0, col.size + sizeToGrow);
                return;
            }
        }

    }

    private String underline(int length, boolean crossAtBeg, boolean supported) {
        char[] exmarks = new char[length];
        Arrays.fill(exmarks,  supported ? SEP_HORIZONTAL : SEP_HORIZONTAL_ASCII);
        if (crossAtBeg) {
            exmarks[1] = supported ? SEP_CROSS : SEP_CROSS_ASCII;
        }
        return new String(exmarks);
    }

}
