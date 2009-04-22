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
package org.apache.felix.ipojo.junit4osgi.command;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.felix.ipojo.junit4osgi.OSGiJunitRunner;
import org.apache.felix.shell.Command;

/**
 * Felix shell command. Allow to run tests.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JunitCommand implements Command {

    /**
     * OSGi Junit Runner service.
     */
    private OSGiJunitRunner m_runner;

    /**
     * Gets the Test names.
     * @param list the list of test
     * @return the list of test names.
     */
    private List getNamesFromTests(List list) {
        List names = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof TestCase) {
                names.add(((TestCase) list.get(i)).getName());
            }
            if (list.get(i) instanceof TestSuite) {
                String name = ((TestSuite) list.get(i)).getName();
                if (name == null) {
                    name = ((TestSuite) list.get(i)).toString();
                }
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Executes the command.
     * @param line the command line
     * @param out the output stream
     * @param err the error stream
     * @see org.apache.felix.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void execute(String line, PrintStream out, PrintStream err) {
        line = line.substring(getName().length()).trim();
        List tr = null;
        if (line.equals("all")) {
            if (m_runner.getTests() == null) {
                err.println("No tests to execute");
                return;
            } else {
                out.println("Executing " + getNamesFromTests(m_runner.getTests()));
                tr = m_runner.run();
            }
        } else {
            try {
                Long bundleId = new Long(line);
                if (m_runner.getTests(bundleId.longValue()) == null) {
                    err.println("No tests to execute");
                    return;
                } else {
                    out.println("Executing " + getNamesFromTests(m_runner.getTests(bundleId.longValue())));
                    tr = m_runner.run(bundleId.longValue());
                }
            } catch (NumberFormatException e) {
                err.println("Unable to parse id " + line);
                return;
            }
        }

        ListIterator it = tr.listIterator();
        while (it.hasNext()) {
            TestResult result = (TestResult) it.next();
            if (result.failureCount() != 0) {
                TestFailure fail = (TestFailure) result.failures().nextElement();
                out.println(fail.trace());
                return;
            }
        }

    }

    /**
     * Gets the command name.
     * @return "junit"
     * @see org.apache.felix.shell.Command#getName()
     */
    public String getName() {
        return "junit";
    }

    /**
     * Gets a small description of the command.
     * @return "launch junit tests"
     * @see org.apache.felix.shell.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "launch junit tests";
    }

    /**
     * Gets command usage.
     * @return the command usage.
     * @see org.apache.felix.shell.Command#getUsage()
     */
    public String getUsage() {
        return "junit <bundleid> | junit all";
    }

}
