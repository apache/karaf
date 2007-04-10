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
package org.apache.felix.ipojo.composite.service.importer;

import java.util.List;

import org.apache.felix.ipojo.architecture.HandlerDescription;

/**
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ImportExportDescription extends HandlerDescription {
	
	private List m_imports;
	private List m_exports;

	/**
	 * Constructor.
	 * @param name
	 * @param isValid
	 * @param importers
	 * @param exporters
	 */
	public ImportExportDescription(String name, boolean isValid, List importers, List exporters) {
		super(name, isValid);
		m_imports = importers;
		m_exports = exporters;
	}
	
	/**
	 * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
	 */
	public String getHandlerInfo() {
		String s = "";
		for (int i = 0; i < m_imports.size(); i++) {
			ServiceImporter imp = (ServiceImporter) m_imports.get(i);
			if (imp.isSatisfied()) {
				s += "\t Specification " + imp.getSpecification() + " provided by \n \t";
				for (int j = 0; j < imp.getProviders().size(); j++) {
					String prov = (String) imp.getProviders().get(j);
					s += prov + " ";
				}	
			} else {
				s += "\t Specification " + imp.getSpecification() + " is not statisfied \n";
			}
		}
		for (int i = 0; i < m_exports.size(); i++) {
			ServiceExporter exp = (ServiceExporter) m_exports.get(i);
			if (exp.isSatisfied()) {
				s += "\t Specification " + exp.getSpecification() + " is exported or optional";
			} else {
				s += "\t Specification " + exp.getSpecification() + " is not exported";
			}
		}
		return s;
		
	}
	
	

}
