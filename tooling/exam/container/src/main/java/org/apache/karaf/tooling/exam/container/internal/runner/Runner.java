package org.apache.karaf.tooling.exam.container.internal.runner;

import java.io.File;

/**
 * Abstracts the runner to be able to add different runners easier.
 */
public interface Runner {

    /**
     * Starts the environment in the specific environment.
     */
    public abstract void
        exec(final String[] environment, final File karafBase, final String javaHome, final String[] javaOpts,
                final String[] javaEndorsedDirs,
                final String[] javaExtDirs, final String karafHome, final String karafData, final String[] karafOpts,
                final String[] opts, final String[] classPath, final String main, final String options);

    /**
     * Shutsdown the runner again.
     */
    public abstract void shutdown();

}
