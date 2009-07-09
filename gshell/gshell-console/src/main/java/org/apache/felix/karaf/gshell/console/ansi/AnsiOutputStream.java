package org.apache.felix.karaf.gshell.console.ansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public class AnsiOutputStream extends FilterOutputStream
{
    char[] buf = new char[16];
    int count = 0;
    int ansiCodeState;

    public AnsiOutputStream(OutputStream out)
    {
        super(out);
    }

    @Override
    public void write(int b) throws IOException
    {
        if (ansiCodeState == 0) {
            if (b == '@') {
                ansiCodeState = 1;
            } else {
                super.write(b);
            }
        } else if (ansiCodeState == 1) {
            if (b == '|') {
                ansiCodeState = 2;
                count = 0;
            } else {
                super.write('@');
                super.write(b);
                ansiCodeState = 0;
            }
        } else if (ansiCodeState == 2) {
            if (b == ',') {
                write(AnsiCode.valueOf(new String(buf, 0, count).toUpperCase()));
                count = 0;
            } else if (b == ' ') {
                write(AnsiCode.valueOf(new String(buf, 0, count).toUpperCase()));
                ansiCodeState = 3;
            } else if (count < buf.length) {
                buf[count++] = (char) b;
            } else {
                throw new IOException("Unknown ANSI code (too long): " + new String(buf, 0, count));
            }
        } else if (ansiCodeState == 3) {
            if (b == '|') {
                write(AnsiCode.OFF);
                ansiCodeState = 0;
            } else if (b == '\\') {
                ansiCodeState = 4;
            } else {
                super.write(b);
            }
        } else if (ansiCodeState == 4) {
            if (b != '|') {
                super.write('\\');
            }
            super.write(b);
            ansiCodeState = 3;
        } else {
            throw new IllegalStateException();
        }
    }

    protected void write(AnsiCode code) throws IOException {
        super.write(27); // ESC
        super.write('[');
        if (code.code >= 10) {
            super.write((code.code / 10) + '0');
        }
        super.write((code.code % 10) + '0');
        super.write('m');
    }

    public static String decode(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AnsiOutputStream aos = new AnsiOutputStream(baos);
        aos.write(str.getBytes());
        aos.close();
        return baos.toString();
    }

}
