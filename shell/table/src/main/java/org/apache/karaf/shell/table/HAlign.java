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

import static org.apache.karaf.shell.table.StringUtil.*;

/**
 * Enumeration type which contains all possible horizontal alignments.
 */
public enum HAlign {

    /**
     * Center align.
     */
    center {
        @Override
        public String position(String text, int colWidth) {
            int width = colWidth - length(text);
            text = repeat(" ", width / 2) + text + repeat(" ", width / 2);
            if (length(text) < colWidth) {
                // if colWidth is odd we add space at the end.
                text += " ";
            }
            return text;
        }
    },

    /**
     * Left align.
     */
    left {
        @Override
        public String position(String text, int colWidth) {
            return text + repeat(" ", colWidth - length(text));
        }
    },

    /**
     * Right align.
     */
    right {
        @Override
        public String position(String text, int colWidth) {
            return repeat(" ", colWidth - length(text)) + text;
        }
    };

    /**
     * Calculate text position.
     * 
     * @param text The text to align.
     * @param colWidth The width of the column.
     * @return The aligned string.
     */
    public abstract String position(String text, int colWidth);

}
