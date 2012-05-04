package org.apache.karaf.shell.commands;

import java.lang.annotation.Annotation;

public class HelpOption {

    public static final Option HELP = new Option() {
        public String name() {
            return "--help";
        }

        public String[] aliases() {
            return new String[]{};
        }

        public String description() {
            return "Display this help message";
        }

        public boolean required() {
            return false;
        }

        public boolean multiValued() {
            return false;
        }

        public String valueToShowInHelp() {
            return Option.DEFAULT_STRING;
        }

        public Class<? extends Annotation> annotationType() {
            return Option.class;
        }
    };
}
