package org.apache.karaf.diagnostic.command;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.diagnostic.core.common.ZipDumpDestination;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "dev", name = "dump", description = "Dump data")
public class DumpCommand extends OsgiCommandSupport {

	private List<DumpProvider> providers = new LinkedList<DumpProvider>();

	@Override
	protected Object doExecute() throws Exception {
		ZipDumpDestination destination = new ZipDumpDestination(new File("dump.zip"));
		for (DumpProvider provider : providers) {
			provider.createDump(destination);
		}
		destination.save();

		return null;
	}

	public void setProviders(List<DumpProvider> providers) {
		this.providers = providers;
	}
}
