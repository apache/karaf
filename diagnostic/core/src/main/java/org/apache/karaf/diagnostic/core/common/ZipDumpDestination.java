/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.core.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.karaf.diagnostic.core.Dump;
import org.apache.karaf.diagnostic.core.DumpDestination;

/**
 * Class which packages dumps to ZIP archive.
 * 
 * @author ldywicki
 */
public class ZipDumpDestination implements DumpDestination {

	private ZipOutputStream outputStream;

	public ZipDumpDestination(File directory, String name) {
		this(new File(directory, name));
	}

	public ZipDumpDestination(File file) {
		try {
			outputStream = new ZipOutputStream(new FileOutputStream(
				file));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Unable to create dump destination", e);
		}
	}

	public void add(Dump... dump) throws Exception {
		for (Dump entry : dump) {
			ZipEntry zipEntry = new ZipEntry(entry.getName());
			outputStream.putNextEntry(zipEntry);

			InputStream in = entry.createResource();
            int len;
            byte[] buf = new byte[1024];

            while ((len = in.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.closeEntry();
		}
	}

	public void save() throws Exception {
		outputStream.close();
	}
	
}
