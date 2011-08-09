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

import static org.fusesource.jansi.Ansi.Attribute.*;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Class which covers {@link org.fusesource.jansi.Ansi} class with standard fluent-api.
 */
public class Style {

    /**
     * We register all styles here.
     */
    private Ansi style = new Ansi();
    private boolean clean = true;

    /**
     * Mark text as bold.
     * 
     * @return Style.
     */
    public Style bold() {
        unclean().a(INTENSITY_BOLD);
        return this;
    }

    /**
     * Mark text as italic.
     * 
     * @return Style.
     */
    public Style italic() {
        unclean().a(ITALIC);
        return this;
    }

    /**
     * Sets text color.
     * 
     * @param c Color.
     * @return Style.
     */
    public Style color(Color c) {
        unclean().fg(c);
        return this;
    }

    /**
     * Sets background color.
     * 
     * @param c Color.
     * @return Style.
     */
    public Style background(Color c) {
        unclean().bg(c);
        return this;
    }

    /**
     * Strike text.
     * 
     * @return Style.
     */
    public Style strike() {
        unclean().a(STRIKETHROUGH_ON);
        return this;
    }

    /**
     * Underline text.
     * 
     * @return Style.
     */
    public Style underline() {
        unclean().a(UNDERLINE);
        return this;
    }

    /**
     * Make text blink.
     * 
     * @return Style.
     */
    public Style blink() {
        unclean().a(BLINK_SLOW);
        return this;
    }

    /**
     * Change foreground with background.
     * 
     * @return Style.
     */
    public Style inverse() {
        unclean().a(NEGATIVE_ON);
        return this;
    }

    /**
     * Mark text as invisible;
     * 
     * @return Style.
     */
    public Style invisible() {
        unclean().a(NEGATIVE_ON);
        return this;
    }

    /**
     * Is any values was set?
     * 
     * @return Boolean.
     */
    public boolean isClean() {
        return clean;
    }

    /**
     * Apply style to given text.
     * 
     * @param text Text to stylish.
     * @return Styled text - with ansi espace codes.
     */
    public String apply(String text) {
//        return new Ansi(style).a(text).reset().toString();
		return "";
    }

    /**
     * Sets dirty flag and return ansi object.
     */
    private Ansi unclean() {
        clean = false;
        return style;
    }
    @Override
    public String toString() {
//        return isClean() ? "[no style]" : "[ansi: " + new Ansi(style).a("x").reset() + "]";
		return "";
    }
}
