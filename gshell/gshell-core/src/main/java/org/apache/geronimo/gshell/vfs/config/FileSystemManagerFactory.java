package org.apache.geronimo.gshell.vfs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.vfs.FilesCache;
import org.apache.commons.vfs.CacheStrategy;
import org.apache.commons.vfs.FileContentInfoFactory;
import org.apache.commons.vfs.impl.DefaultFileReplicator;
import org.apache.commons.vfs.impl.PrivilegedFileReplicator;
import org.apache.commons.vfs.impl.FileContentInfoFilenameFactory;
import org.apache.commons.vfs.cache.SoftRefFilesCache;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.TemporaryFileStore;
import org.apache.commons.vfs.provider.FileProvider;
import org.apache.commons.vfs.provider.url.UrlFileProvider;

/**
 * Factory to construct a {@link org.apache.commons.vfs.FileSystemManager} instance.
 *
 * @version $Rev: 707031 $ $Date: 2008-10-22 13:08:07 +0200 (Wed, 22 Oct 2008) $
 */
public class FileSystemManagerFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private FilesCache filesCache;

    private CacheStrategy cacheStrategy = CacheStrategy.ON_RESOLVE;

    private FileReplicator fileReplicator;

    private TemporaryFileStore temporaryFileStore;

    private FileContentInfoFactory fileContentInfoFactory;

    private FileProvider defaultProvider;

    // FileObjectDecorator (Class/Constructor of DecoratedFileObject? or make a factory?)

    public void setFilesCache(final FilesCache cache) {
        this.filesCache = cache;
    }

    public void setCacheStrategy(final CacheStrategy strategy) {
        this.cacheStrategy = strategy;
    }

    public void setFileReplicator(final FileReplicator replicator) {
        this.fileReplicator = replicator;
    }

    public void setTemporaryFileStore(final TemporaryFileStore store) {
        this.temporaryFileStore = store;
    }

    public void setFileContentInfoFactory(final FileContentInfoFactory factory) {
        this.fileContentInfoFactory = factory;
    }

    public void setDefaultProvider(final FileProvider provider) {
        this.defaultProvider = provider;
    }

    public void init() {
        if (filesCache == null) {
            filesCache = new SoftRefFilesCache();
        }

        if (fileReplicator == null || temporaryFileStore == null) {
            DefaultFileReplicator replicator = new DefaultFileReplicator();
            if (fileReplicator == null) {
                fileReplicator = new PrivilegedFileReplicator(replicator);
            }
            if (temporaryFileStore == null) {
                temporaryFileStore = replicator;
            }
        }

        if (fileContentInfoFactory == null) {
            fileContentInfoFactory = new FileContentInfoFilenameFactory();
        }

        if (defaultProvider == null) {
            defaultProvider = new UrlFileProvider();
        }
    }

    //
    // FactoryBean
    //

    public ConfigurableFileSystemManager getFileSystemManager() throws Exception {
        ConfigurableFileSystemManager fsm = new ConfigurableFileSystemManager();

        assert fileReplicator != null;
        log.debug("File replicator: {}", fileReplicator);
        fsm.setReplicator(fileReplicator);

        assert temporaryFileStore != null;
        log.debug("Temporary file store: {}", temporaryFileStore);
        fsm.setTemporaryFileStore(temporaryFileStore);

        assert filesCache != null;
        log.debug("Files cache: {}", filesCache);
        fsm.setFilesCache(filesCache);

        assert cacheStrategy != null;
        log.debug("Cache strategy: {}", cacheStrategy);
        fsm.setCacheStrategy(cacheStrategy);

        assert fileContentInfoFactory != null;
        log.debug("File content info factory: {}", fileContentInfoFactory);
        fsm.setFileContentInfoFactory(fileContentInfoFactory);

        assert defaultProvider != null;
        log.debug("Default provider: {}", defaultProvider);
        fsm.setDefaultProvider(defaultProvider);

        // Finally init the manager
        fsm.init();

        return fsm;
    }

}
