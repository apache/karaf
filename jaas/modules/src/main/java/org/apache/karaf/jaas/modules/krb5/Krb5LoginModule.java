package org.apache.karaf.jaas.modules.krb5;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Karaf Kerberos login module.
 */
public class Krb5LoginModule implements LoginModule {

    private com.sun.security.auth.module.Krb5LoginModule loginModule = new com.sun.security.auth.module.Krb5LoginModule();

    @Override
    public void initialize(Subject _subject, CallbackHandler _callbackHandler, Map<String, ?> _sharedState, Map<String, ?> _options) {
        Map<String, Object> options = new HashMap<>(_options);
        // interpolate system properties like ${karaf.etc} in options
        _options.forEach((key, value) -> {
            if (value instanceof String) {
                options.put(key, Krb5LoginModule.interpolate((String) value));
            }
        });
        this.loginModule.initialize(_subject, _callbackHandler, _sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        return loginModule.login();
    }

    @Override
    public boolean commit() throws LoginException {
        return loginModule.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        return loginModule.abort();
    }

    @Override
    public boolean logout() throws LoginException {
        return loginModule.logout();
    }

    private static String interpolate(String _value) {
        String value = _value;
        Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(value);
        while (matcher.find()) {
            String rep = System.getProperty(matcher.group(1));
            if (rep != null) {
                value = value.replace(matcher.group(0), rep);
                matcher.reset(value);
            }
        }
        return value;
    }
}
