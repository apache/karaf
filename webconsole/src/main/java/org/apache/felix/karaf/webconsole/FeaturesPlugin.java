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
package org.apache.felix.karaf.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.karaf.gshell.features.FeaturesService;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;

/**
 * Felix Web Console plugin to interact with Karaf features
 *
 * @author Marcin Wilkos
 */
public class FeaturesPlugin extends AbstractWebConsolePlugin implements Servlet {

    private static final String LABEL = "features";
    private static final String TITLE = "Features";
    
    private FeaturesService featuresService;
    
    public FeaturesPlugin() {
        super();
    }

    public String getTitle() {
        return TITLE;
    }

    public String getLabel() {
        return LABEL;
    }

    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter pw = response.getWriter();
        pw.println("<pre>");
        pw.println("</pre>");

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content container'>" + getTitle() + "</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>" );
        pw.println( "<pre>" );

        pw.println("*** Features:");
        String[] features;
        try {
            features = getFeatures();
        } catch (Exception e) {
            throw new ServletException("Unable to fetch features", e);
        }
        for(int i=0; i<features.length;i++){
            pw.println(features[i]);
        }

        pw.println( "</pre>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );
        pw.println( "</table>" );
    }

    /*
     * Get the list of installed/uninstalled features
     */
    private String[] getFeatures() throws Exception {        
        return getFeaturesService().listFeatures();
    }
    
    public FeaturesService getFeaturesService() {
        return featuresService;
    }
    
    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
}
