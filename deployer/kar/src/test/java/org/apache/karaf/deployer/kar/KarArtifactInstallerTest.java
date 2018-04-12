/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.deployer.kar;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.kar.KarService;
import org.junit.Before;
import org.junit.Test;

public class KarArtifactInstallerTest {
	private KarArtifactInstaller karArtifactInstaller;
	private KarService karService;

	private URI goodKarFile;
	private URI zipFileWithKarafManifest;
	private URI zipFileWithoutKarafManifest;
	private URI badZipFile;
	
	@Before
	public void setUp() throws Exception {
		karService = createMock(KarService.class);

		karArtifactInstaller = new KarArtifactInstaller();

		karArtifactInstaller.setKarService(karService);
		
		goodKarFile = getClass().getClassLoader().getResource("goodKarFile.kar").toURI();
		zipFileWithKarafManifest = getClass().getClassLoader().getResource("karFileAsZip.zip").toURI();
		zipFileWithoutKarafManifest = getClass().getClassLoader().getResource("karFileAsZipNoManifest.zip").toURI();
		badZipFile = getClass().getClassLoader().getResource("badZipFile.zip").toURI();
	}

	@Test
	public void shouldHandleKarFile() throws Exception {
		assertTrue(karArtifactInstaller.canHandle(new File(goodKarFile)));
	}
	
	@Test
	public void shouldHandleZipFileWithKarafManifest() throws Exception {
		assertTrue(karArtifactInstaller.canHandle(new File(zipFileWithKarafManifest)));
	}

	@Test
	public void shouldIgnoreZipFileWithoutKarafManifest() throws Exception { 		
		assertFalse(karArtifactInstaller.canHandle(new File(zipFileWithoutKarafManifest)));
	}

	@Test
	public void shouldIgnoreBadZipFile() throws Exception { 		
		assertFalse(karArtifactInstaller.canHandle(new File(badZipFile)));
	}

    /**
     * This is test for KARAF-5533. Issue comes from fact that internally KAR service process file in it's own way.
     *
     * Because of that artifact installer must follow the same logic of service it calls, as returned list of installed.
     * KAR files is not list of full file names, but files with stripped extensions.
     *
     * @throws Exception Any exception causes test failure.
     */
    @Test
    public void shouldNotInstallSameFileTwice() throws Exception {
        File file = new File(goodKarFile);
        URI uri = file.toURI();

        // make sure we have clean state.
        presentKarList(Collections.emptyList());
        karService.install(uri);

        replay(karService);

        karArtifactInstaller.install(file);
        verify(karService);

        // once again,
        reset(karService);
        presentKarList(Collections.singletonList(karArtifactInstaller.getKarName(file)));
        replay(karService);
        karArtifactInstaller.install(file);
        verify(karService);
    }

    private void presentKarList(List<String> deployedKars) throws Exception {
        expect(karService.list()).andReturn(deployedKars).once();
    }
}
