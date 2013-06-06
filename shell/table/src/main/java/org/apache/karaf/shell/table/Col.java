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
     * may grow indefinitely
     */
    int maxSize = -1;
    
    int size = 0;
    
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

    public int getSize() {
        return size;
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
        if (cellData == null) {
            cellData = "";
        }
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

    String getContent(String content) {
        return this.align.position(cut(content, this.size), this.size);
    }

    private String cut(String content, int size) {
        if (content.length() <= size) {
            return content;
        } else {
            return content.substring(0, Math.max(0, size - 1));
        }
    }

}
