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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.support.ansi.SimpleAnsi;

/**
 * Column definition.
 */
public class Col {
	// This is kept here only for backwards compatibility
	// and is used in cyan(boolean) method
	private static final Function<String, String> COLOR_CYAN =
			(cellContent) -> SimpleAnsi.COLOR_CYAN;

	Function<String, String> colorProvider;

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

    boolean wrap;
    boolean bold;
    boolean cyan;


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

    public Col wrap() {
        return wrap(true);
    }

    public Col wrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    public Col bold() {
        return bold(true);
    }

    public Col bold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public Col cyan() {
        return cyan(true);
    }

	public Col cyan(boolean cyan) {
		if(cyan)
			colorProvider(COLOR_CYAN);
		
		// Only remove colorProvider if argument is false and 
		// member equals COLOR_CYAN
		else if(this.colorProvider == COLOR_CYAN)
			colorProvider(null);
		
		return this;
	}

    public int getSize() {
        return size;
    }
	
	public Col colorProvider(Function<String, String> colorProvider) {
		this.colorProvider = colorProvider;
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
        if (cellData == null) {
            cellData = "";
        }
        String fullContent = String.format("%s", cellData);
        if (fullContent.length() == 0) {
            return "";
        }
        String finalContent = cut(fullContent, getClippedSize(fullContent.length()));
        updateSize(finalContent.length());
        return finalContent;
    }

    String getHeader() {
        return header;
    }

    String getContent(String content) {
        List<String> lines = new ArrayList<>(Arrays.asList(content.split("\n")));
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) {
                wrapped.addAll(wrap(line));
            }
            lines = wrapped;
        }

        String color = null;
        if(colorProvider != null) {
        	color = colorProvider.apply(content);
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            line = this.align.position(cut(line, size), this.size);
            if (bold) {
                line = SimpleAnsi.INTENSITY_BOLD + line + SimpleAnsi.INTENSITY_NORMAL;
            }

            if(color != null)
            	sb.append(color);
            
            sb.append(line);
            
            if(color != null)
            	sb.append(SimpleAnsi.COLOR_DEFAULT);
        }
        return sb.toString();
    }

    protected String cut(String content, int size) {
        if (content.length() <= size) {
            return content;
        } else {
            return content.substring(0, Math.max(0, size - 1));
        }
    }

    protected List<String> wrap(String str) {
        List<String> result = new ArrayList<>();
        Pattern wrap = Pattern.compile("(\\S\\S{" + size + ",}|.{1," + size + "})(\\s+|$)");
        int cur = 0;
        while (cur >= 0) {
            int lst = str.indexOf('\n', cur);
            String s = (lst >= 0) ? str.substring(cur, lst) : str.substring(cur);
            if (s.length() == 0) {
                result.add(s);
            } else {
                Matcher m = wrap.matcher(s);
                while (m.find()) {
                    result.add(m.group());
                }
            }
            if (lst >= 0) {
                cur = lst + 1;
            } else {
                break;
            }
        }
        return result;
    }


}
