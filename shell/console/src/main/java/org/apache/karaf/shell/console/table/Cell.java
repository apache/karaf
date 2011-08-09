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

/**
 * Cell information.
 */
public class Cell extends TableElement {

    private String value;
    private int colSpan;
    private HAlign align;

    public Cell(Object value) {
        this(value, 0);
    }

    public Cell(Object value, HAlign align) {
        this(value, align, 0);
    }

    public Cell(Object value, int colSpan) {
        this(value, null, colSpan);
    }

    public Cell(Object value, HAlign align, int colSpan) {
        this.value = value.toString();
        this.colSpan = colSpan;
        this.align = align;
    }

    public String getValue() {
        return value;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = colSpan;
    }

    public int getColSpan() {
        return colSpan;
    }

    public HAlign getAlign() {
        return align;
    }

    public void setAlign(HAlign align) {
        this.align = align;
    }

    @Override
    public String toString() {
        return "[Cell: " + value +"]";
    }
}
