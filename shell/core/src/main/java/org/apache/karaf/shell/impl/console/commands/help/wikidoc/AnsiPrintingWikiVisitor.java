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

import java.io.PrintStream;

import org.apache.karaf.shell.support.table.ShellTable;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

/**
 * Translates the Wiki tags to Ansi escape sequences to display them on the console
 */
public class AnsiPrintingWikiVisitor implements WikiVisitor {

	private PrintStream out;
	private int maxSize;
	private StringBuilder sb = new StringBuilder();
	private String indent;
	
	public AnsiPrintingWikiVisitor(PrintStream out, int maxSize) {
		this.out = out;
		this.maxSize = maxSize;
	}

	@Override
	public void startPara(int size) {
		indent = "";
		while (size-- > 0) {
			indent += " ";
		}
	}

	@Override
	public void endPara() {
		if (sb.length() > 0) {
			ShellTable table = new ShellTable().noHeaders().separator("").size(maxSize - 1);
			table.column("").maxSize(indent.length());
			table.column("").wrap();
			table.addRow().addContent(indent, sb.toString());
			table.print(out);
			sb.setLength(0);
		} else {
			out.println();
		}
	}

	@Override
	public void heading(int level, String header) {
		sb.append(Ansi.ansi().a(Attribute.INTENSITY_BOLD).a(header)
				.a(Attribute.INTENSITY_BOLD_OFF).toString());
	}
	
	@Override
	public void link(String target, String title) {
		sb.append(Ansi.ansi().fg(Color.YELLOW)
				.a(target).fg(Color.DEFAULT));
	}

	@Override
	public void enumeration(String text) {
		sb.append(Ansi.ansi().a(" * ").fg(Color.CYAN).a(text).fg(Color.DEFAULT).a(" "));
	}

	@Override
	public void text(String text) {
		sb.append(text);
	}

	@Override
	public void bold(boolean on) {
		if (on) {
			sb.append(Ansi.ansi().bold().toString());
		} else {
			sb.append(Ansi.ansi().boldOff().toString());
		}
	}

}
