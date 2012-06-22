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
package org.apache.karaf.kar.command.completers;

import org.apache.karaf.kar.KarService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;

/**
 * Completer on all installed KAR files.
 */
public class KarCompleter implements Completer {
    
    private KarService karService;
    
    public int complete(String buffer, int cursor, @SuppressWarnings("rawtypes") List candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (String karName : karService.list()) {
                delegate.getStrings().add(karName);
            }
        } catch (Exception e) {
            // ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }
    
    public void setKarService(KarService karService) {
        this.karService = karService;
    }
    
    public KarService getKarService() {
        return this.karService;
    }
    
}
