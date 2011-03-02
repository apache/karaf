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
 * Column definition.
 * 
 * @author ldywicki
 */
public class Column extends TableElement {

    /**
     * Column header.
     */
    private String header;

    /**
     * Preferred size.
     */
    private int size;

    /**
     * This flag allows to grow column when value in row is longer than initial column size. After growing 'size' will
     * be increased.
     */
    private boolean mayGrow;

    /**
     * Default align.
     */
    private HAlign align = HAlign.left;

    /**
     * Optional element which allows to set style dynamically.
     */
    private StyleCalculator styleCalculator;

    public Column(HAlign align) {
        this.align = align;
    }

    public Column() {
    }

    public Column(int size, boolean mayGrow, HAlign align) {
        this.size = size;
        this.mayGrow = mayGrow;
        this.align = align;
    }

    public Column(int size, boolean mayGrow) {
        this.size = size;
        this.mayGrow = mayGrow;
    }

    public Column(int size, HAlign align) {
        this(size, false, align);
    }

    public Column(boolean mayGrow) {
        this(0, true);
    }

    public Column(int size) {
        this(size, false);
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isMayGrow() {
        return mayGrow;
    }

    public void setMayGrow(boolean mayGrow) {
        this.mayGrow = mayGrow;
    }

    public HAlign getAlign() {
        return align;
    }

    public void setAlign(HAlign align) {
        this.align = align;
    }

    public StyleCalculator getStyleCalculator() {
        return styleCalculator;
    }

    public void setStyleCalculator(StyleCalculator styleCalculator) {
        this.styleCalculator = styleCalculator;
    }

}
