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
package org.apache.karaf.instance.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.easymock.EasyMock;

public class CreateCommandTest extends TestCase {

    public void testCreateCommandExecute() throws Exception {
        InstanceService instanceService = EasyMock.createMock(InstanceService.class);
        EasyMock.replay(instanceService);
        
        CreateCommand cc = new CreateCommand();
        cc.setInstanceService(instanceService);
        cc.sshPort = 9941;
        cc.rmiRegistryPort = 1122;
        cc.rmiServerPort = 44444;
        cc.location = "top";
        cc.javaOpts = "foo";
        cc.features = new ArrayList<>(Arrays.asList("abc", "def"));
        cc.featureURLs = new ArrayList<>(Collections.singletonList("http://something"));
        cc.instance = "myInstance";
        cc.verbose = true;

        // Create the features config file used
        System.setProperty("karaf.etc", "target/etc");
        File cfgFile = new File("target/etc/" + CreateCommand.FEATURES_SERVICE_CONFIG_FILE);
        cfgFile.getParentFile().mkdirs();
        cfgFile.createNewFile();

        
        EasyMock.verify(instanceService); // check precondition
        EasyMock.reset(instanceService);
        InstanceSettings expectedIS =
            new InstanceSettings(9941, 1122, 44444, "top", "foo", Collections.singletonList("http://something"), Arrays.asList("abc", "def"));
        EasyMock.expect(instanceService.createInstance("myInstance", expectedIS, true)).andReturn(null);
        EasyMock.replay(instanceService);
        
        cc.doExecute();
        EasyMock.verify(instanceService);
    }

}
