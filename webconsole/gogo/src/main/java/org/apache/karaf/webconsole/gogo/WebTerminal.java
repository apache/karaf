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
package org.apache.karaf.webconsole.gogo;

 import org.apache.karaf.shell.api.console.Terminal;
 import org.apache.karaf.shell.support.terminal.SignalSupport;

public class WebTerminal extends SignalSupport implements Terminal {

    private int width;
    private int height;
    private boolean echo = true;

    public WebTerminal(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String getType() {
        return "ansi";
    }

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    @Override
    public boolean isEchoEnabled() {
        return echo;
    }

    @Override
    public void setEchoEnabled(boolean enabled) {
        echo = enabled;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
