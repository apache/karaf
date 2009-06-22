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
package org.apache.felix.gogo.threadio;

import java.io.*;

public class ThreadInputStream extends InputStream {
    ThreadLocal<InputStream> map = new ThreadLocal<InputStream>();
	InputStream dflt;

	public ThreadInputStream(InputStream in) {
		dflt = in;
	}

	public int read(byte[] buffer, int offset, int length) throws IOException {
		return getCurrent().read(buffer, offset, length);
	}

	public int read(byte[] buffer) throws IOException {
		return getCurrent().read(buffer);
	}

	private InputStream getCurrent() {
		InputStream in = map.get();
		if (in != null)
			return in;
		return dflt;
	}

	public int read() throws IOException {
		return getCurrent().read();
	}

	public void setStream(InputStream in) {
		if ( in != dflt && in != this )
			map.set(in);
		else
			map.remove();
	}

	public void end() {
		map.remove();
	}

	InputStream getRoot() {
		return dflt;
	}
}
