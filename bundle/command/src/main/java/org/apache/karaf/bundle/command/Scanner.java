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
package org.apache.karaf.bundle.command;

import java.util.Map;

import org.apache.karaf.bundle.core.BundleScanner;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command( //
scope = "bundle", //
name = "scanner", //
description = "Scans and updates snapshot bundles.", //
detailedDescription = "Scans installed snapshot bundles, updates them from remote maven repository, and re-deploys changed jars in karaf.")
public class Scanner extends AbstractAction {

	@Option(name = "-i", aliases = { "--interval", "--time", "--delay" }, //
	description = "Scan interval.", required = false, multiValued = false)
	protected long interval;

	@Option(name = "-b", aliases = { "--on", "--up", "--start", "--begin" }, //
	description = "Starts scanner.", required = false, multiValued = false)
	protected boolean start;

	@Option(name = "-e", aliases = { "--off", "--down", "--stop", "--end" }, //
	description = "Stops scanner.", required = false, multiValued = false)
	protected boolean stop;

	@Option(name = "-a", aliases = { "--add", "--new" }, //
	description = "Adds new matching pattern.", required = false, multiValued = false)
	protected String add;

	@Option(name = "-r", aliases = { "--del", "--rem", "--kill", "--remove",
			"--delete" }, //
	description = "Removes existing matching pattern.", required = false, multiValued = false)
	protected String remove;

	@Option(name = "-l", aliases = { "--list", "--show", "--pattern" }, //
	description = "Displays matching patterns.", required = false, multiValued = false)
	protected boolean list;

	@Option(name = "-x", aliases = { "--clear-list", "--list-clear",
			"--kill-list" }, //
	description = "Clear matching patterns.", required = false, multiValued = false)
	protected boolean listClear;

	@Option(name = "-s", aliases = { "--stat", "--statistics", "--updates" }, //
	description = "Displays update statistics.", required = false, multiValued = false)
	protected boolean stats;

	@Option(name = "-z", aliases = { "--clear-stat", "--stat-clear",
			"--kill-stat" }, //
	description = "Clear update statistics.", required = false, multiValued = false)
	protected boolean statsClear;

	private BundleScanner bundleScanner;

	public void setBundleScanner(BundleScanner bundleScanner) {
		this.bundleScanner = bundleScanner;
	}

	@Override
	protected Object doExecute() throws Exception {

		/** Keep in order. */

		if (start && stop) {
			System.err
					.println("Please use only one of 'start' and 'stop' options.");
			return null;
		}

		if (interval > 0) {
			bundleScanner.setInterval(interval);
			System.out.println("Setting scanner interval to " + interval
					+ " ms.");
		} else if (interval < 0) {
			System.err.println("Interval must be positive.");
		}

		if (start) {
			if (!bundleScanner.isRunning()) {
				System.out.println("Starting scanner.");
				bundleScanner.start();
			} else {
				System.err.println("Scanner is already running.");
			}
		}

		if (stop) {
			if (bundleScanner.isRunning()) {
				System.out.println("Stopping scanner.");
				bundleScanner.stop();
			} else {
				System.err.println("Scanner was not running.");
			}
		}

		if (add != null) {
			if (bundleScanner.add(add)) {
				System.out.println("Scanner pattern added.");
			} else {
				System.err.println("Scanner pattern ignored.");
			}
		}

		if (remove != null) {
			if (bundleScanner.remove(remove)) {
				System.out.println("Scanner pattern removed.");
			} else {
				System.err.println("Scanner pattern ignored.");
			}
		}

		if (list) {
			System.out.println("Matching patterns:");
			for (String regex : bundleScanner.getPatterns()) {
				System.out.println("\t" + regex);
			}
		}

		if (stats) {
			System.out.println("Update statistics:");
			for (Map.Entry<String, Integer> entry : bundleScanner
					.getStatistics().entrySet()) {
				final String bundle = entry.getKey();
				final Integer counter = entry.getValue();
				System.out.println(String.format("\t%6d %s", counter, bundle));
			}
		}

		if (listClear) {
			bundleScanner.clearPatterns();
			System.out.println("Matching patterns cleared.");
		}

		if (statsClear) {
			bundleScanner.clearStatistics();
			System.out.println("Update statistics cleared.");
		}

		return null;

	}

}
