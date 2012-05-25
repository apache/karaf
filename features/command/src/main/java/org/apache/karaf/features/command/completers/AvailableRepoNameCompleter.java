package org.apache.karaf.features.command.completers;

import java.util.Arrays;
import java.util.List;

import org.apache.karaf.features.command.FeatureFinder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

/**
 * Shows the list of feature repos that can be installed with their short name
 */
public class AvailableRepoNameCompleter implements Completer {

    FeatureFinder featureFinder;

    public void setFeatureFinder(FeatureFinder featureFinder) {
        this.featureFinder = featureFinder;
    }

    public int complete(final String buffer, final int cursor, @SuppressWarnings("rawtypes") final List candidates) {
        StringsCompleter delegate = new StringsCompleter(Arrays.asList(featureFinder.getNames()));
        return delegate.complete(buffer, cursor, candidates);
    }

}
