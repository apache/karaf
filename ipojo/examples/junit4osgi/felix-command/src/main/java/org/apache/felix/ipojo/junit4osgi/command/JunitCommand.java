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

import junit.framework.Test;
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

    private OSGiJunitRunner runner;

    private List<String> getNamesFromTests(List<Test> list) {
        List<String> names = new ArrayList<String>(list.size());
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

    public void execute(String line, PrintStream out, PrintStream err) {
        line = line.substring(getName().length()).trim();
        List<TestResult> tr = null;
        if (line.equals("all")) {
            if (runner.getTests() == null) {
                err.println("No tests to execute");
                return;
            } else {
                out.println("Executing " + getNamesFromTests(runner.getTests()));
                tr = runner.run();
            }
        } else {
            try {
                Long bundleId = new Long(line);
                if (runner.getTests(bundleId) == null) {
                    err.println("No tests to execute");
                    return;
                } else {
                    out.println("Executing " + getNamesFromTests(runner.getTests(bundleId)));
                    tr = runner.run(bundleId);
                }
            } catch (NumberFormatException e) {
                err.println("Unable to parse id " + line);
                return;
            }
        }

        ListIterator<TestResult> it = tr.listIterator();
        while (it.hasNext()) {
            TestResult result = it.next();
            if (result.failureCount() != 0) {
                TestFailure fail = (TestFailure) result.failures().nextElement();
                out.println(fail.trace());
                return;
            }
        }

    }

    public String getName() {
        return "junit";
    }

    public String getShortDescription() {
        return "launch junit tests";
    }

    public String getUsage() {
        return "junit";
    }

}
