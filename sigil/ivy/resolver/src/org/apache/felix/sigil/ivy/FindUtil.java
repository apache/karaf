/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.sigil.ivy;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;

public class FindUtil
{
    static final String WILD_ANY = "[^.].*";
    static final String WILD_ONE = "[^.][^;]*"; // WILD_ONE.endsWith(WILD_ANY) == false

    // example pattern: ${repository}/projects/abc*/*/project.sigil
    public static Collection<File> findFiles(String pattern) throws IOException
    {
        int star = pattern.indexOf("*");
        if (star == -1)
        {
            throw new IOException("pattern doesn't contain '*': " + pattern);
        }
        int slash = pattern.lastIndexOf('/', star);

        String regex = pattern.substring((slash == -1) ? 0 : slash + 1);
        regex = regex.replaceAll("\\*\\*", "-wildany-");
        regex = regex.replaceAll("\\*", "-wildone-");
        regex = regex.replaceAll("-wildany-", WILD_ANY);
        regex = regex.replaceAll("-wildone-", WILD_ONE);

        String[] patterns = regex.split("/");

        TreeSet<File> list = new TreeSet<File>();
        File root = new File(slash == -1 ? "." : pattern.substring(0, slash));

        if (root.isDirectory())
        {
            findFiles(root, 0, patterns, list);
        }
        else
        {
            throw new IOException("pattern root directory does not exist: " + root);
        }

        return list;
    }

    private static void findFiles(File dir, int level, String[] patterns,
        Collection<File> list)
    {
        final String filePattern = patterns[patterns.length - 1];
        final String dirPattern;

        if (level < patterns.length - 1)
        {
            dirPattern = patterns[level];
        }
        else
        {
            dirPattern = "/"; // impossible to match marker
        }

        final boolean stillWild;
        if ((level > 0) && (level < patterns.length)
            && patterns[level - 1].endsWith(WILD_ANY))
        {
            stillWild = true;
        }
        else
        {
            stillWild = false;
        }

        for (File path : dir.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                String name = pathname.getName();
                if (pathname.isDirectory())
                {
                    return name.matches(dirPattern)
                        || (stillWild && name.matches(WILD_ANY));
                }
                else if (dirPattern.equals("/") || dirPattern.equals(WILD_ANY))
                {
                    return name.matches(filePattern);
                }
                else
                {
                    return false;
                }
            }
        }))
        {
            if (path.isDirectory())
            {
                int inc = path.getName().matches(dirPattern) ? 1 : 0;
                findFiles(path, level + inc, patterns, list);
            }
            else
            {
                list.add(path);
            }
        }
    }
}
