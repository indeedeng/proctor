package com.indeed.proctor.store;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.util.concurrent.TimeUnit;

/** @author parker */
final class SvnObjectPools {

    private SvnObjectPools() {
    }

    private static class SVNClientManagerFactory extends BasePooledObjectFactory<SVNClientManager> {

        final String username;
        final String password;
        private final boolean requiresAuthentication;

        private SVNClientManagerFactory(final String username,
                                        final String password) {
            requiresAuthentication = true;
            this.username = username;
            this.password = password;
        }

        private SVNClientManagerFactory() {
            requiresAuthentication = false;
            username = null;
            password = null;
        }

        @Override
        public SVNClientManager create() throws Exception {
            if (requiresAuthentication) {
                final BasicAuthenticationManager authManager = new BasicAuthenticationManager(username, password);
                return SVNClientManager.newInstance(null, authManager);
            } else {
                return SVNClientManager.newInstance();
            }
        }

        @Override
        public PooledObject<SVNClientManager> wrap(final SVNClientManager obj) {
            return new DefaultPooledObject<SVNClientManager>(obj);
        }

        @Override
        public void destroyObject(final PooledObject<SVNClientManager> p) throws Exception {
            final SVNClientManager m = p.getObject();
            m.dispose();
        }

    }

    public static ObjectPool<SVNClientManager> clientManagerPool() {
        return createObjectPool(new SVNClientManagerFactory());
    }

    public static ObjectPool<SVNClientManager> clientManagerPoolWithAuth(final String username, final String password) {
        return createObjectPool(new SVNClientManagerFactory(username, password));
    }

    private static <T> ObjectPool<T> createObjectPool(final PooledObjectFactory<T> factory) {
        final GenericObjectPoolConfig objectPoolConfig = new GenericObjectPoolConfig();
        objectPoolConfig.setMinEvictableIdleTimeMillis(TimeUnit.HOURS.toMillis(1)); // arbitrary, but positive so objects do get evicted
        objectPoolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.MINUTES.toMillis(10)); // arbitrary, but positive so objects do get evicted
        objectPoolConfig.setJmxEnabled(false);
        objectPoolConfig.setBlockWhenExhausted(false);
        objectPoolConfig.setMaxTotal(-1); // uncapped number of objects in the pool
        final AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setRemoveAbandonedTimeout((int) TimeUnit.MINUTES.toSeconds(30));
        return new GenericObjectPool<T>(factory, objectPoolConfig, abandonedConfig);
    }

}
