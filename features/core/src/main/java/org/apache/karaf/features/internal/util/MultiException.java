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
package org.apache.karaf.features.internal.util;

import java.util.List;

@SuppressWarnings("serial")
public class MultiException extends Exception {

    public MultiException(String message) {
        super(message);
    }

    public MultiException(String message, List<Exception> exceptions) {
        super(message);
        if (exceptions != null) {
            for (Exception exception : exceptions) {
                addSuppressed(exception);
            }
        }
    }

    @Deprecated
    public void addException(Exception e) {
        addSuppressed(e);
    }

    public void throwIfExceptions() throws MultiException {
        if (getSuppressed().length > 0) {
            throw this;
        }
    }

    @Deprecated
    public Throwable[] getCauses() {
        return getSuppressed();
    }

    public static void throwIf(String message, List<Exception> exceptions) throws MultiException {
        if (exceptions != null && !exceptions.isEmpty()) {
            StringBuilder sb = new StringBuilder(message);
            sb.append(":");
            for (Exception e : exceptions) {
                sb.append("\n\t");
                sb.append(e.getMessage());
            }
            throw new MultiException(sb.toString(), exceptions);
        }
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append(":");
        for (Throwable e : getSuppressed()) {
            sb.append("\n\t");
            sb.append(e.getMessage());
        }
        return sb.toString();
    }
}
