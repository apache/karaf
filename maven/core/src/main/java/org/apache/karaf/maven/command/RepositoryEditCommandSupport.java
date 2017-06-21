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
package org.apache.karaf.maven.command;

import java.util.Dictionary;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Option;

public abstract class RepositoryEditCommandSupport extends MavenSecuritySupport {

    @Option(name = "-id", description = "Identifier of repository", required = true, multiValued = false)
    String id;

    @Option(name = "-d", aliases = { "--default" }, description = "Edit default repository instead of remote one", required = false, multiValued = false)
    boolean defaultRepository = false;

    boolean success = false;

    @Override
    public final void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        edit(prefix, config);

        if (success) {
            if (showPasswords) {
                session.execute("maven:repository-list -x");
            } else {
                session.execute("maven:repository-list");
            }
        }
    }

    protected abstract void edit(String prefix, Dictionary<String, Object> config) throws Exception;

}
