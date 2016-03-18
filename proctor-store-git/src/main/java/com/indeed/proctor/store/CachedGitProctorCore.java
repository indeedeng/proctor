package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.indeed.proctor.store.StoreException.ReadException;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author atran
 */
public class CachedGitProctorCore extends GitProctorCore {
    private final Cache<FileContentsKey, Object> cache = CacheBuilder.newBuilder()
        .maximumSize(2048)
        .expireAfterAccess(6, TimeUnit.HOURS) // We use revision/hash as part of the key so we don't need to worry about cache entry becoming invalid
        .softValues()
        .build();

    public CachedGitProctorCore(final String gitUrl,
                                final String username,
                                final String password,
                                final String testDefinitionsDirectory,
                                final GitWorkspaceProviderImpl workspaceProvider) {
        super(gitUrl, username, password, testDefinitionsDirectory, workspaceProvider);
    }

    @Override
    public <C> C getFileContents(final Class<C> c,
                                 final String[] path,
                                 final C defaultValue,
                                 final String revision) throws ReadException, JsonProcessingException {
        final FileContentsKey key = new FileContentsKey(c, path, revision);
        final Object obj = cache.getIfPresent(key);
        if (obj == null) {
            final C newObj = super.getFileContents(c, path, defaultValue, revision);
            if (newObj != defaultValue) {
                cache.put(key, newObj);
            }
            return newObj;
        } else {
            if (c.isAssignableFrom(obj.getClass())) {
                return c.cast(obj);
            }
            return super.getFileContents(c, path, defaultValue, revision);
        }
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws ReadException {
        // Extremely fast in git - no caching required
        return super.determineVersions(fetchRevision);
    }

    private static class FileContentsKey {
        final Class c;
        final String[] path;
        final String revision;

        private FileContentsKey(Class c, String[] path, String revision) {
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

            if (revision != null ? !revision.equals(that.revision) : that.revision != null) {
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
            result = 31 * result + (revision != null ? revision.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return Joiner.on('/').join(path) + "@" + revision;
        }
    }
}