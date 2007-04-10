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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.CompositeManager;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * This handler manages the importation and the exportation of services from / to the parent context.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ImportExportHandler extends CompositeHandler {
	
	/**
	 * Composite Manager.
	 */
	private CompositeManager m_manager;
	
	/**
	 * Service Scope. 
	 */
	private ServiceContext m_scope;
	
	/**
	 * Parent context. 
	 */
	private BundleContext m_context;
	
	/**
	 * List of importers. 
	 */
	private List m_importers = new ArrayList();
	
	/**
	 * List of exporters.
	 */
	private List m_exporters = new ArrayList(); 
	
	/**
	 * Is the handler valid ?
	 */
	private boolean m_valid;

	/**
	 * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
	 */
	public void configure(CompositeManager im, Element metadata, Dictionary conf) {
		m_manager = im;
		m_context = im.getContext();
		m_scope = m_manager.getServiceContext();
		
		Element[] imp = metadata.getElements("import");
		Element[] exp = metadata.getElements("export");
		
		for (int i = 0; i < imp.length; i++) {
			boolean optional = false;
			boolean aggregate = false;
			String specification = null;
			
			if (!imp[i].containsAttribute("specification")) { // Malformed import
				im.getFactory().getLogger().log(Logger.ERROR, "Malformed import : the specification attribute is mandatory");
			} else {
				specification = imp[i].getAttribute("specification");
				String filter = "(&(objectClass=" + specification + ")(!(service.pid=" + m_manager.getInstanceName() + ")))"; // Cannot import yourself
				if (imp[i].containsAttribute("optional") && imp[i].getAttribute("optional").equalsIgnoreCase("true")) { optional = true; }
				if (imp[i].containsAttribute("aggregate") && imp[i].getAttribute("aggregate").equalsIgnoreCase("true")) { aggregate = true; }
				if (imp[i].containsAttribute("filter")) { 
					String classnamefilter = "(objectClass=" + specification + ")";
			        filter = "";
			        if (!imp[i].getAttribute("filter").equals("")) {
			            filter = "(&" + classnamefilter + imp[i].getAttribute("filter") + ")";
			        } else {
			            filter = classnamefilter;
			        }
				}
				ServiceImporter si = new ServiceImporter(specification, filter, aggregate, optional, this);
				m_importers.add(si);
			}
		}
		
		for (int i = 0; i < exp.length; i++) {
			boolean optional = false;
			boolean aggregate = false;
			String specification = null;
			
			if (!exp[i].containsAttribute("specification")) { // Malformed exports
				im.getFactory().getLogger().log(Logger.ERROR, "Malformed exports : the specification attribute is mandatory");
			} else {
				specification = exp[i].getAttribute("specification");
				String filter = "(objectClass=" + specification + ")";
				if (exp[i].containsAttribute("optional") && exp[i].getAttribute("optional").equalsIgnoreCase("true")) { optional = true; }
				if (exp[i].containsAttribute("aggregate") && exp[i].getAttribute("aggregate").equalsIgnoreCase("true")) { aggregate = true; }
				if (exp[i].containsAttribute("filter")) { 
					String classnamefilter = "(objectClass=" + specification + ")";
			        filter = "";
			        if (!imp[i].getAttribute("filter").equals("")) {
			            filter = "(&" + classnamefilter + exp[i].getAttribute("filter") + ")";
			        } else {
			            filter = classnamefilter;
			        }
			    }
				ServiceExporter si = new ServiceExporter(specification, filter, aggregate, optional, this);
				// Update the componenet type description
				
				m_manager.getComponentDescription().addProvidedServiceSpecification(specification);
				m_exporters.add(si);
			}
		}
		
		if (m_importers.size() > 0 || m_exporters.size() > 0) {
			im.register(this);
		}
	}

	/**
	 * @see org.apache.felix.ipojo.CompositeHandler#start()
	 */
	public void start() {
		for (int i = 0; i < m_importers.size(); i++) {
			ServiceImporter si = (ServiceImporter) m_importers.get(i);
			si.configure(m_context, m_scope);
			si.start();
		}
		
		for (int i = 0; i < m_exporters.size(); i++) {
			ServiceExporter se = (ServiceExporter) m_exporters.get(i);
			se.configure(m_scope, m_context);
			se.start();
		}
		
	}

	/**
	 * @see org.apache.felix.ipojo.CompositeHandler#stop()
	 */
	public void stop() {
		for (int i = 0; i < m_importers.size(); i++) {
			ServiceImporter si = (ServiceImporter) m_importers.get(i);
			si.stop();
		}
		
		for (int i = 0; i < m_exporters.size(); i++) {
			ServiceExporter se = (ServiceExporter) m_exporters.get(i);
			se.stop();
		}
	}
	
	/**
	 * @see org.apache.felix.ipojo.CompositeHandler#isValid()
	 */
	public boolean isValid() {
		for (int i = 0; i < m_importers.size(); i++) {
			ServiceImporter si = (ServiceImporter) m_importers.get(i);
			if (!si.isSatisfied()) { m_valid = false; return false; }
		}
		
		for (int i = 0; i < m_exporters.size(); i++) {
			ServiceExporter se = (ServiceExporter) m_exporters.get(i);
			if (!se.isSatisfied()) { m_valid = false; return false; }
		}
	
		m_valid = true;
		return true;
	}

	/**
	 * Notify the handler that an importer is no more valid.
	 * @param importer : the implicated importer.
	 */
	protected void invalidating(ServiceImporter importer) {
		//	An import is no more valid
		if (m_valid) { m_manager.checkInstanceState(); }
		
	}

	/**
	 * Notify the handler that an importer becomes valid.
	 * @param importer : the implicated importer.
	 */
	protected void validating(ServiceImporter importer) {
		// An import becomes valid
		if (!m_valid && isValid()) { m_manager.checkInstanceState(); }
		
	}

	/**
	 * Notify the handler that an exporter becomes invalid.
	 * @param exporter : the impcated exporter.
	 */
	protected void invalidating(ServiceExporter exporter) {
		// An import is no more valid
		if (m_valid) { m_manager.checkInstanceState(); }
		
	}

	/**
	 * Notify the handler that an exporter becomes valid.
	 * @param exporter : the impcated exporter.
	 */
	protected void validating(ServiceExporter exporter) {
		// An import becomes valid
		if (!m_valid && isValid()) { m_manager.checkInstanceState(); }
		
	}
	
	/**
	 * @return the attached composite manager.
	 */
	protected CompositeManager getManager() { return m_manager; }
	
	
	/**
	 * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
	 */
	public HandlerDescription getDescription() { return new ImportExportDescription(this.getClass().getName(), isValid(), m_importers, m_exporters); }
}
