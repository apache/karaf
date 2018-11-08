/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console.commands;

import org.apache.karaf.shell.console.SubShell;

@Deprecated
public class AnnotatedSubShell implements SubShell {

    public String getName() {
        return getAnnotation().name();
    }

    public String getDescription() {
        return getAnnotation().description();
    }

    public String getDetailedDescription() {
        return getAnnotation().detailedDescription();
    }

    org.apache.felix.gogo.commands.SubShell getAnnotation() {
        org.apache.felix.gogo.commands.SubShell ann =
                getClass().getAnnotation(org.apache.felix.gogo.commands.SubShell.class);
        if (ann == null) {
            throw new IllegalStateException(
                    "The class should be annotated with the org.apache.felix.gogo.commands.SubShell annotation");
        }
        return ann;
    }
}
