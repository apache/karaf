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
package org.apache.felix.scrplugin;

import java.util.*;

/**
 * Utility class for handling errors and warnings
 */
public class IssueLog {

    private final List<String> errors;

    private final List<String> warnings;

    public IssueLog(final boolean strictMode) {
        this.errors = new ArrayList<String>();
        if ( strictMode ) {
            this.warnings = this.errors;
        } else {
            this.warnings = new ArrayList<String>();
        }
    }

    public void addError(final String e) {
        errors.add(e);
    }

    public void addWarning(final String e) {
        warnings.add(e);
    }

    public int getNumberOfErrors() {
        return this.errors.size();
    }

    public List<String> getErrors() {
        return this.errors;
    }

    @SuppressWarnings("unchecked")
    public List<String> getWarnings() {
        if ( this.errors == this.warnings ) {
            return Collections.EMPTY_LIST;
        }
        return this.warnings;
    }
}
