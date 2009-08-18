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
package org.apache.felix.karaf.web;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.felix.karaf.main.Main;
import org.apache.felix.karaf.main.Bootstrap;

public class WebAppListener implements ServletContextListener {
	
	private Main main;
	
	public void contextInitialized(ServletContextEvent sce) {
		try {
			System.err.println("contextInitialized");
			String root = new File(sce.getServletContext().getRealPath("/") + "WEB-INF/karaf").getAbsolutePath();
			System.err.println("Root: " + root);
			System.setProperty("karaf.home", root);
			System.setProperty("karaf.base", root);
			System.setProperty("karaf.startLocalConsole", "false");
			System.setProperty("karaf.startRemoteShell", "true");
			main = Bootstrap.launch(new String[0]);
		} catch (Exception e) {
			main = null;
			e.printStackTrace();
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		try {
			System.err.println("contextDestroyed");
			if (main != null) {
				main.destroy(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
