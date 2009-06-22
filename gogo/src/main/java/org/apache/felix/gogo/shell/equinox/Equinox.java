package org.apache.felix.gogo.shell.equinox;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.command.*;
import org.osgi.service.component.*;
import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.startlevel.*;

public class Equinox implements Converter {
    BundleContext         context;
    PackageAdmin          pka;
    LogReaderService      lrs;
    StartLevel            sls;
    final static String[] functions = { "active", "bundles", "close", "diag",
            "exec", "exit", "fork", "gc", "getprop", "headers", "init",
            "install", "launch", "log", "packages", "packages", "refresh",
            "services", "setbsl", "setfwsl", "setibsl", "setprop", "shutdown",
            "sl", "ss", "start", "status", "stop", "uninstall", "update" };

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "eqx");
        dict.put(CommandProcessor.COMMAND_FUNCTION, functions);
        this.context.registerService( Converter.class.getName(), this, dict);
    }

    BundleContext getContext() {
        return context;
    }

    public void setPka(PackageAdmin pka) {
        this.pka = pka;
    }

    public void setLrs(LogReaderService lrs) {
        this.lrs = lrs;
    }

    public void setSls(StartLevel sls) {
        this.sls = sls;
    }

    /**
     * - Displays unsatisfied constraints for the specified bundle(s).
     */
    public void diag() {
    }

    /*
     * active - Displays a list of all bundles currently in the ACTIVE state.
     */
    public List<Bundle> active() {
        List<Bundle> result = new ArrayList<Bundle>();
        Bundle[] bundles = getContext().getBundles();
        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE)
                result.add(b);
        }
        return result;
    }

    /*
     * getprop { name } - Displays the system properties with the given name, or
     * all of them.
     */

    public Object getprop(CharSequence name) {
        if (name == null)
            return System.getProperties();
        else
            return System.getProperty(name.toString());
    }

    /**
     * launch - start the OSGi Framework
     */

    public void launch() {
        throw new IllegalStateException("Already running");
    }

    /**
     * shutdown - shutdown the OSGi Framework
     */
    public void shutdown() throws BundleException {
        getContext().getBundle().stop();
    }

    /**
     * close - shutdown and exit
     */
    public void close(CommandSession session) {
        session.close();
    }

    /**
     * exit - exit immediately (System.exit)
     */

    public void exit(int exitValue) {
        exit(exitValue);
    }

    /**
     * gc - perform a garbage collection
     */
    public long gc() {
        Runtime.getRuntime().gc();
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * init - uninstall all bundles
     */

    public void init() throws Exception {
        Bundle bundles[] = getContext().getBundles();
        for (Bundle b : bundles)
            if (b.getBundleId() != 0)
                b.uninstall();
    }

    /**
     * setprop <key>=<value> - set the OSGi property
     */
    public void setprop(CommandSession session, String key, String value) {
        session.put(key, value);
    }

    /**
     * install - install and optionally start bundle from the given URL
     * 
     * @throws BundleException
     */

    public Bundle install(URL url) throws BundleException {
        return getContext().installBundle(url.toExternalForm());
    }

    /**
     * uninstall - uninstall the specified bundle(s)
     * 
     * @throws BundleException
     */
    public void uninstall(Bundle[] bundles) throws BundleException {
        for (Bundle b : bundles) {
            b.uninstall();
        }
    }

    /**
     * start - start the specified bundle(s)
     */
    public void start(Bundle[] bundles) throws BundleException {
        for (Bundle b : bundles) {
            b.start();
        }
    }

    /**
     * stop - stop the specified bundle(s)
     */
    public void stop(Bundle[] bundles) throws BundleException {
        for (Bundle b : bundles) {
            b.stop();
        }
    }

    /**
     * refresh - refresh the packages of the specified bundles
     */
    public void refresh(Bundle[] bundles) throws Exception {
        if (pka != null)
            pka.refreshPackages(bundles);
        else
            throw new RuntimeException("No PackageAdmin service registered");
    }

    /**
     * update - update the specified bundle(s)
     */
    public void update(Bundle[] bundles) throws BundleException {
        for (Bundle b : bundles) {
            b.update();
        }
    }

    /**
     * status - display installed bundles and registered services
     */
    public List<Object> status() throws Exception {
        List<Object> status = new ArrayList<Object>();
        status.addAll(Arrays.asList(getContext().getBundles()));
        status.addAll(Arrays.asList(getContext().getServiceReferences(null,
                null)));
        return status;
    }

    /**
     * ss - display installed bundles (short status)
     */
    public Bundle[] ss() {
        return getContext().getBundles();
    }

    /**
     * services {filter} - display registered service details
     */
    public ServiceReference[] services(String filter) throws Exception {
        return getContext().getServiceReferences(null, filter);
    }

    /**
     * packages {<pkgname>|<id>|<location>} - display imported/exported
     * package details
     */
    public ExportedPackage[] packages(Bundle bundle) throws Exception {
        if (pka != null)
            return pka.getExportedPackages(bundle);
        else
            throw new RuntimeException("No PackageAdmin service registered");
    }

    public ExportedPackage[] packages(String packageName) throws Exception {
        if (pka != null)
            return pka.getExportedPackages(packageName);
        return null;
    }

    /**
     * bundles - display details for all installed bundles
     */
    public Bundle[] bundles() {
        return ss();
    }

    /**
     * bundle (<id>|<location>) - display details for the specified bundle(s)
     */

    /**
     * headers (<id>|<location>) - print bundle headers
     */

    @SuppressWarnings("unchecked")
    public Dictionary headers(Bundle b, String locale) {
        return b.getHeaders(locale);
    }

    /**
     * log (<id>|<location>) - display log entries
     */

    @SuppressWarnings("unchecked")
    public Collection<LogEntry> log(Bundle bundle) throws Exception {
        if (lrs != null)
            return Collections.list((Enumeration<LogEntry>) lrs.getLog());
        else
            throw new RuntimeException("LogReader not available");
    }

    /**
     * exec <command> - execute a command in a separate process and wait
     * 
     * @throws IOException
     */

    public int exec(Object[] args, boolean fork) throws IOException {
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Object arg : args) {
            sb.append(del);
            sb.append(arg);
            del = " ";
        }
        Process p = Runtime.getRuntime().exec(sb.toString());
        if (fork) {
            int c;
            while ((c = p.getInputStream().read()) > 0)
                System.out.print(c);
        }
        return p.exitValue();
    }

    /**
     * fork <command> - execute a command in a separate process
     */

    public void fork(Object args[]) throws Exception {
        exec(args, true);
    }

    /**
     * sl {(<id>|<location>)} - display the start level for the specified
     * bundle, or for the framework if no bundle specified
     */
    public int sl(Bundle b) throws Exception {
        if (sls == null)
            throw new RuntimeException("No StartLevel service registered");
        if (b == null)
            return sls.getStartLevel();
        else
            return sls.getBundleStartLevel(b);
    }

    /**
     * setfwsl <start level> - set the framework start level
     */
    public int setfwsl(int n) throws Exception {
        if (sls == null)
            throw new RuntimeException("No StartLevel service registered");
        int old = sls.getStartLevel();
        sls.setStartLevel(n);
        return old;
    }

    /**
     * setbsl <start level> (<id>|<location>) - set the start level for the
     * bundle(s)
     */
    public int setbsl(Bundle b, int n) throws Exception {
        if (sls == null)
            throw new RuntimeException("No StartLevel service registered");
        int old = sls.getBundleStartLevel(b);
        sls.setBundleStartLevel(b, n);
        return old;
    }

    /**
     * setibsl <start level> - set the initial bundle start level
     */
    public int setibsl(int n) throws Exception {
        if (sls == null)
            throw new RuntimeException("No StartLevel service registered");
        int old = sls.getInitialBundleStartLevel();
        sls.setInitialBundleStartLevel(n);
        return old;
    }

    public Object convert(Class<?> desiredType, Object in) throws Exception {
        return null;
    }

    String getLevel(int index) {
        switch (index) {
        case LogService.LOG_DEBUG:
            return "DEBUG";
        case LogService.LOG_INFO:
            return "INFO ";
        case LogService.LOG_WARNING:
            return "WARNI";
        case LogService.LOG_ERROR:
            return "ERROR";
        default:
            return "<" + index + ">";
        }
    }

    public CharSequence format(Object target, int level, Converter escape) {
        if (target instanceof LogEntry) {
            LogEntry entry = (LogEntry) target;
            switch (level) {
            case LINE:
                Formatter f = new Formatter();
                f.format("%tT %04d %s %s", entry.getTime(), entry.getBundle()
                        .getBundleId(), getLevel(entry.getLevel())+"", entry
                        .getMessage()+"");
                return f.toString();
                
            case PART:
                Formatter f2 = new Formatter();
                f2.format("%tT %s", entry.getTime(), entry
                        .getMessage());
                return f2.toString();
            }
        }
        return null;
    }
    /**
     * profilelog - Display & flush the profile log messages
     */

}
