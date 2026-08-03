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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

	private void createTestFiles() throws Exception {
		// create goodKarFile.kar
		File karFile = new File("target/test-classes/goodKarFile.kar");
		karFile.getParentFile().mkdirs();

		URL karFileResource = getClass().getClassLoader().getResource("goodKarFile");
		File karFileSourceDir = new File(karFileResource.toURI());
		createJarFromFolder(karFileSourceDir.toPath(), karFile);

		goodKarFile = karFile.toURI();

		// create karFileAsZip.zip
		File zipFile = new File("target/test-classes/karFileAsZip.zip");
		zipFile.getParentFile().mkdirs();

		URL karFileAsZipResource = getClass().getClassLoader().getResource("karFileAsZip");
		createZipFromFolder(Path.of(karFileAsZipResource.toURI()), zipFile);

		zipFileWithKarafManifest = zipFile.toURI();

		// create karFileAsZipNoManifest.zip
		File zipFileWithoutKarafManifestFile = new File("target/test-classes/karFileAsZipNoManifest.zip");
		zipFileWithoutKarafManifestFile.getParentFile().mkdirs();

		URL karFileAsZipNoManifestResource = getClass().getClassLoader().getResource("karFileAsZipNoManifest");
		createZipFromFolder(Path.of(karFileAsZipNoManifestResource.toURI()), zipFileWithoutKarafManifestFile);

		zipFileWithoutKarafManifest = zipFileWithoutKarafManifestFile.toURI();

		// create badZipFile.zip
		Path badZipFileFile = Path.of("target/test-classes/badZipFile.zip");
		Files.writeString(badZipFileFile, "Not a valid zip file");
		badZipFile = badZipFileFile.toUri();
	}

	private static void createJarFromFolder(Path sourceDir, File jarFile) throws Exception {
		try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
			Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String entryName = sourceDir.relativize(file).toString().replace(File.separator, "/");
					jos.putNextEntry(new ZipEntry(entryName));
					Files.copy(file, jos);
					jos.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static void createZipFromFolder(Path sourceDir, File zipFile) throws Exception {
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
			Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String entryName = sourceDir.relativize(file).toString().replace(File.separator, "/");
					zos.putNextEntry(new ZipEntry(entryName));
					Files.copy(file, zos);
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	@Before
	public void setUp() throws Exception {
		createTestFiles();

		karService = createMock(KarService.class);

		karArtifactInstaller = new KarArtifactInstaller();

		karArtifactInstaller.setKarService(karService);

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
