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
