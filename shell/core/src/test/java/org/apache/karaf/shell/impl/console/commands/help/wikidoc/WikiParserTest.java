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
            "h1. myTestdoc\n"
                    + "\n"
                    + "Some text\n"
                    + "* enumeration\n"
                    + " some text [a link] some more text\n"
                    + "h1 is no heading";

    private static final String HEADINGCASES = "h1.\n" + "hf.";

    @Test
    public void parseTestDoc() throws IOException {
        WikiVisitor visitor = EasyMock.createStrictMock(WikiVisitor.class);
        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.heading(1, "myTestdoc");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();
        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();
        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.text("Some text");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();
        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.enumeration("enumeration");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();
        visitor.startPara(1);
        EasyMock.expectLastCall();
        visitor.text("some text ");
        EasyMock.expectLastCall();
        visitor.link("a link", "");
        EasyMock.expectLastCall();
        visitor.text(" some more text");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();
        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.text("h1 is no heading");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();

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
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();

        visitor.startPara(0);
        EasyMock.expectLastCall();
        visitor.text("hf.");
        EasyMock.expectLastCall();
        visitor.endPara();
        EasyMock.expectLastCall();

        EasyMock.replay(visitor);
        WikiParser parser = new WikiParser(visitor);
        parser.parse(new StringReader(HEADINGCASES));
        EasyMock.verify(visitor);
    }
}
