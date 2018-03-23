/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.deployer.kar;

import java.io.IOException;
import java.io.File;
import java.util.zip.ZipFile;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.karaf.kar.KarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarArtifactInstaller implements ArtifactInstaller {
    
    public static final String FEATURE_CLASSIFIER = "features";

    private static final Logger LOGGER = LoggerFactory.getLogger(KarArtifactInstaller.class);
    
    private final static String KAR_SUFFIX = ".kar";
    private final static String ZIP_SUFFIX = ".zip";

	private KarService karService;

	public void install(File file) throws Exception {
        // check if the KAR is not already installed
        if (karService.list().contains(getKarName(file))) {
            LOGGER.info("KAR {} is already installed. Please uninstall it first.", file.getName());
            return;
        }
        
        LOGGER.info("Installing KAR file {}", file);
        
        karService.install(file.toURI());
	}

    public void uninstall(File file) throws Exception {
        String karName = getKarName(file);
        LOGGER.info("Uninstalling KAR {}", karName);
        karService.uninstall(karName);
	}

	public void update(File file) throws Exception {
        LOGGER.warn("Karaf archive {}' has been updated; redeploying.", file);
        karService.uninstall(getKarName(file));
        karService.install(file.toURI());
	}

    String getKarName(File karFile) {
        String karName = karFile.getName();
        karName = karName.substring(0, karName.lastIndexOf("."));
        return karName;
    }

    public boolean canHandle(File file) {
		// If the file or directory ends with .kar, then we can handle it!
        //
        if (file.getName().endsWith(KAR_SUFFIX)) {
			LOGGER.info("Found a .kar file to deploy.");
			return true;
		}
		// Otherwise, check to see if it's a zip file containing a META-INF/KARAF.MF manifest.
        //
        else if (file.isFile() && file.getName().endsWith(ZIP_SUFFIX)) {
			LOGGER.debug("Found a .zip file to deploy; checking contents to see if it's a Karaf archive.");
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(file);
                if (zipFile.getEntry("META-INF/KARAF.MF") != null) {
					LOGGER.info("Found a Karaf archive with .zip prefix; will deploy.");
                    return true;
                }
	    } catch (Exception e) {
		    LOGGER.warn("Problem extracting zip file '{}'; ignoring.", file.getName(), e);
	    } finally {
                try {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                } catch (IOException e) {
                    LOGGER.warn("Problem closing zip file '{}'; ignoring.", file.getName(), e);
                }
            }
	}

	return false;
    }

    public KarService getKarService() {
        return karService;
    }

    public void setKarService(KarService karService) {
        this.karService = karService;
    }

}
