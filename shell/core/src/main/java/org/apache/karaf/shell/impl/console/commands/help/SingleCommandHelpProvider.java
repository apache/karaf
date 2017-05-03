/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.impl.console.commands.help;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.karaf.shell.api.console.Session;

public class SingleCommandHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("command|")) {
                path = path.substring("command|".length());
            } else {
                return null;
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true);
        Session s = session.getFactory().create(bais, ps, ps, session);
        s.put(Session.SCOPE, session.get(Session.SCOPE));
        s.put(Session.SUBSHELL, session.get(Session.SUBSHELL));
        try {
            s.execute(path + " --help");
        } catch (Throwable t) {
            return null;
        } finally {
            s.close();
        }
        return baos.toString();
    }

}
