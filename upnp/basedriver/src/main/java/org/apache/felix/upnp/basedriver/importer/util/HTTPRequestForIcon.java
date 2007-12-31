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

package org.apache.felix.upnp.basedriver.importer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

/** 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class HTTPRequestForIcon {
	private URL url;

	public HTTPRequestForIcon(URL url) {
		this.url = url;
	}
	public InputStream getInputStream() throws IOException {
		//TODO we should speak about that
		InetAddress inet = InetAddress.getByName(url.getHost());
		int port = url.getPort();
		Socket socket = null;
		socket = new Socket(inet, port);
		OutputStream out = null;
		out = socket.getOutputStream();
		String CRLF = "\r\n";
		url.getFile();
		String request = "GET " + url.getPath() + " " + "HTTP/1.1" + CRLF
				+ "Host: " + url.getHost() + CRLF + "Connection: " + "close"
				+ CRLF + CRLF;
		//System.out.println(request);
		byte[] get = request.getBytes();
		out.write(get, 0, get.length);
		InputStream in = socket.getInputStream();
		boolean exit = true;
		while (exit) {
			byte[] b = new byte[1];
			in.read(b, 0, b.length);

			if (b[0] == '\r') {
				in.read(b, 0, b.length);
				while (b[0] == '\r') {
					in.read(b, 0, b.length);
				}
				if (b[0] != '\n') {
					continue;
				}
				in.read(b, 0, b.length);
				if (b[0] != '\r') {
					continue;
				}
				in.read(b, 0, b.length);
				if (b[0] != '\n') {
					continue;
				}
				exit = false;
			}
		}

		return in;

		/*
		 * HTTPResponse response=new HTTPResponse(in); 
		 * InputStream  iconInStream=response.getContentInputStream(); 
		 * return iconInStream;
		 * 
		 */
		/*
		 * 
		 * byte[] buff = new byte[maxLength]; int initial = 0; while (initial <
		 * maxLength - 1) { int read = 0; read = in.read(buff, initial, 1024);
		 * if (read == -1) break; initial += read; } System.out.println(new
		 * String(buff));
		 */

	}

}
