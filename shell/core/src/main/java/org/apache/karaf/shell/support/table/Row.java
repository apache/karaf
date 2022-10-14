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
package org.apache.karaf.shell.support.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Row {
    private List<Object> data;
    private List<String> content;
    
    Row() {
        data = new ArrayList<>();
        content = new ArrayList<>();
    }
    
    Row(List<Col> cols) {
        this();
        for (Col col : cols) {
            data.add(col.getHeader());
        }
    }

    public void addContent(List<Object> data) {
        this.data = data;
    }

    public void addContent(Object ... cellDataAr) {
        data.addAll(Arrays.asList(cellDataAr));
    }
    
    void formatContent(List<Col> cols) {
        content.clear();
        int c = 0;
        for (Col col : cols) {
            content.add(col.format(data.get(c)));
            c++;
        }
    }
    
    String getContent(List<Col> cols, String separator) {
        if (cols.size() != content.size()) {
            throw new RuntimeException("Number of columns and number of content elements do not match");
        }

        List<String[]> contents = new ArrayList<>();
        int lines = 0;
        for (int col = 0; col < cols.size(); col++) {
            String[] cnt = cols.get(col).getContent(content.get(col)).split("\n");
            lines = Math.max(lines, cnt.length);
            contents.add(cnt);
        }
        StringBuilder st = new StringBuilder();
        for (int line = 0; line < lines; line++) {
            if (line > 0) {
                st.append("\n");
            }
            StringBuilder st2 = new StringBuilder();
            for (int col = 0; col < cols.size(); col++) {
                String[] strings = contents.get(col);
                if (col > 0) {
                    st2.append(separator);
                }
                if (line < strings.length) {
                    st2.append(strings[line]);
                } else {
                    st2.append(StringUtil.repeat(" ", cols.get(col).getSize()));
                }
            }
            while (st2.length() > 0 && st2.charAt(st2.length() - 1) == ' ') {
                st2.setLength(st2.length() - 1);
            }
            st.append(st2);
        }
        return st.toString();
    }

}
