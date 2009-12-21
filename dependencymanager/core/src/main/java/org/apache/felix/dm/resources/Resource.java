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
package org.apache.felix.dm.resources;

import java.io.IOException;
import java.io.InputStream;

/** 
 * Interface that defines a resource. Each resource has a unique ID, which should be
 * used for equality tests. Resources also have a repository name, path and name, which
 * can all be used to filter on. To access a resource, use the <code>openStream()</code>
 * method.
 */
public interface Resource {
    public static final String ID = "id";
    public static final String FILTER = "filter";
	public static final String PATH = "path";
	public static final String NAME = "name";
	public static final String REPOSITORY = "repository";
	
	/** Returns the unique identification of this resource. */
	public String getID();
	/** Returns the name of this resource. */
	public String getName();
	/** Returns the path of this resource. */
	public String getPath();
	/** Returns the repository of this resource. */
	public String getRepository();
	/**	Returns an input stream containing all data behind this resource. */
	public InputStream openStream() throws IOException;
}
