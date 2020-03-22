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
		"* enumeration - with additional text\n" +
		" some text [a link] some more text\n" +
		"# a comment in between\n" +
		"h1 is no heading\n" +
		"some **bold** text\n" +
		"and a line for KARAF-6650 h\n";
	
	private static final String HEADINGCASES = 
		"h1.\n" +
		"hf.";

	@Test
	public void parseTestDoc() throws IOException {
		WikiVisitor visitor = EasyMock.createStrictMock(WikiVisitor.class);
		visitor.startPara(0);
		visitor.heading(1, "myTestdoc");
		visitor.endPara();
		visitor.startPara(0);
		visitor.endPara();
		visitor.startPara(0);
		visitor.text("Some text");
		visitor.endPara();
		visitor.startPara(0);
		visitor.enumeration("enumeration");
		visitor.endPara();
		visitor.startPara(0);
		visitor.enumeration("enumeration");
		visitor.text("- wit");
		visitor.text("h additional text");
		visitor.endPara();
		visitor.startPara(1);
		visitor.text("some text ");
		visitor.link("a link", "");
		visitor.text(" some more text");
		visitor.endPara();
		visitor.startPara(0);
		visitor.text("h1 is no heading");
		visitor.endPara();
		visitor.startPara(0);
		visitor.text("some ");
		visitor.bold(true);
		visitor.text("bold");
		visitor.bold(false);
		visitor.text(" text");
		visitor.endPara();
		visitor.startPara(0);
		visitor.text("and a line for KARAF-6650 ");
		visitor.text("h");
		visitor.endPara();

		EasyMock.replay(visitor);
		WikiParser parser = new WikiParser(visitor);
		parser.parse(new StringReader(TESTDOC));
		EasyMock.verify(visitor);
	}
	
	@Test
	public void parseHeadingSpecialCases() throws IOException {
		WikiVisitor visitor = EasyMock.createStrictMock(WikiVisitor.class);
		visitor.startPara(0);
		EasyMock.expectLastCall();
		visitor.heading(1, "");
		visitor.endPara();
		visitor.startPara(0);
		visitor.text("hf.");
		visitor.endPara();
		
		EasyMock.replay(visitor);
		WikiParser parser = new WikiParser(visitor);
		parser.parse(new StringReader(HEADINGCASES));
		EasyMock.verify(visitor);
	}
}
