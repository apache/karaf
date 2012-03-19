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
package org.apache.karaf.bundle.command.wikidoc;

import java.io.PrintStream;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

/**
 * Translates the Wiki tags to Ansi escape sequences to display them on the console
 */
public class AnsiPrintingWikiVisitor implements WikiVisitor {
	private PrintStream out;
	
	public AnsiPrintingWikiVisitor(PrintStream out) {
		this.out = out;
	}
	
	@Override
	public void heading(int level, String header) {
		this.out.print(Ansi.ansi().a(Attribute.INTENSITY_BOLD).a(header)
				.a(Attribute.INTENSITY_BOLD_OFF).toString());
	}
	
	@Override
	public void link(String target, String title) {
		this.out.print(Ansi.ansi().fg(Color.YELLOW) 
				.a(target).fg(Color.DEFAULT));
	}

	@Override
	public void enumeration(String text) {
		this.out.print(Ansi.ansi().a(" * ").fg(Color.CYAN).a(text).fg(Color.DEFAULT).a(" "));
	}

	@Override
	public void text(String text) {
		this.out.print(text);
	}


}
