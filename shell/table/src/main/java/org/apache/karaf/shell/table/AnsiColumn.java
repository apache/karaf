package org.apache.karaf.shell.table;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

public class AnsiColumn extends Col {

    private Color color;
    private boolean bold;

    public AnsiColumn(String header, Color color, boolean bold) {
        super(header);
        this.color = color;
        this.bold = bold;
    }

    @Override
    String getContent(String content) {
        String in = super.getContent(content);

        Ansi ansi = Ansi.ansi();
        ansi.fg(color);

        if (bold)
            ansi.a(Ansi.Attribute.INTENSITY_BOLD);

        ansi.a(in);

        if (bold)
            ansi.a(Ansi.Attribute.INTENSITY_BOLD_OFF);

        ansi.fg(Ansi.Color.DEFAULT);

        return ansi.toString();
    }
}
