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
package org.apache.felix.das;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;

/**
 * 
 * Some simple tests for the DriverAttributes class.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class DriverAttributesTest {

	
	private DriverAttributes m_attributes;
	
	@Mock
	private ServiceReference m_ref;
	
	@Mock
	private Driver m_driver;
	
	@Mock
	private Bundle m_bundle;
	
	@Before
	public void setUp() throws Exception {
		
		MockitoAnnotations.initMocks(this);
		
		Mockito.when(m_ref.getBundle()).thenReturn(m_bundle);
		
		Mockito.when(m_bundle.getLocation()).thenReturn("_DD_test-driverbundle");
		
		m_attributes = new DriverAttributes(m_ref, m_driver);
	}


	@Test
	public void VerifyDriverReferenceReturned() throws Exception {
		
		Assert.assertEquals(m_ref, m_attributes.getReference());
	}

	@Test
	public void VerifyDriverInUseByDevice() throws Exception {
		
		ServiceReference ref = Mockito.mock(ServiceReference.class);
		
		Mockito.when(ref.getProperty(Constants.OBJECTCLASS))
			.thenReturn(new String[]{Object.class.getName()});
		
		Mockito.when(ref.getProperty(
			org.osgi.service.device.Constants.DEVICE_CATEGORY))
			.thenReturn(new String[]{"dummy"});
		
		Mockito.when(m_bundle.getServicesInUse()).thenReturn(new ServiceReference[]{ref});
		
		m_attributes.tryUninstall();
		
		Mockito.verify(m_bundle).getLocation();
		Mockito.verify(m_bundle).getServicesInUse();
		Mockito.verifyNoMoreInteractions(m_bundle);

	}
	
	@Test
	public void VerifyDriverInUseByDeviceInstance() throws Exception {
		
		ServiceReference ref = Mockito.mock(ServiceReference.class);
		
		Mockito.when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[]{Device.class.getName()});
		Mockito.when(ref.getProperty(
				org.osgi.service.device.Constants.DEVICE_CATEGORY))
				.thenReturn(new String[]{"dummy"});
		
		Mockito.when(m_bundle.getServicesInUse()).thenReturn(new ServiceReference[]{ref});
		
		m_attributes.tryUninstall();
		
		Mockito.verify(m_bundle).getLocation();
		Mockito.verify(m_bundle).getServicesInUse();
		Mockito.verifyNoMoreInteractions(m_bundle);

	}
	
	
	@Test
	public void VerifyDriverInUseByNoDevice() throws Exception {
		
		ServiceReference ref = Mockito.mock(ServiceReference.class);
		
		Mockito.when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[]{Object.class.getName()});
		Mockito.when(m_bundle.getServicesInUse()).thenReturn(new ServiceReference[]{ref});
		
		m_attributes.tryUninstall();
		
		Mockito.verify(m_bundle).getLocation();
		Mockito.verify(m_bundle).getServicesInUse();
		Mockito.verify(m_bundle).uninstall();

	}
	@Test
	public void VerifyDriverNotInUseLeadsToUnInstall1() throws Exception {
		
		Mockito.when(m_bundle.getServicesInUse()).thenReturn(new ServiceReference[0]);
		
		m_attributes.tryUninstall();
		
		Mockito.verify(m_bundle).uninstall();
		

	}
	
	@Test
	public void VerifyDriverNotInUseLeadsToUnInstall2() throws Exception {
		
		m_attributes.tryUninstall();
		
		Mockito.verify(m_bundle).uninstall();
		
	}	
	
	@Test
	public void VerifyAttachCalledOnDriver() throws Exception {
		
		
		ServiceReference ref = Mockito.mock(ServiceReference.class);
		m_attributes.attach(ref);
		
		Mockito.verify(m_driver).attach(Mockito.eq(ref));
		
	}

	@Test
	public void VerifyMatchCalledOnDriver() throws Exception {
		
		
		ServiceReference ref = Mockito.mock(ServiceReference.class);
		m_attributes.match(ref);
		
		Mockito.verify(m_driver).match(Mockito.eq(ref));
		
	}
}
