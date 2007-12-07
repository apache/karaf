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
package org.apache.geronimo.gshell.osgi;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.geronimo.gshell.ansi.RenderWriter;
import org.apache.geronimo.gshell.branding.BrandingSupport;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 11, 2007
 * Time: 10:20:37 PM
 * To change this template use File | Settings | File Templates.
 */
@CommandComponent(id="osgi:osgi", description="The OSGI Shell")
public class OsgiShell extends OsgiCommandSupport {
	SubShell subShell;

	@Override
	public Object execute(CommandContext context, Object... args) throws Exception {
    	BrandingSupport branding = new BrandingSupport() {
		    public String getAbout() {
		        StringWriter writer = new StringWriter();
		        PrintWriter out = new RenderWriter(writer);

		        out.println("For information about @|cyan ServiceMix OSGI|, visit:");
		        out.println("    @|bold http://servicemix.apache.org| ");
		        out.flush();

		        return writer.toString();
		    }
			
			public String getName() {
				return "osgi";
			}

			@Override
			public String getDisplayName() {
				return "OSGI Shell";
			}
			
			public String getVersion() {
				// TODO: replace this with a version loader.
				return "1.0";
			}

			public String getWelcomeBanner() {
				return "@|bold OSGI Shell| ("+getVersion()+")";
			}
    		
    	};
    	try {
			return subShell.execute(branding, context, args);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	protected Object doExecute() throws Exception {
		return null;
	}

	public SubShell getSubShell() {
		return subShell;
	}

	public void setSubShell(SubShell subShell) {
		this.subShell = subShell;
	}

}
