package org.apache.karaf.bundle.command.wikidoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Parses wiki syntax from a reader and calls a Wikivisitor with the 
 * tokens it finds
 */
public class WikiParser {
	WikiVisitor visitor;
	
	public WikiParser(WikiVisitor visitor) {
		this.visitor = visitor;
	}

	public void parse(String line) {
		StringTokenizer tokenizer = new StringTokenizer(line , "[h*", true);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if ("[".equals(token)) {
				parseLink(tokenizer);
			} else if ("h".equals(token)) {
				parseHeading(tokenizer);
			} else if ("*".equals(token)){
				parseEnumeration(tokenizer);
			} else {
				visitor.text(token);
			}
		}
	}
	
	private void parseEnumeration(StringTokenizer tokenizer) {
		String text = tokenizer.nextToken("-\n");
		visitor.enumeration(text.trim());
	}

	private void parseHeading(StringTokenizer tokenizer) {
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
		String heading = tokenizer.hasMoreTokens() ? tokenizer.nextToken("\n") : "";
		visitor.heading(new Integer(level), heading.trim());
	}

	private void parseLink(StringTokenizer tokenizer) {
		String token = tokenizer.nextToken("]");
		visitor.link(token, "");
		tokenizer.nextToken();
	}

	public void parse(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;
		while ((line = br.readLine()) != null) {
			parse(line);
			visitor.text("\n");
		}
	}

}
