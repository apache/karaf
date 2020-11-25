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
package org.apache.karaf.bundle.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Dictionary;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenConfigService {

	private final static Logger logger = LoggerFactory.getLogger(MavenConfigService.class);
	private final ConfigurationAdmin configurationAdmin;

	public MavenConfigService(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

    File getLocalRepository() {
        String path = null;
        Dictionary<String, Object> config = getConfiguration();
        if (config != null) {
        	path = getLocalRepoFromConfig(config);
        }
        if (path == null) {
            path = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        int index = path.indexOf('@');
        if (index > 0) {
            return new File(path.substring(index)).getAbsoluteFile();
        } else {
            return new File(path).getAbsoluteFile();
        }
    }

	private Dictionary<String, Object> getConfiguration() {
		try {
            Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.url.mvn", null);
            if (configuration != null) {
                return configuration.getProcessedProperties(null);
            }
        } catch (Exception e) {
            logger.error("Error retrieving maven configuration", e);
        }
		return null;
	}

    static String getLocalRepoFromConfig(Dictionary<String, Object> dict) {
        String path = null;
        if (dict != null) {
            path = (String) dict.get("org.ops4j.pax.url.mvn.localRepository");
            if (path == null) {
                String settings = (String) dict.get("org.ops4j.pax.url.mvn.settings");
                if (settings != null) {
                    path = getLocalRepositoryFromSettings(new File(settings));
                }
            }
        }
        return path;
    }

	private static String getLocalRepositoryFromSettings(File file) {
		XMLStreamReader reader = null;
		try (InputStream fin = new FileInputStream(file)) {
			XMLInputFactory factory = XMLInputFactory.newFactory();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			reader = factory.createXMLStreamReader(fin);
		    int event;
		    String elementName = null;
		    while ((event = reader.next()) != XMLStreamConstants.END_DOCUMENT) {
		        if (event == XMLStreamConstants.START_ELEMENT) {
		            elementName = reader.getLocalName();
		        } else if (event == XMLStreamConstants.END_ELEMENT) {
		            elementName = null;
		        } else if (event == XMLStreamConstants.CHARACTERS && "localRepository".equals(elementName))  {
		            return reader.getText().trim();
		        }
		    }
		} catch (Exception e) {
			logger.error("Error retrieving maven configuration", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					// Ignore
				}
			}
		}
		return null;
	}

}
