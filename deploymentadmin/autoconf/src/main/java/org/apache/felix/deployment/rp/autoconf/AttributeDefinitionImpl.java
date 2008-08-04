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
package org.apache.felix.deployment.rp.autoconf;

import org.apache.felix.metatype.AD;
import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl implements AttributeDefinition {

	private final AD m_ad;

	public AttributeDefinitionImpl(AD ad) {
		m_ad = ad;
	}
	
	public int getCardinality() {
		return m_ad.getCardinality();
	}

	public String[] getDefaultValue() {
		return m_ad.getDefaultValue();
	}

	public String getDescription() {
		return m_ad.getDescription();
	}

	public String getID() {
		return m_ad.getID();
	}

	public String getName() {
		return m_ad.getName();
	}

	public String[] getOptionLabels() {
		return m_ad.getOptionLabels();
	}

	public String[] getOptionValues() {
		return m_ad.getOptionValues();
	}

	public int getType() {
		return m_ad.getType();
	}

	public String validate(String value) {
        return m_ad.validate(value);
	}

}
