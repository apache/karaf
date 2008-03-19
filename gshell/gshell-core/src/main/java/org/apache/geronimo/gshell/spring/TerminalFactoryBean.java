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
package org.apache.geronimo.gshell.spring;

import jline.Terminal;
import jline.UnixTerminal;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring factory bean for JLine terminal.
 * The main purpose of this factory is to destroy the terminal when
 * the Spring application is terminated, so that the terminal is
 * restored to its normal state.
 */
public class TerminalFactoryBean implements FactoryBean, DisposableBean {

    private Terminal terminal;

    public synchronized Object getObject() throws Exception {
        if (terminal == null) {
            terminal = Terminal.getTerminal();
        }
        return terminal;
    }

    public Class getObjectType() {
        return Terminal.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public synchronized void destroy() throws Exception {
        if (terminal != null) {
            if (terminal instanceof UnixTerminal) {
                ((UnixTerminal) terminal).restoreTerminal();
            }
            terminal = null;
        }
    }
}
