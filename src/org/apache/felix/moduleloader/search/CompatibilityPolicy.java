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
package org.apache.felix.moduleloader.search;

/**
 * <p>
 * This interface represents the naming and version numbering policy of
 * import and export identifiers for the <tt>ImportSearchPolicy</tt>. A concrete
 * implementation of this interface is required to create an instance
 * of <tt>ImportSearchPolicy</tt>. The sole purpose of this interface
 * is to allow the <tt>ImportSearchPolicy</tt> to determine if one
 * import/export identifier and version is compatible with another.
 * </p>
 * @see org.apache.felix.moduleloader.search.ImportSearchPolicy
**/
public interface CompatibilityPolicy
{
    /**
     * Compares two import/export identifiers.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>0</tt> if the identifiers are equal, <tt>-1</tt> if the
     *         left identifier is less then the right identifier, and <tt>1</tt>
     *         if the left identifier is greater than the right identifier.
     * @throws java.lang.IllegalArgumentException if the two identifiers
     *         are not comparable, i.e., they refer to intrinsically different
     *         entities.
    **/
    public int compare(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion);

    /**
     * Returns whether the first import/export identifer is compatible
     * with the second; this method should not throw any exceptions.
     * @param leftId the identifier to test for compatibility.
     * @param leftVersion the version number to test for compatibility.
     * @param rightId the identifier used as the compatibility base line.
     * @param rightVersion the version used as the compatibility base line.
     * @return <tt>true</tt> if the left version number object is compatible
     *         with the right version number object, otherwise <tt>false</tt>.
    **/
    public boolean isCompatible(
        Object leftId, Object leftVersion,
        Object rightId, Object rightVersion);
}