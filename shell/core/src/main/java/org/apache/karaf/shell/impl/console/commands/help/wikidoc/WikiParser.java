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
package org.apache.karaf.shell.impl.console.commands.help.wikidoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.karaf.util.StringEscapeUtils;

/**
 * Parses wiki syntax from a reader and calls a Wikivisitor with the 
 * tokens it finds
 */
public class WikiParser {

	WikiVisitor visitor;
	
	public WikiParser(WikiVisitor visitor) {
		this.visitor = visitor;
	}

	public void parse(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;
		while ((line = br.readLine()) != null) {
			if (!line.startsWith("#")) {
				parse(line);
			}
		}
	}

	public void parse(String line) {
        String unescaped = StringEscapeUtils.unescapeJava(line);
		Tokenizer tokenizer = new Tokenizer(unescaped);
		String token;
		boolean bold = false;
		boolean first = true;
		while ((token = tokenizer.nextToken("\u001B[h*")) != null) {
			if (first) {
				first = false;
				int tabs = 0;
				for (int i = 0; i < token.length() && token.charAt(i) == '\t'; i++) {
					tabs++;
				}
				token = token.substring(tabs);
				for (int i = 0; i < tabs; i++) {
					token = "    " + token;
				}
				int i = 0;
				while (i < token.length() && token.charAt(i) == ' ') {
					i++;
				}
				visitor.startPara(i);
				token = token.substring(i);
			}
            if ("\u001B".equals(token)) {
                parseEsc(tokenizer, token);
            } else if ("[".equals(token)) {
				parseLink(tokenizer);
			} else if ("h".equals(token)) {
				parseHeading(tokenizer);
			} else if ("*".equals(token)) {
				parseEnumeration(tokenizer);
			} else if ("**".equals(token)) {
				bold = !bold;
				visitor.bold(bold);
			} else {
				visitor.text(token);
			}
		}
		if (first) {
			visitor.startPara(0);
		}
		visitor.endPara();
	}

    private void parseEsc(Tokenizer tokenizer, String token) {
        visitor.text(token + tokenizer.nextToken("\u001B[h*") + tokenizer.nextToken("\u001B[]"));
    }
	
	private void parseEnumeration(Tokenizer tokenizer) {
		String text = tokenizer.nextToken("-\n");
		visitor.enumeration(text.trim());
	}

	private void parseHeading(Tokenizer tokenizer) {
		String level = tokenizer.nextToken("123456789");
		if (!level.matches("[123456789]")) {
			visitor.text("h" + level);
			return;
		}
		String dot = tokenizer.nextToken(".\n");
		if (!".".equals(dot)) {
			visitor.text("h" + level + dot);
			return;
		}
		String heading = tokenizer.nextToken("\n");
		if (heading == null) {
			heading = "";
		}
		visitor.heading(Integer.parseInt(level), heading.trim());
	}

	private void parseLink(Tokenizer tokenizer) {
		String token = tokenizer.nextToken("]");
		visitor.link(token, "");
		tokenizer.nextToken("]");
	}

	public static class Tokenizer {

		final String str;
		int pos;

		public Tokenizer(String str) {
			this.str = str;
		}

		public String nextToken(String delim) {
			StringBuilder sb = new StringBuilder();
			boolean escape = false;
			boolean del = false;
			while (pos < str.length()) {
				char c = str.charAt(pos++);
				if (escape) {
					escape = false;
					sb.append(c);
				} else if (c == '\\') {
					if (del) {
						pos--;
						break;
					} else {
						escape = true;
					}
				} else if (delim.indexOf(c) >= 0) {
					if (sb.length() == 0 || del) {
						sb.append(c);
						del = true;
					} else {
						pos--;
						break;
					}
				} else {
					if (del) {
						pos--;
						break;
					}
					sb.append(c);
				}
			}
			return sb.length() > 0 ? sb.toString() : null;
		}

	}

}
