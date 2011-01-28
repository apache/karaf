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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;

import org.apache.karaf.features.FeaturesService;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class KarArtifactInstallerTest {
	private KarArtifactInstaller karArtifactInstaller;
	private FeaturesService featuresService;

	private URI goodKarFile;
	private URI zipFileWithKarafManifest;
	private URI zipFileWithoutKarafManifest;
	private URI badZipFile;
	
	@Before
	public void setUp() throws Exception {
		featuresService = createMock(FeaturesService.class);

		karArtifactInstaller = new KarArtifactInstaller();

		karArtifactInstaller.setFeaturesService(featuresService);
		karArtifactInstaller.setBasePath("./target");
		karArtifactInstaller.setLocalRepoPath("./target/local-repo");

		karArtifactInstaller.init();
		
		goodKarFile = getClass().getClassLoader().getResource("goodKarFile.kar").toURI();
		zipFileWithKarafManifest = getClass().getClassLoader().getResource("karFileAsZip.zip").toURI();
		zipFileWithoutKarafManifest = getClass().getClassLoader().getResource("karFileAsZipNoManifest.zip").toURI();
		badZipFile = getClass().getClassLoader().getResource("badZipFile.zip").toURI();
	}
	
	@After
	public void destroy() throws Exception { 
		karArtifactInstaller.destroy();
		karArtifactInstaller.deleteLocalRepository();
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

	@Test 
	public void shouldRecognizeGoodFeaturesFile() throws Exception
	{
		File goodFeaturesXml = new File(getClass().getClassLoader().getResource("goodKarFile/org/foo/goodFeaturesXml.xml").getFile());
		Assert.assertTrue(karArtifactInstaller.isFeaturesRepository(goodFeaturesXml));
	}
	
	@Test 
	public void shouldRejectNonFeaturesXMLFile() throws Exception
	{
		File goodFeaturesXml = new File(getClass().getClassLoader().getResource("badFeaturesXml.xml").toURI());
		Assert.assertFalse(karArtifactInstaller.isFeaturesRepository(goodFeaturesXml));
	}
	
	
	@Test 
	public void shouldRejectMalformedXMLFile() throws Exception
	{
		File malformedXml = new File(getClass().getClassLoader().getResource("malformedXml.xml").toURI());
		Assert.assertFalse(karArtifactInstaller.isFeaturesRepository(malformedXml));
	}
	
	@Test
	public void shouldExtractAndRegisterFeaturesFromKar() throws Exception { 
		// Setup expectations on the features service
		featuresService.addRepository(EasyMock.anyObject(URI.class));
		EasyMock.replay(featuresService);
		
		// Test
		//
		File goodKarFile = new File(getClass().getClassLoader().getResource("goodKarFile.kar").getFile());
		karArtifactInstaller.install(goodKarFile);
		
		// Verify expectations.
		//
		EasyMock.verify(featuresService);
	}
	
	@Test
	public void shouldLogAndNotThrowExceptionIfCannotAddToFeaturesRepository() throws Exception { 
		// Setup expectations on the features service
		featuresService.addRepository(EasyMock.anyObject(URI.class));
		EasyMock.expectLastCall().andThrow(new Exception("Unable to add to repository."));
		EasyMock.replay(featuresService);
		
		// Test
		//
		File goodKarFile = new File(getClass().getClassLoader().getResource("goodKarFile.kar").getFile());
		karArtifactInstaller.install(goodKarFile);
		
		// Verify expectations.
		//
		EasyMock.verify(featuresService);
	}	
	
	@Test
	public void shouldIgnoreUpdateIfFileHasNotChanged() throws Exception { 
		// Setup expectations on the features service: the addRepository 
		// should only be added once, as the update command should be ignored! 
		//
		featuresService.addRepository(EasyMock.anyObject(URI.class));
		EasyMock.replay(featuresService);
		
		// Test
		//
		File goodKarFile = new File(getClass().getClassLoader().getResource("goodKarFile.kar").getFile());
		karArtifactInstaller.install(goodKarFile);
		karArtifactInstaller.update(goodKarFile);
		
		// Verify expectations.
		//
		EasyMock.verify(featuresService);
	}		
	
	@Test
	public void shouldExtractAndRegisterFeaturesFromZip() throws Exception { 
		// Setup expectations on the features service
		featuresService.addRepository(EasyMock.anyObject(URI.class));
		EasyMock.replay(featuresService);
		
		// Test
		//
		File karFileAsZip = new File(getClass().getClassLoader().getResource("karFileAsZip.zip").getFile());
		karArtifactInstaller.install(karFileAsZip);
		
		// Verify expectations.
		//
		EasyMock.verify(featuresService);
	}
	
	@Test (expected = java.io.IOException.class) 
	public void shouldThrowExceptionIfFileDoesNotExist() throws Exception
	{
		File nonExistantFile = new File("DoesNotExist");
		karArtifactInstaller.install(nonExistantFile);
	}	
	
	@Test
	public void uninstallShouldDoNothing() throws Exception
	{
		EasyMock.replay(featuresService);
		
		// Test
		//
		File karFileAsZip = new File(getClass().getClassLoader().getResource("karFileAsZip.zip").getFile());
		karArtifactInstaller.uninstall(karFileAsZip);
		
		// Verify expectations.
		//
		EasyMock.verify(featuresService);
	}

    @Test
    public void testPathToMvnUri() throws Exception {
        URI uri = KarArtifactInstaller.pathToMvnUri("org/apache/geronimo/features/org.apache.geronimo.transaction.kar/3.1.1-SNAPSHOT/org.apache.geronimo.transaction.kar-3.1.1-SNAPSHOT-feature.xml");
        assert "mvn:org.apache.geronimo.features/org.apache.geronimo.transaction.kar/3.1.1-SNAPSHOT/xml/feature".equals(uri.toString());
    }
			
}
