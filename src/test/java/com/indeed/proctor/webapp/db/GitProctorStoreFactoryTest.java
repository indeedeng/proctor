package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.cache.GlobalCachingProctorStore;
import com.indeed.proctor.webapp.extensions.GlobalCacheStore;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class GitProctorStoreFactoryTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GlobalCacheStore globalCacheStore;

    @Mock
    private ProctorStore proctorStore;

    @Mock
    private ScheduledExecutorService executor;

    private GitProctorStoreFactory gitProctorStoreFactory;

    @Before
    public void setUp() throws IOException, ConfigurationException {
        gitProctorStoreFactory = createGitProctorStoreFactory(globalCacheStore);
    }

    @Test
    public void testCreateStoreWithoutGlobalCache() throws IOException, ConfigurationException {
        final GitProctorStoreFactory gitProctorStoreFactoryWithoutGlobalCache = createGitProctorStoreFactory(null);
        final ProctorStore store = gitProctorStoreFactoryWithoutGlobalCache.createStoreWithGlobalCache("trunk", proctorStore);
        assertThat(store).isNotInstanceOf(GlobalCachingProctorStore.class);
    }

    @Test
    public void testCreateStoreWithGlobalCache() {
        final ProctorStore store = gitProctorStoreFactory.createStoreWithGlobalCache("trunk", proctorStore);
        assertThat(store).isInstanceOf(GlobalCachingProctorStore.class);

    }

    @Test
    public void testCreateStoreWithInvalidBranch() {
        expectedException.expect(NullPointerException.class);
        gitProctorStoreFactory.createStoreWithGlobalCache("unknown", proctorStore);
    }

    private GitProctorStoreFactory createGitProctorStoreFactory(
            final GlobalCacheStore globalCacheStore
    ) throws IOException, ConfigurationException {
        return new GitProctorStoreFactory(
                "test.git",
                "test-user",
                "password",
                "/definition/",
                "/",
                20,
                10,
                10,
                true,
                globalCacheStore
        );
    }
}