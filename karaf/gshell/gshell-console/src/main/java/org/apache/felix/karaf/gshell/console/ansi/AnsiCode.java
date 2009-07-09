package org.apache.felix.karaf.gshell.console.ansi;

public enum AnsiCode
{
    OFF(0),
    BOLD(1),
    UNDERSCORE(4),
    BLINK(5),
    REVERSE(7),
    CONCEALED(8),

    FG_BLACK(30),
    FG_RED(31),
    FG_GREEN(32),
    FG_YELLOW(33),
    FG_BLUE(34),
    FG_MAGENTA(35),
    FG_CYAN(36),
    FG_WHITE(37),

    BLACK(FG_BLACK),
    RED(FG_RED),
    GREEN(FG_GREEN),
    YELLOW(FG_YELLOW),
    BLUE(FG_BLUE),
    MAGENTA(FG_MAGENTA),
    CYAN(FG_CYAN),
    WHITE(FG_WHITE),

    BG_BLACK(40),
    BG_RED(41),
    BG_GREEN(42),
    BG_YELLOW(43),
    BG_BLUE(44),
    BG_MAGENTA(45),
    BG_CYAN(46),
    BG_WHITE(47);

    final int code;

    private AnsiCode(final int code) {
        this.code = code;
    }

    private AnsiCode(final AnsiCode code) {
        this(code.code);
    }

}
