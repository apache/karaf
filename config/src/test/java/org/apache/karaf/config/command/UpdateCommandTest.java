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
package org.apache.karaf.config.command;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.config.core.ConfigRepository;
import org.easymock.EasyMock;

import junit.framework.TestCase;

/**
 * Test cases for {@link EditCommand}
 */
public class UpdateCommandTest extends TestCase {

    private static final String FACTORY_PID = "myFactoryPid";
    private static final String PID = "myPid";

    public void testupdateRegularConfig() throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();

        UpdateCommand command = new UpdateCommand();
        ConfigRepository configRepo = EasyMock.createMock(ConfigRepository.class);
        configRepo.update(EasyMock.eq(PID), EasyMock.eq(props), EasyMock.eq(null));
        EasyMock.expectLastCall();
		command.setConfigRepository(configRepo);

        MockCommandSession session = createMockSessionForFactoryEdit(PID, false, props);
        command.setSession(session);
        replay(configRepo);

        command.execute();
        EasyMock.verify(configRepo);
    }

    public void testupdateOnNewFactoryPid() throws Exception {
		Hashtable<String, Object> props = new Hashtable<>();

        UpdateCommand command = new UpdateCommand();
        ConfigRepository configRepo = EasyMock.createMock(ConfigRepository.class);
        expect(configRepo.createFactoryConfiguration(EasyMock.eq(FACTORY_PID), EasyMock.eq(null), EasyMock.eq(props), EasyMock.eq(null)))
        	.andReturn(PID + ".35326647");
		command.setConfigRepository(configRepo);

        MockCommandSession session = createMockSessionForFactoryEdit(FACTORY_PID, true, props);
        command.setSession(session);
        replay(configRepo);

        command.execute();
        EasyMock.verify(configRepo);
    }

	private MockCommandSession createMockSessionForFactoryEdit(String pid, boolean isFactory,
			Dictionary<String, Object> props) {
		MockCommandSession session = new MockCommandSession();
        session.put(ConfigCommandSupport.PROPERTY_CONFIG_PID, pid);
        session.put(ConfigCommandSupport.PROPERTY_FACTORY, isFactory);
        TypedProperties tp = new TypedProperties();
        for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            tp.put(key, props.get(key));
        }
        session.put(ConfigCommandSupport.PROPERTY_CONFIG_PROPS, tp);
		return session;
	}

}
