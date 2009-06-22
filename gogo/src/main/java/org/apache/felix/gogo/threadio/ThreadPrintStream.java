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

public class ThreadPrintStream extends PrintStream {
	PrintStream dflt;
	ThreadLocal<PrintStream>	map  = new ThreadLocal<PrintStream>();
	
	public ThreadPrintStream(PrintStream out) {
		super(out);
		dflt = out;
	}

	public void write(byte[] buffer, int offset, int length) {
		getCurrent().write(buffer, offset, length);
	}

	public void write(byte[] buffer) throws IOException {
		getCurrent().write(buffer);
	}

	public PrintStream getCurrent() {
		PrintStream out = map.get();
		if (out != null)
			return out;
		return dflt;
	}

	public void write(int b) {
		getCurrent().write(b);
	}

	public void setStream(PrintStream out) {
		if (out != dflt && out != this) {
			map.set(out);
		}
		else {
			map.remove();			
		}
	}

	public void end() {
		map.remove();
	}
	

}
