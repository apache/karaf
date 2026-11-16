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
import java.util.regex.Pattern;

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

    private static final String PAX_URL_PID = "org.ops4j.pax.url.mvn";
    private static final String KARAF_URL_PID = "org.apache.karaf.url.mvn";

    private static final Pattern OPTIONS = Pattern.compile(
            "@(?:(?:id|update|releasesUpdate|snapshotsUpdate|checksum|releasesChecksum|snapshotsChecksum)=[^@]*"
            + "|snapshots|noreleases|multi)(?=@|$)", Pattern.CASE_INSENSITIVE);

	private final ConfigurationAdmin configurationAdmin;

	public MavenConfigService(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

    File getLocalRepository() {
        String path = null;
        Dictionary<String, Object> config = getConfiguration(PAX_URL_PID);
        if (config != null) {
        	path = getLocalRepoFromConfig(config);
        } else {
            // without Pax URL, the Karaf URL service (karaf-light distribution)
            config = getConfiguration(KARAF_URL_PID);
            if (config != null) {
                path = getLocalRepoFromConfig(config, KARAF_URL_PID);
            }
        }
        if (path == null) {
            path = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        return new File(stripOptions(path)).getAbsoluteFile();
    }

    /**
     * As for the repositories, the options of the local repository (<code>@id=...</code>) are not part of its path,
     * which may contain <code>@</code> too (<code>/var/jenkins/workspace/job@2</code>).
     */
    static String stripOptions(String path) {
        return OPTIONS.matcher(path).replaceAll("");
    }

	private Dictionary<String, Object> getConfiguration(String pid) {
		try {
            Configuration configuration = configurationAdmin.getConfiguration(pid, null);
            if (configuration != null) {
                return configuration.getProcessedProperties(null);
            }
        } catch (Exception e) {
            logger.error("Error retrieving maven configuration", e);
        }
		return null;
	}

    static String getLocalRepoFromConfig(Dictionary<String, Object> dict) {
        return getLocalRepoFromConfig(dict, PAX_URL_PID);
    }

    static String getLocalRepoFromConfig(Dictionary<String, Object> dict, String pid) {
        String path = null;
        if (dict != null) {
            path = (String) dict.get(pid + ".localRepository");
            if (path == null) {
                String settings = (String) dict.get(pid + ".settings");
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
