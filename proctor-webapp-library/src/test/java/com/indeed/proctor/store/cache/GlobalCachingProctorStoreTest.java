package com.indeed.proctor.store.cache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.extensions.GlobalCacheStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalCachingProctorStoreTest {
    @Mock
    private GlobalCacheStore globalCacheStore;

    @Mock
    private ProctorStore delegate;

    private GlobalCachingProctorStore globalCachingProctorStore;

    private static String TEST_NAME = "example_tst";
    private static Environment ENVIRONMENT = Environment.PRODUCTION;
    private static TestMatrixVersion TEST_MATRIX_VERSION = createTestMatrixVersion();
    private static TestDefinition TEST_DEFINITION = createTestDefinition();
    private static List<Revision> HISTORY = createHistory();
    private static Map<String, List<Revision>> ALL_HISTORIES = createAllHistories();
    private static String USERNAME = "user";
    private static String PASSWORD = "password";
    private static String AUTHOR = "author";
    private static String REVISION = "abc";
    private static Map<String, String> METADATA = Collections.emptyMap();
    private static String COMMENT = "test comment";

    @Before
    public void setUp() {
        globalCachingProctorStore = new GlobalCachingProctorStore(delegate, globalCacheStore, ENVIRONMENT);
    }

    @Test
    public void tesGetName() {
        final String proctorStoreName = "TestProctorStore";
        when(delegate.getName()).thenReturn(proctorStoreName);
        assertThat(globalCachingProctorStore.getName()).isEqualTo(proctorStoreName);
    }

    @Test
    public void testGetCurrentTestMatrix() throws StoreException {
        when(delegate.getCurrentTestMatrix()).thenReturn(TEST_MATRIX_VERSION);
        final TestMatrixVersion actualTestMatrixVersion = globalCachingProctorStore.getCurrentTestMatrix();
        assertThat(actualTestMatrixVersion).isEqualToComparingFieldByFieldRecursively(TEST_MATRIX_VERSION);
    }

    @Test
    public void testGetCurrentTestDefinitionWhenGlobalCacheHas() throws StoreException {
        when(globalCacheStore.getCachedTestDefinition(Environment.PRODUCTION, TEST_NAME)).thenReturn(Optional.of(TEST_DEFINITION));
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getCurrentTestDefinition(TEST_NAME);
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(TEST_DEFINITION);
    }

    @Test
    public void testGetCurrentTestDefinitionWhenGlobalCacheDoesNotHave() throws StoreException {
        when(globalCacheStore.getCachedTestDefinition(ENVIRONMENT, TEST_NAME)).thenReturn(Optional.empty());
        when(delegate.getCurrentTestDefinition(TEST_NAME)).thenReturn(TEST_DEFINITION);
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getCurrentTestDefinition(TEST_NAME);
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(TEST_DEFINITION);
    }

    @Test
    public void testVerifySetup() throws StoreException {
        doNothing().when(delegate).verifySetup();
        globalCachingProctorStore.verifySetup();
        verify(delegate, times(1)).verifySetup();
    }

    @Test
    public void testCleanUserWorkspace() {
        when(delegate.cleanUserWorkspace(USERNAME)).thenReturn(true);
        final boolean isSuccess = globalCachingProctorStore.cleanUserWorkspace(USERNAME);
        verify(delegate, times(1)).cleanUserWorkspace(USERNAME);
        assertThat(isSuccess).isTrue();
    }

    @Test
    public void testUpdateTestDefinition() throws StoreException {
        doNothing().when(delegate).updateTestDefinition(
                USERNAME, PASSWORD, AUTHOR, REVISION, TEST_NAME, TEST_DEFINITION, METADATA, COMMENT);
        doNothing().when(globalCacheStore).updateCache(ENVIRONMENT, TEST_NAME, TEST_DEFINITION, HISTORY);
        when(delegate.getHistory(TEST_NAME, 0, Integer.MAX_VALUE)).thenReturn(HISTORY);
        globalCachingProctorStore.updateTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                REVISION,
                TEST_NAME,
                TEST_DEFINITION,
                METADATA,
                COMMENT
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).updateTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                REVISION,
                TEST_NAME,
                TEST_DEFINITION,
                METADATA,
                COMMENT
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                ENVIRONMENT,
                TEST_NAME,
                TEST_DEFINITION,
                HISTORY
        );
    }

    @Test
    public void testDeleteTestDefinition() throws StoreException {
        doNothing().when(delegate).deleteTestDefinition(
                USERNAME, PASSWORD, AUTHOR, REVISION, TEST_NAME, TEST_DEFINITION, COMMENT);
        doNothing().when(globalCacheStore).updateCache(ENVIRONMENT, TEST_NAME, TEST_DEFINITION, HISTORY);
        when(delegate.getHistory(TEST_NAME, 0, Integer.MAX_VALUE)).thenReturn(HISTORY);
        globalCachingProctorStore.deleteTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                REVISION,
                TEST_NAME,
                TEST_DEFINITION,
                COMMENT
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).deleteTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                REVISION,
                TEST_NAME,
                TEST_DEFINITION,
                COMMENT
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                ENVIRONMENT,
                TEST_NAME,
                null,
                HISTORY
        );
    }

    @Test
    public void testAddTestDefinition() throws StoreException {
        doNothing().when(delegate).addTestDefinition(
                USERNAME, PASSWORD, AUTHOR, TEST_NAME, TEST_DEFINITION, METADATA, COMMENT);
        doNothing().when(globalCacheStore).updateCache(ENVIRONMENT, TEST_NAME, TEST_DEFINITION, HISTORY);
        when(delegate.getHistory(TEST_NAME, 0, Integer.MAX_VALUE)).thenReturn(HISTORY);
        globalCachingProctorStore.addTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                TEST_NAME,
                TEST_DEFINITION,
                METADATA,
                COMMENT
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).addTestDefinition(
                USERNAME,
                PASSWORD,
                AUTHOR,
                TEST_NAME,
                TEST_DEFINITION,
                METADATA,
                COMMENT
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                ENVIRONMENT,
                TEST_NAME,
                TEST_DEFINITION,
                HISTORY
        );
    }

    @Test
    public void testGetLatestVersion() throws StoreException {
        when(delegate.getLatestVersion()).thenReturn(REVISION);
        final String version = globalCachingProctorStore.getLatestVersion();
        assertThat(version).isEqualTo(REVISION);
    }

    @Test
    public void testGetTestMatrix() throws StoreException {
        when(delegate.getTestMatrix(REVISION)).thenReturn(TEST_MATRIX_VERSION);
        final TestMatrixVersion actualTestMatrixVersion = globalCachingProctorStore.getTestMatrix(REVISION);
        assertThat(actualTestMatrixVersion).isEqualToComparingFieldByFieldRecursively(TEST_MATRIX_VERSION);
    }

    @Test
    public void testGetTestDefinitionWhenGlobalCacheHas() throws StoreException {
        when(globalCacheStore.getCachedTestDefinition(ENVIRONMENT, TEST_NAME, REVISION)).thenReturn(Optional.of(TEST_DEFINITION));
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getTestDefinition(TEST_NAME, REVISION);
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(actualTestDefinition);
    }

    @Test
    public void testGetTestDefinitionWhenGlobalCacheDoesNotHave() throws StoreException {
        when(globalCacheStore.getCachedTestDefinition(ENVIRONMENT, TEST_NAME, REVISION)).thenReturn(Optional.empty());
        when(delegate.getTestDefinition(anyString(), anyString())).thenReturn(TEST_DEFINITION);
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getTestDefinition(TEST_NAME, REVISION);
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(actualTestDefinition);
    }

    @Test
    public void testGetMatrixHistory() throws StoreException {
        when(delegate.getMatrixHistory(0, 10)).thenReturn(HISTORY);
        final List<Revision> actualHistory = globalCachingProctorStore.getMatrixHistory(0, 10);
        assertThat(actualHistory).isEqualTo(HISTORY);
    }

    @Test
    public void testGetHistoryWhenGlobalCacheHas() throws StoreException {
        when(globalCacheStore.getCachedHistory(ENVIRONMENT, TEST_NAME)).thenReturn(Optional.of(HISTORY));
        final List<Revision> actualHistory = globalCachingProctorStore.getHistory(TEST_NAME, 0, 10);
        assertThat(actualHistory).isEqualTo(HISTORY);
    }

    @Test
    public void testGetHistoryWhenGlobalCacheDoesNotHave() throws StoreException {
        when(globalCacheStore.getCachedHistory(ENVIRONMENT, TEST_NAME)).thenReturn(Optional.empty());
        when(delegate.getHistory(TEST_NAME, 0, 10)).thenReturn(HISTORY);
        final List<Revision> actualHistory = globalCachingProctorStore.getHistory(TEST_NAME, 0, 10);
        assertThat(actualHistory).isEqualTo(HISTORY);
    }

    @Test
    public void testGetAllHistories() throws StoreException {
        when(delegate.getAllHistories()).thenReturn(ALL_HISTORIES);
        final Map<String, List<Revision>> actualAllHistories = globalCachingProctorStore.getAllHistories();
        assertThat(actualAllHistories).isEqualTo(ALL_HISTORIES);
    }

    @Test
    public void testRefresh() throws StoreException {
        doNothing().when(delegate).refresh();
        globalCachingProctorStore.refresh();
        verify(delegate, times(1)).refresh();
    }

    @Test
    public void testClose() throws IOException {
        doNothing().when(delegate).close();
        globalCachingProctorStore.close();
        verify(delegate, times(1)).close();
    }

    private static List<Revision> createHistory() {
        return ImmutableList.of(
                new Revision(REVISION, AUTHOR, new Date(118, 1, 1), "test")
        );
    }

    private static Map<String, List<Revision>> createAllHistories() {
        final List<Revision> history = createHistory();
        return ImmutableMap.of(TEST_NAME, history);
    }

    private static TestDefinition createTestDefinition() {
        return new TestDefinition(
                "-1",
                null,
                TestType.ANONYMOUS_USER,
                "&example_tst",
                ImmutableList.of(new TestBucket("active", 1, "")),
                ImmutableList.of(
                        new Allocation(null, ImmutableList.of(new Range(1, 1.0)), "#A1")
                ),
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                "SAND-1: test"
        );
    }

    private static TestMatrixVersion createTestMatrixVersion() {
        return new TestMatrixVersion(
                new TestMatrixDefinition(
                        ImmutableMap.of(TEST_NAME, createTestDefinition())
                ),
                new Date(118, 1, 1),
                REVISION,
                "test-description",
                AUTHOR
        );
    }
}