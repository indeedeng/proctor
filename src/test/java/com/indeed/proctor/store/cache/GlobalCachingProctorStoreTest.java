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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
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

    @Before
    public void setUp() {
        globalCachingProctorStore = new GlobalCachingProctorStore(delegate, globalCacheStore, Environment.PRODUCTION);
    }

    @Test
    public void tesGetName() {
        when(delegate.getName()).thenReturn("TestProctorStore");
        assertThat(globalCachingProctorStore.getName()).isEqualTo("TestProctorStore");
    }

    @Test
    public void testGetCurrentTestMatrix() throws StoreException {
        final TestMatrixVersion testMatrixVersion = createTestMatrixVersion();
        when(delegate.getCurrentTestMatrix()).thenReturn(testMatrixVersion);
        final TestMatrixVersion actualTestMatrixVersion = globalCachingProctorStore.getCurrentTestMatrix();
        assertThat(actualTestMatrixVersion).isEqualToComparingFieldByFieldRecursively(testMatrixVersion);
    }

    @Test
    public void testGetCurrentTestDefinitionWhenGlobalCacheHas() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        when(globalCacheStore.getCachedTestDefinition(any(), anyString())).thenReturn(Optional.of(testDefinition));
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getCurrentTestDefinition("example_tst");
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(testDefinition);
    }

    @Test
    public void testGetCurrentTestDefinitionWhenGlobalCacheDoesNotHave() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        when(globalCacheStore.getCachedTestDefinition(any(), anyString())).thenReturn(Optional.empty());
        when(delegate.getCurrentTestDefinition(anyString())).thenReturn(testDefinition);
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getCurrentTestDefinition("example_tst");
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(testDefinition);
    }

    @Test
    public void testVerifySetup() throws StoreException {
        doNothing().when(delegate).verifySetup();
        globalCachingProctorStore.verifySetup();
        verify(delegate, times(1)).verifySetup();
    }

    @Test
    public void testCleanUserWorkspace() {
        when(delegate.cleanUserWorkspace(anyString())).thenReturn(true);
        final boolean isSuccess = globalCachingProctorStore.cleanUserWorkspace("test-user");
        verify(delegate, times(1)).cleanUserWorkspace("test-user");
        assertThat(isSuccess).isTrue();
    }

    @Test
    public void testUpdateTestDefinition() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        final List<Revision> history = createHistory();
        doNothing().when(delegate).updateTestDefinition(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyMap(), anyString());
        doNothing().when(globalCacheStore).updateCache(any(), anyString(), any(), anyList());
        when(delegate.getHistory(anyString(), anyInt(), anyInt())).thenReturn(history);
        globalCachingProctorStore.updateTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "abc",
                "example_tst",
                testDefinition,
                null,
                "test-comment"
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).updateTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "abc",
                "example_tst",
                testDefinition,
                null,
                "test-comment"
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                Environment.PRODUCTION,
                "example_tst",
                testDefinition,
                history
        );
    }

    @Test
    public void testDeleteTestDefinition() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        final List<Revision> history = createHistory();
        doNothing().when(delegate).deleteTestDefinition(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString());
        doNothing().when(globalCacheStore).updateCache(any(), anyString(), any(), anyList());
        when(delegate.getHistory(anyString(), anyInt(), anyInt())).thenReturn(history);
        globalCachingProctorStore.deleteTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "abc",
                "example_tst",
                testDefinition,
                "test-comment"
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).deleteTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "abc",
                "example_tst",
                testDefinition,
                "test-comment"
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                Environment.PRODUCTION,
                "example_tst",
                null,
                history
        );
    }

    @Test
    public void testAddTestDefinition() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        final List<Revision> history = createHistory();
        doNothing().when(delegate).addTestDefinition(
                anyString(), anyString(), anyString(), anyString(), any(), anyMap(), anyString());
        doNothing().when(globalCacheStore).updateCache(any(), anyString(), any(), anyList());
        when(delegate.getHistory(anyString(), anyInt(), anyInt())).thenReturn(history);
        globalCachingProctorStore.addTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "example_tst",
                testDefinition,
                null,
                "test-comment"
        );
        InOrder inOrder = Mockito.inOrder(delegate, globalCacheStore);
        inOrder.verify(delegate, times(1)).addTestDefinition(
                "test-user",
                "test-password",
                "test-author",
                "example_tst",
                testDefinition,
                null,
                "test-comment"
        );
        inOrder.verify(globalCacheStore, times(1)).updateCache(
                Environment.PRODUCTION,
                "example_tst",
                testDefinition,
                history
        );
    }

    @Test
    public void testGetLatestVersion() throws StoreException {
        when(delegate.getLatestVersion()).thenReturn("abc");
        final String version = globalCachingProctorStore.getLatestVersion();
        assertThat(version).isEqualTo("abc");
    }

    @Test
    public void testGetTestMatrix() throws StoreException {
        final TestMatrixVersion testMatrixVersion = createTestMatrixVersion();
        when(delegate.getTestMatrix(anyString())).thenReturn(testMatrixVersion);
        final TestMatrixVersion actualTestMatrixVersion = globalCachingProctorStore.getTestMatrix("abc");
        assertThat(actualTestMatrixVersion).isEqualToComparingFieldByFieldRecursively(testMatrixVersion);
    }

    @Test
    public void testGetTestDefinition() throws StoreException {
        final TestDefinition testDefinition = createTestDefinition();
        when(delegate.getTestDefinition(anyString(), anyString())).thenReturn(testDefinition);
        final TestDefinition actualTestDefinition = globalCachingProctorStore.getTestDefinition("example_tst", "abc");
        assertThat(actualTestDefinition).isEqualToComparingFieldByFieldRecursively(actualTestDefinition);
    }

    @Test
    public void testGetMatrixHistory() throws StoreException {
        final List<Revision> history = createHistory();
        when(delegate.getMatrixHistory(anyInt(), anyInt())).thenReturn(history);
        final List<Revision> actualHistory = globalCachingProctorStore.getMatrixHistory(0, 10);
        assertThat(actualHistory).isEqualTo(history);
    }

    @Test
    public void testGetHistoryWhenGlobalCacheHas() throws StoreException {
        final List<Revision> history = createHistory();
        when(globalCacheStore.getCachedHistory(any(), anyString())).thenReturn(Optional.of(history));
        final List<Revision> actualHistory = globalCachingProctorStore.getHistory("example_tst", 0, 10);
        assertThat(actualHistory).isEqualTo(history);
    }

    @Test
    public void testGetHistoryWhenGlobalCacheDoesNotHave() throws StoreException {
        final List<Revision> history = createHistory();
        when(globalCacheStore.getCachedHistory(any(), anyString())).thenReturn(Optional.empty());
        when(delegate.getHistory(anyString(), anyInt(), anyInt())).thenReturn(history);
        final List<Revision> actualHistory = globalCachingProctorStore.getHistory("example_tst", 0, 10);
        assertThat(actualHistory).isEqualTo(history);
    }

    @Test
    public void testGetAllHistories() throws StoreException {
        final Map<String, List<Revision>> allHistories = createAllHistories();
        when(delegate.getAllHistories()).thenReturn(allHistories);
        final Map<String, List<Revision>> actualAllHistories = globalCachingProctorStore.getAllHistories();
        assertThat(actualAllHistories).isEqualTo(allHistories);
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

    private List<Revision> createHistory() {
        return ImmutableList.of(
                new Revision("test-revision", "test-author", new Date(2018, 1, 1), "test")
        );
    }

    private Map<String, List<Revision>> createAllHistories() {
        final List<Revision> history = createHistory();
        return ImmutableMap.of("test", history);
    }

    private TestDefinition createTestDefinition() {
        return new TestDefinition(
                "-1",
                null,
                TestType.ANONYMOUS_USER,
                "&test",
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

    private TestMatrixVersion createTestMatrixVersion() {
        return new TestMatrixVersion(
                new TestMatrixDefinition(
                        ImmutableMap.of("test", createTestDefinition())
                ),
                new Date(2018, 1, 1),
                "abc",
                "test-description",
                "test-author"
        );
    }
}