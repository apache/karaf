package org.apache.geronimo.gshell.branding;

import java.io.PrintWriter;
import java.io.StringWriter;

import jline.Terminal;
import org.apache.geronimo.gshell.ansi.Buffer;
import org.apache.geronimo.gshell.ansi.Code;
import org.apache.geronimo.gshell.ansi.RenderWriter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 25, 2007
 * Time: 10:14:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceMixBranding extends BrandingSupport {

    private static final String[] BANNER = {
        " ____                  _          __  __ _       ",
        "/ ___|  ___ _ ____   _(_) ___ ___|  \\/  (_)_  __",
        "\\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\ |\\/| | \\ \\/ /",
        " ___) |  __/ |   \\ V /| | (_|  __/ |  | | |>  <",
        "|____/ \\___|_|    \\_/ |_|\\___\\___|_|  |_|_/_/\\_\\",
    };

    private VersionLoader versionLoader;

    private Terminal terminal;

    public ServiceMixBranding(final VersionLoader versionLoader, final Terminal terminal) {
        this.versionLoader = versionLoader;
        this.terminal = terminal;
    }

    public String getName() {
        return "servicemix";
    }

    public String getDisplayName() {
        return "ServiceMix";
    }

    public String getProgramName() {
        return System.getProperty("program.name", "gsh");
    }

    public String getAbout() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new RenderWriter(writer);

        out.println("For information about @|cyan ServiceMix|, visit:");
        out.println("    @|bold http://servicemix.apache.org| ");
        out.flush();

        return writer.toString();
    }

    public String getVersion() {
        return versionLoader.getVersion();
    }

    public String getWelcomeBanner() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new RenderWriter(writer);
        Buffer buff = new Buffer();

        for (String line : BANNER) {
            buff.attrib(line, Code.CYAN);
            out.println(buff);
        }

        out.println();
        out.println(" @|bold ServiceMix| (" + getVersion() + ")");
        out.println();
        out.println("Type '@|bold help|' for more information.");

        // If we can't tell, or have something bogus then use a reasonable default
        int width = terminal.getTerminalWidth();
        if (width < 1) {
            width = 80;
        }

        out.print(StringUtils.repeat("-", width - 1));

        out.flush();

        return writer.toString();
    }
}
