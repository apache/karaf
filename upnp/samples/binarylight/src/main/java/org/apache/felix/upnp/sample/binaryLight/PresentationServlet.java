/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.upnp.sample.binaryLight;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class PresentationServlet extends HttpServlet
{
	LightModel model;

    public PresentationServlet(LightModel model) {
		this.model = model;
	}

	public void init()
    {
		//ServletConfig config = getServletConfig();
        System.out.println("BinaryLight Servlet init called:" );
    }

    public void destroy() {}



    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        String path = request.getPathInfo();
        if (path != null){
	        if (path.equals("/On"))
	        	model.switchOn();
	        else if (path.equals("/Off"))
	        	model.switchOff();
        }
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<HTML>");
        out.println("<head><title>Apache Felix UPnP BinaryLight</title></head>");
        out.println("<body>");
        out.println("  <center>");
        out.println("  <h1><font face='Arial' color='#808080'>Apache Felix UPnP BinaryLight</font></h1>");
       
        if (model.getStatus()== false){
        	out.println("  <p><a href=/upnp/binaryLight/On><img border='0' src='images/LightOff.gif' width='64' height='64'></a></p>");
        }
        else{       	
        	out.println("  <p><a href=/upnp/binaryLight/Off><img border='0' src='images/LightOn.gif' width='64' height='64'></a></p>");
        }
        
        out.println("  <p><a href=/upnp/binaryLight/>Refresh status</a></p>");
        out.println("  </center>");
        out.println("  </BODY>");
        out.println("</HTML>");
        out.flush();
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

}
