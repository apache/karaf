/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.moduleloader.search.compatibility;

import org.apache.osgi.moduleloader.search.CompatibilityPolicy;

/**
 * This class implements a simple version numbering compatibility policy for the
 * <tt>ImportSearchPolicy</tt> where only exact version numbers are considered
 * to be compatible.  This policy simply returns the result of
 * "<tt>leftId.equals(rightId) && leftVersion.equals(rightVersion)</tt>". Any
 * calls to the <tt>compare()</tt> method result in an exception since this
 * policy has no basis for comparing identifiers and versions.
 * @see org.apache.osgi.moduleloader.search.CompatibilityPolicy
 * @see org.apache.osgi.moduleloader.search.ImportSearchPolicy
**/
public class ExactCompatibilityPolicy implements CompatibilityPolicy
{
    /**
     * Compares two versioned identifiers, but since this policy has
     * no understanding of how to compare identifiers, it always throws
     * an <tt>IllegalArgumentException</tt>.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>0</tt> if the identifiers are equal, <tt>-1</tt> if the
     *         left identifier is less then the right identifier, and <tt>1</tt>
     *         if the left identifier is greater than the right identifier.
     * @throws java.lang.IllegalArgumentException if the two identifiers
     *         are not comparable, i.e., they refer to completely different
     *         entities.
    **/
    public int compare(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion)
    {
        throw new IllegalArgumentException("Identifiers are not comparable.");
    }

    /**
     * Returns whether the first import/export target is compatible
     * with the second. This method simply uses the "<tt>equals()</tt>" method
     * to test both the identifier and the verison number.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>true</tt> if the left version number object is compatible
     *         with the right version number object, otherwise <tt>false</tt>.
    **/
    public boolean isCompatible(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion)
    {
        return leftId.equals(rightId) && leftVersion.equals(rightVersion);
    }
}