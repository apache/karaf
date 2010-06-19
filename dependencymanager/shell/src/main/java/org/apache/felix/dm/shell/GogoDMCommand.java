package org.apache.felix.dm.shell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.osgi.framework.BundleContext;

/**
 * This class provides DependencyManager commands for the Gogo shell.
 */
public class GogoDMCommand extends DMCommand
{
    public GogoDMCommand(BundleContext context)
    {
        super(context);
    }
    
    public void dmhelp() {
        System.out.println("dependencymanager:dm -> list DM component diagnostics.");
        System.out.println("dependencymanager:dm bundleId -> list DM component diagnostics for a given bundle.");
        System.out.println("dependencymanager:dmnotavail -> list unavailable DM components.");
        System.out.println("dependencymanager:dmnotavail bundleId -> list unavailable DM components for a given bundle.");
        System.out.println("dependencymanager:dmnodeps -> list DM component diagnostics without dependencies.");
        System.out.println("dependencymanager:dmnodeps bundleId-> list DM component diagnostics without dependencies for a given bundle.");
        System.out.println("dependencymanager:dmcompact -> list DM component compact diagnostics.");
        System.out.println("dependencymanager:dmcompact bundleId -> list DM component compact diagnostics for a given bundle.");
    }
    
    public void dm() {
        execute("dm", new String[0]);
    }

    public void dm(int bundleId) {
        execute("dm", new String[] { String.valueOf(bundleId) });
    }

    public void dmnodeps() {
        execute("dm nodeps", new String[0]);
    }
    
    public void dmnodeps(int bundleId) {
        execute("dm nodeps", new String[] { String.valueOf(bundleId) });
    }
    
    public void dmnotavail() {
        execute("dm notavail", new String[0]);
    }
   
   public void dmnotavail(int bundleId) {
       execute("dm notavail", new String[] { String.valueOf(bundleId) });
   }

   public void dmcompact() {
       execute("dm compact", new String[0]);
   }    

   public void dmcompact(int bundleId) {
        execute("dm compact", new String[] { String.valueOf(bundleId) });
   }    
        
   private void execute(String line, String[] args) {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
       PrintStream out = new PrintStream(bytes);
       PrintStream err = new PrintStream(errorBytes);
        
       if (args != null && args.length > 0) {
           line += " " + args[0]; // Add bundle Id
       }
        
       super.execute(line.toString(), out, err);
       if (bytes.size() > 0) {
           System.out.println(new String(bytes.toByteArray()));
       }
       if (errorBytes.size() > 0) {
           System.out.print("Error:\n");
           System.out.println(new String(errorBytes.toByteArray()));
       }
    }
}
