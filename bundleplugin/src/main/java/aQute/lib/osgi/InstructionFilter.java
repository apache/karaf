/* Copyright 2009 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.File;
import java.io.FileFilter;

public class InstructionFilter implements FileFilter {

	private Instruction instruction;
	private boolean recursive;
	
	public InstructionFilter (Instruction instruction, boolean recursive) {
		this.instruction = instruction;
		this.recursive = recursive;
	}
	public boolean isRecursive() {
		return recursive;
	}
	public boolean accept(File pathname) {
		if (Analyzer.doNotCopy.matcher(pathname.getName()).matches()) {
			return false;
		}

		if (pathname.isDirectory() && isRecursive()) {
			return true;
		}
		
		if (instruction == null) {
			return true;
		}
		return !instruction.isNegated() & instruction.matches(pathname.getName());
	}
}
