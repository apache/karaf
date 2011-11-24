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
package org.apache.karaf.shell.http;

import java.util.Arrays;

import javax.servlet.Servlet;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.util.CommandUtils;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.WebEvent;


@Command(scope = "http", name = "list", description = "Lists details for servlets.")
public class ServletListCommand extends OsgiCommandSupport {

	private ServletEventHandler eventHandler;
	
	@Override
	protected Object doExecute() throws Exception {
		
		String headers = " ID   Servlet                        Servlet-Name              State         Alias              Url             ";
		System.out.println(headers);
		for (ServletEvent event : eventHandler.getServletEvents()) {
			Servlet servlet = event.getServlet();
			String servletClassName = " ";
			if (servlet != null) {
				servletClassName = servlet.getClass().getName();
				servletClassName = servletClassName.substring(servletClassName.lastIndexOf(".")+1, servletClassName.length());
			} 
			servletClassName = CommandUtils.trimToSize(servletClassName, 28);
			String servletName = event.getServletName() != null ? event.getServletName() : " ";
			if (servletName.contains(".")) {
				servletName = servletName.substring(servletName.lastIndexOf(".")+1, servletName.length());
			}
			
			servletName = CommandUtils.trimToSize(servletName, 23);
			
			String alias = event.getAlias() != null ? event.getAlias() : " ";
			
			alias = CommandUtils.trimToSize(alias, 16);
			
			String[] urls = (String[]) (event.getUrlParameter() != null ? event.getUrlParameter() : new String[] {""});
			String line = "[" + event.getBundle().getBundleId() + "] [" + servletClassName + "] [" + servletName +"] [" 
			+ getStateString(event.getType())+ "] [" + alias + "] [" + Arrays.toString(urls) + "]";
			System.out.println(line);
		}
		return null;
	}
	
	public String getStateString(int type)
    {
        switch(type) {
		case WebEvent.DEPLOYING:
			return "Deploying  ";
		case WebEvent.DEPLOYED:
			return "Deployed   ";
		case WebEvent.UNDEPLOYING:
			return "Undeploying";
		case WebEvent.UNDEPLOYED:
			return "Undeployed ";
		case WebEvent.FAILED:
			return "Failed     ";
		case WebEvent.WAITING:
			return "Waiting    ";
		default:
			return "Failed     ";
		}
    }

	/**
	 * @return the eventHandler
	 */
	public ServletEventHandler getEventHandler() {
		return eventHandler;
	}

	/**
	 * @param eventHandler the eventHandler to set
	 */
	public void setEventHandler(ServletEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

}
