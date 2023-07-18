package com.indeed.proctor.webapp.util;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

public class TestDefinitionUtilTest {

    private static final String TEST_NAME = "tn";
    private static final String TRUNK_REVISION = "tr";
    private static final String QA_REVISION = "qr";
    private static final String COMMIT_MESSAGE = "test commit message";
    private static final List<Revision> TRUNK_HISTORY =
            Collections.singletonList(new Revision(TRUNK_REVISION, null, null, COMMIT_MESSAGE));
    private static final List<Revision> QA_HISTORY =
            Collections.singletonList(new Revision(QA_REVISION, null, null, COMMIT_MESSAGE));

    @Test
    public void testGetResolvedLastVersion() throws StoreException {
        ProctorStore trunk = mock(ProctorStore.class);
        ProctorStore qa = mock(ProctorStore.class);
        Mockito.when(trunk.getHistory(TEST_NAME, 0, 1)).thenReturn(TRUNK_HISTORY);
        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);

        final TestDefinition mockedPromotedTestDefinition = mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(TRUNK_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME))
                .thenReturn(mockedPromotedTestDefinition);
        Assertions.assertThat(
                        TestDefinitionUtil.getResolvedLastVersion(
                                        trunk, TEST_NAME, Environment.WORKING)
                                .getRevision())
                .isEqualTo(TRUNK_HISTORY.get(0));
        Assertions.assertThat(
                        TestDefinitionUtil.getResolvedLastVersion(
                                        trunk, TEST_NAME, Environment.WORKING)
                                .getVersion())
                .isEqualTo(TRUNK_REVISION);
        Assertions.assertThat(
                        TestDefinitionUtil.getResolvedLastVersion(qa, TEST_NAME, Environment.QA)
                                .getRevision())
                .isEqualTo(QA_HISTORY.get(0));
        Assertions.assertThat(
                        TestDefinitionUtil.getResolvedLastVersion(qa, TEST_NAME, Environment.QA)
                                .getVersion())
                .isEqualTo(TRUNK_REVISION);
    }
}
