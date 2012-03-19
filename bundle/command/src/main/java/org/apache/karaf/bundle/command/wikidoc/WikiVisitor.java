package org.apache.karaf.bundle.command.wikidoc;

/**
 * Will be used by WikiParser to call the respective handler when it recognizes the tag 
 */
public interface WikiVisitor {
	void link(String target, String title);
	void heading(int level, String title);
	void enumeration(String text);
	void text(String text);
}