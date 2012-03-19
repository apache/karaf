package org.apache.karaf.bundle.command.wikidoc;

import java.io.IOException;
import java.io.StringReader;

import org.easymock.EasyMock;
import org.junit.Test;

public class WikiParserTest {

	private static final String TESTDOC = 
		"h1. myTestdoc\n" +
		"\n" +
		"Some text\n" +
		"* enumeration\n" +
		" some text [a link] some more text\n" +
		"h1 is no heading";
	
	private static final String HEADINGCASES = 
		"h1.\n" +
		"hf.";

	@Test
	public void parseTestDoc() throws IOException {
		WikiVisitor visitor = EasyMock.createStrictMock(WikiVisitor.class);
		visitor.heading(1, "myTestdoc");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();
		visitor.text("Some text");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();
		visitor.enumeration("enumeration");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();		
		visitor.text(" some text ");
		EasyMock.expectLastCall();
		visitor.link("a link", "");
		EasyMock.expectLastCall();
		visitor.text(" some more text");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();
		visitor.text("h1 is no heading");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();

		EasyMock.replay(visitor);
		WikiParser parser = new WikiParser(visitor);
		parser.parse(new StringReader(TESTDOC));
		EasyMock.verify(visitor);
	}
	
	@Test
	public void parseHeadingSpecialCases() throws IOException {
		WikiVisitor visitor = EasyMock.createStrictMock(WikiVisitor.class);

		visitor.heading(1, "");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();

		visitor.text("hf.");
		EasyMock.expectLastCall();
		visitor.text("\n");
		EasyMock.expectLastCall();
		
		EasyMock.replay(visitor);
		WikiParser parser = new WikiParser(visitor);
		parser.parse(new StringReader(HEADINGCASES));
		EasyMock.verify(visitor);
	}
}
