package org.apache.karaf.jaas.command.completers;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;

public class RealmCompleter implements Completer {

    private List<JaasRealm> realms;

    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            if (realms != null && !realms.isEmpty())
                for (JaasRealm realm : realms) {
                    delegate.getStrings().add(realm.getName());
                }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public List<JaasRealm> getRealms() {
        return realms;
    }

    public void setRealms(List<JaasRealm> realms) {
        this.realms = realms;
    }

}
