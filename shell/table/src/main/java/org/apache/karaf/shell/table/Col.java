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
package org.apache.karaf.shell.table;

import java.text.Format;


/**
 * Column definition.
 */
public class Col {

    /**
     * Column header.
     */
    private String header;

    /**
     * Maximum size of this column. The default -1 means the column
     * may grow indefinately
     */
    private int maxSize = -1;
    
    private int size = 0;

    
    /**
     * Alignment
     */
    private HAlign align = HAlign.left;
    
    public Col(String header) {
        this.header = header;
    }

    public Col align(HAlign align) {
        this.align = align;
        return this;
    }
    
    public Col alignLeft() {
        this.align = HAlign.left;
        return this;
    }
    
    public Col alignRight() {
        this.align = HAlign.right;
        return this;
    }
    
    public Col alignCenter() {
        this.align = HAlign.center;
        return this;
    }
    
    public Col maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }
    
    protected void updateSize(int cellSize) {
        if (this.size <= cellSize) {
            this.size = getClippedSize(cellSize);
        }
    }
    
    private int getClippedSize(int cellSize) {
        return this.maxSize == -1 ? cellSize : Math.min(cellSize, this.maxSize);
    }

    String format(Object cellData) {
        String fullContent = String.format("%s", cellData);
        if (fullContent.length() == 0) {
            return "";
        }
        String finalContent = fullContent.substring(0, getClippedSize(fullContent.length()));
        updateSize(finalContent.length());
        return finalContent;
    }

    String getHeader() {
        return header;
    }

    int getMaxSize() {
        return maxSize;
    }

    Format getFormat() {
        return null;
    }

    int getSize() {
        return this.size;
    }

    String getContent(String content) {
        return this.align.position(content, this.size);
    }

}
