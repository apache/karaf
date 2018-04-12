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

import org.jline.utils.AttributedStringBuilder;

/**
 * Colored support for column.
 */
public class AnsiColumn extends Col {

    private int color;
    private boolean bold;

    public AnsiColumn(String header, int color, boolean bold) {
        super(header);
        this.color = color;
        this.bold = bold;
    }

    @Override
    public String getContent(String content) {
        String in = super.getContent(content);

        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(sb.style().foreground(color));

        if (bold)
            sb.style(sb.style().bold());

        sb.append(in);

        if (bold)
            sb.style(sb.style().boldOff());

        sb.style(sb.style().foregroundOff());

        return sb.toAnsi();
    }

}
