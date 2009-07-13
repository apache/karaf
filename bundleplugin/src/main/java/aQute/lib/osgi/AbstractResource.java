package aQute.lib.osgi;

import java.io.*;

public abstract class AbstractResource implements Resource {
    String extra;
    byte[] calculated;
    long   lastModified;

    protected AbstractResource(long modified) {
        lastModified = modified;
    }

    public String getExtra() {
        return extra;
    }

    public long lastModified() {
        return lastModified;
    }

    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(getLocalBytes());
    }

    private byte[] getLocalBytes() throws IOException {
        try {
            if (calculated != null)
                return calculated;

            return calculated = getBytes();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ee = new IOException("Opening resource");
            ee.initCause(e);
            throw ee;
        }
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public void write(OutputStream out) throws IOException {
        out.write(getLocalBytes());
    }

    abstract protected byte[] getBytes() throws Exception;
}
