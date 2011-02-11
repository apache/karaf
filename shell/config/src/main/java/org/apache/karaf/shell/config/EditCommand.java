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
package org.apache.karaf.shell.config;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

@Command(scope = "config", name = "edit", description = "Creates or edits a configuration.")
public class EditCommand extends ConfigCommandSupport {

	private static final String PID_FILTER="(service.pid=%s*)";
	private static final String FILE_PREFIX="file:";
	private static final String CONFIG_SUFFIX=".cfg";
	private static final String FACTORY_SEPARATOR = "-";
	private static final String FILEINSTALL_FILE_NAME="felix.fileinstall.filename";

    @Argument(index = 0, name = "pid", description = "PID of the configuration", required = true, multiValued = false)
    String pid;

    @Option(name = "--force", aliases = {}, description = "Force the edition of this config, even if another one was under edition", required = false, multiValued = false)
    boolean force;

	@Option(name = "-f", aliases = {"--use-file"}, description = "Force the edition of this config, even if another one was under edition", required = false, multiValued = false)
    boolean useFile;

	 private File storage;

    protected void doExecute(ConfigurationAdmin admin) throws Exception {
        String oldPid = (String) this.session.get(PROPERTY_CONFIG_PID);
        if (oldPid != null && !oldPid.equals(pid) && !force) {
            System.err.println("Another config is being edited.  Cancel / update first, or use the --force option");
            return;
        }
	    Dictionary props;

	    //User selected to use file instead.
	    if (useFile) {
		    Configuration configuration = this.findConfigurationByFileName(admin, pid);
		    if(configuration == null) {
			    System.err.println("Could not find configuration with file install property set to:"+pid);
			    return;
		    }
		    props = configuration.getProperties();
		    pid = (String) configuration.getPid();
	    } else {
		    props = admin.getConfiguration(pid).getProperties();
		    if (props == null) {
			    props = new Properties();
		    }
	    }
        this.session.put(PROPERTY_CONFIG_PID, pid);
        this.session.put(PROPERTY_CONFIG_PROPS, props);
    }

	/**
	 * <p>
	 * Returns the Configuration object of the given (felix fileinstall) file name.
	 * </p>
	 * @param fileName
	 * @return
	 */
	public Configuration findConfigurationByFileName(ConfigurationAdmin admin, String fileName) throws IOException, InvalidSyntaxException {
		if (fileName != null && fileName.contains(FACTORY_SEPARATOR)) {
			String factoryPid = fileName.substring(0, fileName.lastIndexOf(FACTORY_SEPARATOR));
			String absoluteFileName = FILE_PREFIX+storage.getAbsolutePath() + File.separator + fileName + CONFIG_SUFFIX;
			Configuration[] configurations = admin.listConfigurations(String.format(PID_FILTER, factoryPid));
			if (configurations != null) {
				for (Configuration configuration : configurations) {
					Dictionary dictionary = configuration.getProperties();
					if (dictionary != null) {
						String fileInstallFileName = (String) dictionary.get(FILEINSTALL_FILE_NAME);
						if (absoluteFileName.equals(fileInstallFileName)) {
							return configuration;
						}
					}
				}
			}
		}
		return null;
	}

	public File getStorage() {
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }
}
