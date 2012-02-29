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
package org.apache.karaf.scr.command.completer;

import java.util.List;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

public abstract class ScrCompleterSupport implements Completer {

    private ScrService scrService;

    /**
     * Overrides the super method noted below. See super documentation for
     * details.
     *
     * @see org.apache.karaf.shell.console.Completer#complete(java.lang.String,
     *      int, java.util.List)
     */
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (Component component : scrService.getComponents()) {
                if (availableComponent(component)) {
                    delegate.getStrings().add(component.getName());
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public abstract boolean availableComponent(Component component)
            throws Exception;

    /**
     * Get the scrService Object associated with this instance of
     * ScrCompleterSupport.
     *
     * @return the scrService
     */
    public ScrService getScrService() {
        return scrService;
    }

    /**
     * Sets the scrService Object for this ScrCompleterSupport instance.
     *
     * @param scrService the scrService to set
     */
    public void setScrService(ScrService scrService) {
        this.scrService = scrService;
    }

}
