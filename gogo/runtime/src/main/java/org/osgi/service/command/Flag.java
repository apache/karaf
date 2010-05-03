package org.osgi.service.command;

public @interface Flag {
	String name();
	String help() default "no help";
}
