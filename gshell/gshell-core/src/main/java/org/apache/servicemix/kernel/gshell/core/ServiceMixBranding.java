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
package org.apache.servicemix.kernel.gshell.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.geronimo.gshell.ansi.AnsiBuffer;
import org.apache.geronimo.gshell.ansi.AnsiCode;
import org.apache.geronimo.gshell.ansi.AnsiRenderWriter;
import org.apache.geronimo.gshell.application.model.Branding;

public class ServiceMixBranding extends Branding {		
   
    private String prompt; 
    private String[] banner;
    private String displayName;
    private String displayVersion;    
    private String displayLocation;
    private String applicationName;
    private String applicationVersion;
    private String applicationLocation;
    
    private String[] kernelBanner = {
        " ____                  _          __  __ _      ",
        "/ ___|  ___ _ ____   _(_) ___ ___|  \\/  (_)_  __",
        "\\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\ |\\/| | \\ \\/ /",
        " ___) |  __/ |   \\ V /| | (_|  __/ |  | | |>  < ",
        "|____/ \\___|_|    \\_/ |_|\\___\\___|_|  |_|_/_/\\_\\",
    };

    public ServiceMixBranding() {
    	banner = kernelBanner;
    	displayName = "ServiceMix Kernel";
    	displayLocation = "http://servicemix.apache.org/kernel/";    	
    }
    
    public void setEmbeddedResource(URL embeddedResource) {    	
    	Properties brandProps = loadPropertiesFile(embeddedResource);        
    	String brandBanner = brandProps.getProperty("banner");
    	int i = 0;
        char quot = '"';

        StringTokenizer st = new StringTokenizer(brandBanner, ",");
        banner = new String[st.countTokens()];

        while (st.hasMoreTokens()) {        	
            banner[i] = st.nextToken();
            banner[i] = banner[i].substring(1, banner[i].lastIndexOf(quot));        
            i++;
        }

    	applicationName = brandProps.getProperty("application.name");  
    	applicationVersion = brandProps.getProperty("application.version");
    	applicationLocation = brandProps.getProperty("application.location");
    }    
    
    public String getName() {
        return "servicemix";
    }
        
    public String getDisplayName() {    	
        return displayName;
    }
        
    public void setVersion(String version) {
    	displayVersion = version;
    }
    
    public String getVersion() {
    	return displayVersion;
    }        
    
    public String getApplicationName() {
    	return applicationName;
    }

    public String getApplicationVersion() {
    	return applicationVersion;
    }
    
    public String getProgramName() {
        throw new UnsupportedOperationException();
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getAboutMessage() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new AnsiRenderWriter(writer);

        out.println("For information about @|cyan " + displayName + "|, visit:");
        out.println("    @|bold " + displayLocation + "| ");
        out.flush();

        if (applicationName != null && applicationVersion != null) {
        	out.println();
        	out.println(applicationName + " " + applicationVersion);
        	out.println();
        	if (applicationLocation != null) {
                out.println("For information about @|cyan " + applicationName + "|, visit:");
                out.println("    @|bold " + applicationLocation + "| ");
                out.flush();
        	}
        }

        return writer.toString();
    }      
    
    public String getWelcomeMessage() {    	        
        StringWriter writer = new StringWriter();
        PrintWriter out = new AnsiRenderWriter(writer);
    	
        AnsiBuffer buff = new AnsiBuffer();        
                
        for (String line : banner) {
            buff.attrib(line, AnsiCode.CYAN);
            out.println(buff);
        }

        out.println();
        out.println(" @|bold " + displayName + "| (" + displayVersion + ")");
        if (applicationName != null && applicationVersion != null) {
            out.println(" @|bold " + applicationName + "| (" + applicationVersion + ")");
        }
        out.println();
        out.println("Type '@|bold help|' for more information.");
        out.flush();

        return writer.toString();
    }
            
    private static Properties loadPropertiesFile(URL brandPropURL) {
        // Read the properties file.
        Properties brandProps = new Properties();
        InputStream is = null;
        try {
            is = brandPropURL.openConnection().getInputStream();
            brandProps.load(is);
            is.close();
        }
        catch (FileNotFoundException ex) {
            // Ignore file not found.
        }
        catch (Exception ex) {
            System.err.println(
                    "Error loading embedded properties from " + brandPropURL);
            System.err.println("ServicemixBranding: " + ex);
            try {
                if (is != null) is.close();
            }
            catch (IOException ex2) {
                // Nothing we can do.
            }
            return null;
        }
       
        return brandProps;
    }
    
}

