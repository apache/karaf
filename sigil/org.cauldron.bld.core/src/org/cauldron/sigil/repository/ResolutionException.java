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

package org.cauldron.sigil.repository;

import org.cauldron.sigil.model.IModelElement;

public class ResolutionException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private IModelElement[] parsed;
	
	public ResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResolutionException(String message) {
		super(message);
	}

	public ResolutionException(Throwable cause) {
		super(cause);
	}

	public ResolutionException(IModelElement root, IModelElement[] parsed) {
		super(buildMessage(root, parsed));
		this.parsed = parsed;
	}
	
	private static String buildMessage(IModelElement root, IModelElement[] parsed) {
		StringBuilder b = new StringBuilder();
		b.append( "Failed to resolve " );
		b.append( root );
		
		if ( parsed.length > 0 ) {
			b.append( " due to missing provider for " );
			b.append( parsed[parsed.length - 1] );
		}
		
		return b.toString();
	}

	public IModelElement[] getParsed() {
		return parsed;
	}
}
