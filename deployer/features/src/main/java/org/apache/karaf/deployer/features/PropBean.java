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
package org.apache.karaf.deployer.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;

/**
 * Repository/feature install state persistence.
 */
public class PropBean {

	final File file;
	final Properties prop;

	PropBean(final File file) {
		this.file = file;
		this.prop = new Properties();
	}

	/**
	 * Decrement total/local counts if repository/feature is present.
	 */
	boolean checkDecrement(final Repository repo, final Feature feature)
			throws Exception {

		final int total = countValue(null, feature);
		final int local = countValue(repo, feature);

		if (local == 1) {
			countValue(null, feature, total - 1);
			countValue(repo, feature, local - 1);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Increment total/local counts if repository/feature is missing.
	 */
	boolean checkIncrement(final Repository repo, final Feature feature)
			throws Exception {

		final int total = countValue(null, feature);
		final int local = countValue(repo, feature);

		if (local == 0) {
			countValue(null, feature, total + 1);
			countValue(repo, feature, local + 1);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Load properties from file.
	 */
	void propLoad() throws Exception {
		if (!file.exists()) {
			return;
		}
		final InputStream input = new FileInputStream(file);
		try {
			prop.load(input);
		} finally {
			input.close();
		}
	}

	/**
	 * Save properties into file.
	 */
	void propSave() throws Exception {
		final OutputStream output = new FileOutputStream(file);
		try {
			prop.store(output, null);
		} finally {
			output.close();
		}
	}

	/**
	 * Repository/Feature count property name.
	 */
	String countKey(final Repository repo, final Feature feature) {
		final String repoId;
		if (repo == null) {
			repoId = "[repo]";
		} else {
			repoId = repo.getName();
		}
		final String featureId;
		if (feature == null) {
			featureId = "[feature]";
		} else {
			featureId = feature.getId();
		}
		return repoId + "/" + featureId;
	}

	/**
	 * Load repository/feature count.
	 */
	int countValue(final Repository repo, final Feature feature)
			throws Exception {
		propLoad();
		final String key = countKey(repo, feature);
		final String value = prop.getProperty(key, "0");
		return Integer.parseInt(value);
	}

	/**
	 * Save repository/feature count.
	 */
	int countValue(final Repository repo, final Feature feature, final int count)
			throws Exception {
		propLoad();
		final String key = countKey(repo, feature);
		final String value = prop.getProperty(key, "0");
		if (count == 0) {
			prop.remove(key);
		} else {
			prop.setProperty(key, Integer.toString(count));
		}
		propSave();
		return Integer.parseInt(value);
	}

}
