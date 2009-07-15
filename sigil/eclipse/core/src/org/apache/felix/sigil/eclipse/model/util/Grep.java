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

package org.apache.felix.sigil.eclipse.model.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class Grep {

	// Pattern used to parse lines
	private static Pattern linePattern = Pattern.compile(".*\r?\n");
	
	// The input pattern that we're looking for
	private Pattern pattern;
	
	private CharBuffer cb;

	private FileChannel fc;
	
	private Grep(IFile f, Pattern pattern) throws IOException, CoreException {
		this.pattern = pattern;
		cb = openBuffer(f);			
	}
	
	private CharBuffer openBuffer(IFile f) throws IOException, CoreException {
		Charset charset = Charset.forName(f.getCharset());
		CharsetDecoder decoder = charset.newDecoder();
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f.getLocation().toFile());
		fc = fis.getChannel();

		// Get the file's size and then map it into memory
		int sz = (int) fc.size();
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

		// Decode the file into a char buffer
		return decoder.decode(bb);
	}

	public static String[] grep(Pattern pattern, IFile...files) throws CoreException {
		LinkedList<String> matches = new LinkedList<String>();
		for ( IFile f : files ) {
			try {
				Grep g = new Grep( f, pattern );
				g.grep(matches);
				g.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return matches.toArray( new String[matches.size()]);
	}

	private void close() throws IOException {
		fc.close();
	}

	// Use the linePattern to break the given CharBuffer into lines, applying
	// the input pattern to each line to see if we have a match
	//
	private void grep(List<String> matches) {
		Matcher lm = linePattern.matcher(cb); // Line matcher
		Matcher pm = null; // Pattern matcher
		int lines = 0;
		while (lm.find()) {
			lines++;
			CharSequence cs = lm.group(); // The current line
			if (pm == null)
				pm = pattern.matcher(cs);
			else
				pm.reset(cs);
			if (pm.find())
				matches.add(pm.group());
			if (lm.end() == cb.limit())
				break;
		}		
	}
}
