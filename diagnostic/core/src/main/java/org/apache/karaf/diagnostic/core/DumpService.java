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
package org.apache.karaf.diagnostic.core;

import java.util.List;

/**
 * Dump service which allows to customize dump creation process.
 * 
 * @author ldywicki
 */
public interface DumpService {

	/**
	 * Return registered providers.
	 * 
	 * @return Providers registered in OSGi service registry.
	 */
	List<DumpProvider> listProviders();

	/**
	 * List destinations where dumps can be stored.
	 * 
	 * @return Destinations registered in OSGi service registry.
	 */
	List<DumpDestination> listDestinations();

	/**
	 * Make dump using given providers.
	 * 
	 * @param destination Store destination.
	 * @param providers Dump providers to use.
	 * @return True if dump was created.
	 */
	boolean dump(DumpDestination destination, DumpProvider ... providers);

	/**
	 * Creates data witch all dump providers.
	 * 
	 * @param destination Store destination.
	 * @return True if dump was created.
	 */
	boolean dumpAll(DumpDestination destination);
}
