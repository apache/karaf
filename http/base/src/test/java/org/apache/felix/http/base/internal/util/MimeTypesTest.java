/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.util;

import org.junit.Test;
import org.junit.Assert;

public class MimeTypesTest
{
    @Test
    public void testSingleton()
    {
        MimeTypes m1 = MimeTypes.get();
        MimeTypes m2 = MimeTypes.get();

        Assert.assertNotNull(m1);
        Assert.assertSame(m1, m2);

    }

    @Test
    public void testGetByFile()
    {
        Assert.assertNull(MimeTypes.get().getByFile(null));
        Assert.assertEquals("text/plain", MimeTypes.get().getByFile("afile.txt"));
        Assert.assertEquals("text/xml", MimeTypes.get().getByFile(".xml"));
        Assert.assertNull(MimeTypes.get().getByFile("xml"));
        Assert.assertNull(MimeTypes.get().getByFile("somefile.notfound"));
    }

    @Test
    public void testGetByExtension()
    {
        Assert.assertNull(MimeTypes.get().getByExtension(null));
        Assert.assertEquals("text/plain", MimeTypes.get().getByExtension("txt"));
        Assert.assertEquals("text/xml", MimeTypes.get().getByExtension("xml"));
        Assert.assertNull(MimeTypes.get().getByExtension("notfound"));
    }
}
