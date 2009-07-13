package aQute.libg.version;

import java.util.regex.*;

public class Version implements Comparable<Version> {
    final int                   major;
    final int                   minor;
    final int                   micro;
    final String                qualifier;
    public final static String  VERSION_STRING = "(\\d+)(\\.(\\d+)(\\.(\\d+)(\\.([-_\\da-zA-Z]+))?)?)?";
    public final static Pattern VERSION        = Pattern
                                                       .compile(VERSION_STRING);
    public final static Version LOWEST         = new Version();
    public final static Version HIGHEST        = new Version(Integer.MAX_VALUE,
                                                       Integer.MAX_VALUE,
                                                       Integer.MAX_VALUE,
                                                       "\uFFFF");

    public Version() {
        this(0);
    }

    public Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
    }

    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    public Version(int major, int minor) {
        this(major, minor, 0, null);
    }

    public Version(int major) {
        this(major, 0, 0, null);
    }

    public Version(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches())
            throw new IllegalArgumentException("Invalid syntax for version: "
                    + version);

        major = Integer.parseInt(m.group(1));
        if (m.group(3) != null)
            minor = Integer.parseInt(m.group(3));
        else
            minor = 0;

        if (m.group(5) != null)
            micro = Integer.parseInt(m.group(5));
        else
            micro = 0;

        qualifier = m.group(7);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public String getQualifier() {
        return qualifier;
    }

    public int compareTo(Version other) {
        if (other == this)
            return 0;

        if (!(other instanceof Version))
            throw new IllegalArgumentException(
                    "Can only compare versions to versions");

        Version o = (Version) other;
        if (major != o.major)
            return major - o.major;

        if (minor != o.minor)
            return minor - o.minor;

        if (micro != o.micro)
            return micro - o.micro;

        int c = 0;
        if (qualifier != null)
            c = 1;
        if (o.qualifier != null)
            c += 2;

        switch (c) {
        case 0:
            return 0;
        case 1:
            return 1;
        case 2:
            return -1;
        }
        return qualifier.compareTo(o.qualifier);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(major);
        sb.append(".");
        sb.append(minor);
        sb.append(".");
        sb.append(micro);
        if (qualifier != null) {
            sb.append(".");
            sb.append(qualifier);
        }
        return sb.toString();
    }

    public boolean equals(Object ot) {
        if ( ! (ot instanceof Version))
            return false;
        
        return compareTo((Version)ot) == 0;
    }

    public int hashCode() {
        return major * 97 ^ minor * 13 ^ micro
                + (qualifier == null ? 97 : qualifier.hashCode());
    }

    public int get(int i) {
        switch(i) {
        case 0 : return major;
        case 1 : return minor;
        case 2 : return micro;
        default:
            throw new IllegalArgumentException("Version can only get 0 (major), 1 (minor), or 2 (micro)");
        }
    }
}
