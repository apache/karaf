package org.apache.felix.framework.ext;

import java.security.Permission;
import java.security.ProtectionDomain;

import org.osgi.framework.Bundle;

public interface SecurityProvider
{
    boolean hasBundlePermission(ProtectionDomain pd, Permission p, boolean direct);

    Object getSignerMatcher(Bundle bundle);
}
