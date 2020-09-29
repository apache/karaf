/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.itests.examples;

import org.apache.karaf.itests.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.service.cm.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Hashtable;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ConfigExampleTest extends BaseTest {

    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private PrintStream originStream;
    private PrintStream outStream = new PrintStream(byteArrayOutputStream);

    @Before
    public void setUp() throws Exception {
        originStream = System.out;
        System.setOut(outStream);
    }

    public void addFeaturesRepository() throws Exception {
        addFeaturesRepository("mvn:org.apache.karaf.examples/karaf-config-example-features/" + System.getProperty("karaf.version") + "/xml");
    }

    @Test
    public void testStatic() throws Exception {
        addFeaturesRepository();

        installAndAssertFeature("karaf-config-example-static");

        System.out.flush();
        System.setOut(originStream);

        String output = byteArrayOutputStream.toString();

        System.out.println(output);

        assertContains("foo = bar", output);
        assertContains("hello = world", output);
    }

    @Test
    public void testManaged() throws Exception {
        addFeaturesRepository();

        installAndAssertFeature("karaf-config-example-managed");

        System.out.flush();
        assertContains("Configuration changed", byteArrayOutputStream.toString());

        Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.example.config", null);
        Dictionary<String, Object> properties = configuration.getProcessedProperties(null);
        if (properties == null) {
            properties = new Hashtable<>();
        }
        properties.put("exam", "test");
        configuration.update(properties);

        Thread.sleep(500);

        assertContains("exam = test", byteArrayOutputStream.toString());
    }

    @Test
    public void testListener() throws Exception {
        addFeaturesRepository();

        installAndAssertFeature("karaf-config-example-listener");

        Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.example.config", null);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("exam", "test");
        configuration.update(properties);

        Thread.sleep(500);

        System.out.flush();

        assertContains("Configuration org.apache.karaf.example.config has been updated", byteArrayOutputStream.toString());
    }

    @Test
    public void testBlueprint() throws Exception {
        addFeaturesRepository();

        installAndAssertFeature("karaf-config-example-blueprint");

        System.out.flush();

        assertContains("hello = world", byteArrayOutputStream.toString());

        Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.example.config", null);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("hello", "exam");
        configuration.update(properties);

        Thread.sleep(500);

        System.out.flush();

        assertContains("hello = exam", byteArrayOutputStream.toString());
    }

    @Test
    public void testScr() throws Exception {
        addFeaturesRepository();

        installAndAssertFeature("karaf-config-example-scr");

        System.out.flush();

        assertContainsNot("hello = exam", byteArrayOutputStream.toString());

        Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.example.config", null);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("hello", "exam");
        configuration.update(properties);

        Thread.sleep(500);

        System.out.flush();

        assertContains("hello = exam", byteArrayOutputStream.toString());
    }

}
