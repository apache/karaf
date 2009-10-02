/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for handling errors and warnings
 */
public class IssueLog {

    private final boolean strictMode;

    private final List<String> errors = new ArrayList<String>();

    private final List<String> warnings = new ArrayList<String>();

    public IssueLog(final boolean strictMode) {
        this.strictMode = strictMode;
    }

    public int getNumberOfErrors() {
        return this.errors.size();
    }

    public boolean hasErrors() {
        return errors.size() > 0 || (this.strictMode && warnings.size() > 0 );
    }

    public void addError(final String e) {
        errors.add(e);
    }

    public void addWarning(final String e) {
        warnings.add(e);
    }

    public Iterator<String> getWarnings() {
        return warnings.iterator();
    }

    public Iterator<String> getErrors() {
        return errors.iterator();
    }
}
