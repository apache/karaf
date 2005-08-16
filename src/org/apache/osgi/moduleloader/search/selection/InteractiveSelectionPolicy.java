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
package org.apache.osgi.moduleloader.search.selection;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.osgi.moduleloader.Module;
import org.apache.osgi.moduleloader.search.CompatibilityPolicy;
import org.apache.osgi.moduleloader.search.SelectionPolicy;

/**
 * This class implements an interactive selection policy for the
 * <tt>ImportSearchPolicy</tt>. This policy simply uses standard
 * output to present the list of candidate modules and uses standard
 * input to allow the user to select a specific module from the
 * candidates. This selection policy is generally only useful for
 * debugging purposes.
 * @see org.apache.osgi.moduleloader.search.SelectionPolicy
 * @see org.apache.osgi.moduleloader.search.ImportSearchPolicy
**/
public class InteractiveSelectionPolicy implements SelectionPolicy
{
    /**
     * Returns a single package from an array of packages.
     * @param sources array of packages from which to choose.
     * @return the selected package or <tt>null</tt> if no package
     *         can be selected.
    **/
    public Module select(Module module, Object target,
        Object version, Module[] candidates, CompatibilityPolicy compatPolicy)
    {
        try {
            if (candidates.length == 1)
            {
                return candidates[0];
            }
            // Now start an interactive prompt.
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            do
            {
                System.out.println("\nImporting '" + target
                    + "(" + version + ")" + "' for '" + module + "'.");
                System.out.println("");
                for (int i = 0; i < candidates.length; i++)
                {
                    System.out.println((i + 1) + ". " + candidates[i]);
                }
                System.out.print("Select: ");
                String s = br.readLine();

                int choice = -1;
                try {
                    choice = Integer.parseInt(s);
                } catch (Exception ex) {
                }

                if (choice == 0)
                {
                    break;
                }
                else if ((choice > 0) && (choice <= candidates.length))
                {
                    return candidates[choice - 1];
                }
            }
            while (true);
        } catch (Exception ex) {
        }

        return null;
    }
}