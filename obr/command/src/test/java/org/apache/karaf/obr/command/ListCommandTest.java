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
package org.apache.karaf.obr.command;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;

public class ListCommandTest {

	/**
	 * Show how the list of obr resources looks like 
	 * @throws Exception
	 */
	@Test
	public void testList() throws Exception {
		IMocksControl control = EasyMock.createControl();
		RepositoryAdmin repoAdmin = control.createMock(RepositoryAdmin.class);
		ListCommand command = new ListCommand();
		command.setRepoAdmin(repoAdmin);
		
		Resource[] resources = new Resource[] {
			createResource("My bundle", "my.bundle", "1.0.0"),
			createResource("My other Bundle", "org.apache.mybundle", "2.0.1")
		};
		EasyMock.expect(repoAdmin.discoverResources("(|(presentationname=*)(symbolicname=*))")).
			andReturn(resources);
		
		control.replay();
		command.execute(null);
		control.verify();
	}

	private Resource createResource(String presentationName, String symbolicName, String version) {
		ResourceImpl r1 = new ResourceImpl();
		r1.put(Resource.PRESENTATION_NAME, presentationName);
		r1.put(Resource.SYMBOLIC_NAME, symbolicName);
		r1.put(Resource.VERSION, version);
		return r1;
	}

}
