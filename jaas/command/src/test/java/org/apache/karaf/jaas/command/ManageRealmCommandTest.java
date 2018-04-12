/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.command;

import java.util.Arrays;
import java.util.Properties;

import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ManageRealmCommandTest {

    @Test
    public void testSelectByIndex() throws Exception {
        ManageRealmCommand cmd = new ManageRealmCommand();

        // set up two realms, each containing 1 module

        Config realm1 = newConfigNamed("realm1");
        realm1.setModules(new Module[] { newModuleNamed("module1") });

        Config realm2 = newConfigNamed("realm2");
        realm2.setModules(new Module[] { newModuleNamed("module2") });

        Config[] realms = { realm1, realm2 };

        doVerifyIndex(cmd, 1, realms);
        doVerifyIndex(cmd, 2, realms);
    }

    /**
     * Verify that command selects the correct realm, given some index.
     *
     * @param cmd the command to use.
     * @param index the index to verify.
     * @param realms the array of realms.
     * @throws Exception in case of failure.
     */
    private void doVerifyIndex(ManageRealmCommand cmd, int index, Config[] realms) throws Exception {

        // prepare mocks
        Session session = createMock(Session.class);
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle bundle = createMock(Bundle.class);

        // prepare command
        cmd.index = index;
        cmd.setRealms(Arrays.asList(realms));
        cmd.setSession(session);

        for (Config realm : realms)
            realm.setBundleContext(bundleContext);

        Object[] mocks = { session, bundleContext, bundle };

        expect(session.get(ManageRealmCommand.JAAS_REALM)).andReturn(null).anyTimes();
        expect(session.get(ManageRealmCommand.JAAS_ENTRY)).andReturn(null).anyTimes();
        expect(session.get(ManageRealmCommand.JAAS_CMDS)).andReturn(null).anyTimes();
        expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
        expect(bundle.getBundleId()).andReturn(4711L).anyTimes();

        // verify that the correct realm is returned -- cmd.index is 1-based
        session.put(ManageRealmCommand.JAAS_REALM, realms[index - 1]);
        session.put(eq(ManageRealmCommand.JAAS_ENTRY), anyObject());
        session.put(eq(ManageRealmCommand.JAAS_CMDS), anyObject());

        // start the test
        replay(mocks);
        cmd.execute();
        verify(mocks);
    }

    private Config newConfigNamed(String name) {
        Config res = new Config();
        res.setName(name);
        return res;
    }

    private Module newModuleNamed(String name) {
        Module res = new Module();
        res.setName(name);
        res.setOptions(new Properties());
        res.setFlags("required");
        return res;
    }

}
