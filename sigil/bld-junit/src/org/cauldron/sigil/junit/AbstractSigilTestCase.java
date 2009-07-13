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

package org.cauldron.sigil.junit;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import junit.framework.TestCase;

public abstract class AbstractSigilTestCase extends TestCase {

	private final static List<ServiceTracker> trackers = new LinkedList<ServiceTracker>();
	
	private BundleContext ctx;
	
	public void setBundleContext(BundleContext ctx) {
		this.ctx = ctx;
	}
	
	protected BundleContext getBundleContext() {
		return ctx;
	}
	
	@Override
	protected void setUp() {
		for ( Class<?> c : getReferences() ) {
			ServiceTracker t = createBindTracker(c);
			t.open();
			trackers.add( t );
		}
	}

	@Override
	protected void tearDown() {
		for ( ServiceTracker t : trackers ) {
			t.close();
		}
		trackers.clear();
	}


	private ServiceTracker createBindTracker(final Class<?> c) {
		return new ServiceTracker(ctx, c.getName(), new ServiceTrackerCustomizer() {
			public Object addingService(ServiceReference reference) {
				Object o = ctx.getService(reference);
				Method m = getBindMethod(c);
				if ( m != null ) invoke( m, o );
				return o;
			}

			public void modifiedService(ServiceReference reference,
					Object service) {
			}

			public void removedService(ServiceReference reference,
					Object service) {
				Method m = getUnbindMethod(c);
				if ( m != null ) invoke( m, service );
				ctx.ungetService(reference);
			}
		});
	}
	
	private void invoke(Method m, Object o) {
		try {
			m.invoke( this,  new Object[] { o } );
		} catch (Exception e) {
			throw new IllegalStateException( "Failed to invoke binding method " + m, e);
		}
	}

	protected abstract Class<?>[] getReferences();
	
	protected abstract Method getBindMethod(Class<?> clazz);
	
	protected abstract  Method getUnbindMethod(Class<?> clazz);
}
