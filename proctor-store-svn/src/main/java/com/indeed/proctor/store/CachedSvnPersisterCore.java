package com.indeed.proctor.store;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.indeed.util.varexport.Export;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.tmatesoft.svn.core.SVNURL;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author parker
 */
public class CachedSvnPersisterCore implements SvnPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(CachedSvnPersisterCore.class);

    private final Cache<FileContentsKey, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(2048)
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .softValues()
        .build();

    private final LoadingCache<Long, TestVersionResult> versionCache = CacheBuilder.newBuilder()
        .maximumSize(50)
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .softValues()
        .build(new CacheLoader<Long, TestVersionResult>() {
            @Override
            public TestVersionResult load(Long revision) {
                try {
                    return core.determineVersions(String.valueOf(revision.longValue()));
                } catch (StoreException.ReadException e) {
                    throw Throwables.propagate(e);
                }
            }
        });

    final SvnPersisterCoreImpl core;

    public CachedSvnPersisterCore(SvnPersisterCoreImpl core) {
        this.core = core;
    }

    @Override
    public <T> T doWithClientAndRepository(final SvnOperation<T> operation) throws StoreException {
        return core.doWithClientAndRepository(operation);
    }

    @Override
    @Export(name = "svn-url")
    public SVNURL getSvnUrl() {
        return core.getSvnUrl();
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        return core.cleanUserWorkspace(username);
    }

    @Override
    public String toString() {
        return "Cached: " + core.getSvnPath();
    }

    @Override
    public <C> C getFileContents(final Class<C> c,
                                 final String[] path,
                                 final C defaultValue,
                                 final String revision) throws StoreException.ReadException, JsonProcessingException {
        final FileContentsKey key = new FileContentsKey(c, path, core.parseRevisionOrDie(revision));
        final Object obj = cache.getIfPresent(key);
        if (obj == null) {
            final C x = core.getFileContents(c, path, defaultValue, revision);
            if (x != defaultValue) {
                cache.put(key, x);
            }
            return x;
        } else {
            if (c.isAssignableFrom(obj.getClass())) {
                return c.cast(obj);
            }
            return core.getFileContents(c, path, defaultValue, revision);
        }
    }

    @Override
    public void doInWorkingDirectory(String username, String password, String comment, String previousVersion, FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        core.doInWorkingDirectory(username, password, comment, previousVersion, updater);
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        // return core.determineVersions(fetchRevision);
        return versionCache.getUnchecked(core.parseRevisionOrDie(fetchRevision));
    }

    @Override
    public String getAddTestRevision() {
        return core.getAddTestRevision();
    }

    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Ignored exception during closing of core", e);
        }
    }

    @Override
    public void close() throws IOException{
        core.close();
    }

    private static class FileContentsKey {
        final Class c;
        final String[] path;
        final long revision;

        private FileContentsKey(Class c, String[] path, long revision) {
            this.c = c;
            this.path = path;
            this.revision = revision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FileContentsKey)) {
                return false;
            }

            FileContentsKey that = (FileContentsKey) o;

            if (revision != that.revision) {
                return false;
            }
            if (c != null ? !c.equals(that.c) : that.c != null) {
                return false;
            }
            if (!Arrays.equals(path, that.path)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = c != null ? c.hashCode() : 0;
            result = 31 * result + (path != null ? Arrays.hashCode(path) : 0);
            result = 31 * result + (int) (revision ^ (revision >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return Joiner.on('/').join(path) + "@r" + revision;
        }
    }
}
