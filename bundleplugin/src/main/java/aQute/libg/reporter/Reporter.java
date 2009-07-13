package aQute.libg.reporter;

import java.util.*;


public interface Reporter {
	void error(String s, Object ... args);
	void warning(String s, Object ... args);
	void progress(String s, Object ... args);
	void trace(String s, Object ... args);
	List<String> getWarnings();
	List<String> getErrors();
	
	boolean isPedantic();
}
