package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class ProctorPromoterTest {
    @Mock
    private ProctorStore trunk;
    @Mock
    private ProctorStore qa;
    @Mock
    private ProctorStore production;
    @Mock
    private ExecutorService executorService;
    private ProctorPromoter proctorPromoter;

    private static final String TEST_NAME = "tn";
    private static final String TRUNK_REVISION = "tr";
    private static final String QA_REVISION = "qr";
    private static final String EMPTY_QA_REVISION = "";
    private static final String PROD_REVISION = "pr";
    private static final String USERNAME = "u";
    private static final String PASSWORD = "p";
    private static final String AUTHOR = "a";
    private static final Map<String, String> METADATA = Collections.emptyMap();
    private static final String COMMIT_MESSAGE = "test commit message";
    private static final List<Revision> TRUNK_HISTORY = Collections.singletonList(
            new Revision(TRUNK_REVISION, null, null, COMMIT_MESSAGE));
    private static final List<Revision> QA_HISTORY = Collections.singletonList(
            new Revision(QA_REVISION, null, null, COMMIT_MESSAGE));
    private static final List<Revision> PRODUCTION_HISTORY = Collections.singletonList(
            new Revision(PROD_REVISION, null, null, COMMIT_MESSAGE));

    @Before
    public void setup() throws StoreException {
        final TestDefinition mockTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(trunk.getTestDefinition(TEST_NAME, TRUNK_REVISION)).thenReturn(mockTestDefinition);
        proctorPromoter = Mockito.spy(new ProctorPromoter(trunk, qa, production, executorService));
    }

    @Test
    public void promoteTrunkToQA() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.doNothing().when(proctorPromoter).promote(
                Mockito.eq(TEST_NAME),
                Mockito.any(Environment.class),
                Mockito.anyString(),
                Mockito.any(Environment.class),
                Mockito.anyString(),
                Mockito.eq(USERNAME),
                Mockito.eq(PASSWORD),
                Mockito.eq(AUTHOR),
                Mockito.eq(METADATA)
        );

        proctorPromoter.promoteTrunkToQa(TEST_NAME, TRUNK_REVISION, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA);
        Mockito.verify(proctorPromoter, Mockito.times(1)).promote(
                TEST_NAME,
                Environment.WORKING,
                TRUNK_REVISION,
                Environment.QA,
                QA_REVISION,
                USERNAME,
                PASSWORD,
                AUTHOR,
                METADATA
        );
    }

    @Test
    public void promoteQAToProduction() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.doNothing().when(proctorPromoter).promote(
                Mockito.eq(TEST_NAME),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.eq(USERNAME),
                Mockito.eq(PASSWORD),
                Mockito.eq(AUTHOR),
                Mockito.eq(METADATA)
        );

        proctorPromoter.promoteQaToProduction(TEST_NAME, QA_REVISION, PROD_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA);
        Mockito.verify(proctorPromoter, Mockito.times(1)).promote(
                TEST_NAME,
                Environment.QA,
                QA_REVISION,
                Environment.PRODUCTION,
                PROD_REVISION,
                USERNAME,
                PASSWORD,
                AUTHOR,
                METADATA
        );
    }

    @Test
    public void promoteTrunkToProduction() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.doNothing().when(proctorPromoter).promote(
                Mockito.eq(TEST_NAME),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.eq(USERNAME),
                Mockito.eq(PASSWORD),
                Mockito.eq(AUTHOR),
                Mockito.eq(METADATA)
        );

        proctorPromoter.promoteTrunkToProduction(TEST_NAME, TRUNK_REVISION, PROD_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA);
        Mockito.verify(proctorPromoter, Mockito.times(1)).promote(
                TEST_NAME,
                Environment.WORKING,
                TRUNK_REVISION,
                Environment.PRODUCTION,
                PROD_REVISION,
                USERNAME,
                PASSWORD,
                AUTHOR,
                METADATA
        );
    }

    @Test
    public void promoteWhenRevDoesNotExistInDestFails() throws StoreException, ProctorPromoter.TestPromotionException {
        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(EMPTY_QA_REVISION); // can never happen?
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);

        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA))
            .isInstanceOf(ProctorPromoter.TestPromotionException.class)
            .hasMessage("Positive revision r" + QA_REVISION
                + " given for destination ( " + Environment.QA.getName()
                + " ) but '" + TEST_NAME + "' does not exist.");
    }

    @Test
    public void promoteWhenRevIsSpecifiedAsEmptyButExistsInDestFails() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);
        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);
        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, EMPTY_QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA))
                .isInstanceOf(ProctorPromoter.TestPromotionException.class)
                .hasMessage("Non-Positive revision r" + EMPTY_QA_REVISION
                        + " given for destination ( " + Environment.QA.getName()
                        + " ) but '" + TEST_NAME + "' exists.");

    }

    @Test
    public void promoteWhenSrcHistoryIsEmptyFails() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);

        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);

        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA))
                .isInstanceOf(ProctorPromoter.TestPromotionException.class)
                .hasMessage("Could not find history for " + TEST_NAME
                        + " at revision " + TRUNK_REVISION);
    }

    @Test
    public void promoteWhenRevIsNewAddsTestDefinition() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(trunk.getHistory(TEST_NAME, TRUNK_REVISION, 0, 1)).thenReturn(TRUNK_HISTORY);

        proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA);
        Mockito.verify(qa).addTestDefinition(Mockito.eq(USERNAME), Mockito.eq(PASSWORD), Mockito.eq(AUTHOR), Mockito.eq(TEST_NAME),
                Mockito.any(TestDefinition.class), Mockito.eq(METADATA),
                Mockito.eq("Promoting tn (trunk rtr) to qa\n" +
                        "\n" + COMMIT_MESSAGE));
    }

    @Test
    public void promoteWhenHistoryDoesNotExistFails() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(trunk.getHistory(TEST_NAME, TRUNK_REVISION, 0, 1)).thenReturn(TRUNK_HISTORY);
        // not sure how this testcase makes sense after refactoring.
        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY, emptyList());

        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);

        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA))
                .isInstanceOf(ProctorPromoter.TestPromotionException.class)
                .hasMessage("No history found for '" + TEST_NAME
                        + "' in destination ( " + Environment.QA.getName() + " ).");
    }

    @Test
    public void promoteWhenHistoryDoesNotHaveLatestDestRev() throws StoreException, ProctorPromoter.TestPromotionException {
        final String updatedQaRevision = "fqr";
        Mockito.when(trunk.getHistory(TEST_NAME, TRUNK_REVISION, 0, 1)).thenReturn(TRUNK_HISTORY);

        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);

        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);

        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, updatedQaRevision, USERNAME, PASSWORD, AUTHOR, METADATA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Test '" + TEST_NAME + "' updated since " + updatedQaRevision + ". Currently at " + QA_REVISION);
    }

    @Test
    public void promoteAgainWhenTestInDestinationHasBeenDeleted() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(trunk.getHistory(TEST_NAME, TRUNK_REVISION, 0, 1)).thenReturn(TRUNK_HISTORY);

        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);
        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition, null); // not sure how this makes sense


        assertThatThrownBy(() -> proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Test '" + TEST_NAME + "' has been deleted in destination, not allowed to promote again.");
    }

    @Test
    public void promoteWhenRevExistsUpdatesTestDefinition() throws StoreException, ProctorPromoter.TestPromotionException {
        Mockito.when(trunk.getHistory(TEST_NAME, TRUNK_REVISION, 0, 1)).thenReturn(TRUNK_HISTORY);
        Mockito.when(qa.getHistory(TEST_NAME, 0, 1)).thenReturn(QA_HISTORY);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(Mockito.mock(TestDefinition.class));

        final TestDefinition mockedPromotedTestDefinition = Mockito.mock(TestDefinition.class);
        Mockito.when(mockedPromotedTestDefinition.getVersion()).thenReturn(QA_REVISION);
        Mockito.when(qa.getCurrentTestDefinition(TEST_NAME)).thenReturn(mockedPromotedTestDefinition);

        proctorPromoter.promote(TEST_NAME, Environment.WORKING, TRUNK_REVISION, Environment.QA, QA_REVISION, USERNAME, PASSWORD, AUTHOR, METADATA);
        Mockito.verify(qa).updateTestDefinition(
                Mockito.eq(USERNAME),
                Mockito.eq(PASSWORD),
                Mockito.eq(AUTHOR), Mockito.eq(QA_REVISION), Mockito.eq(TEST_NAME),
                Mockito.any(TestDefinition.class), Mockito.eq(METADATA),
                Mockito.eq("Promoting tn (trunk rtr) to qa\n" +
                        "\n" + COMMIT_MESSAGE));
    }

    @Test
    public void getEnvironmentVersion() throws StoreException, InterruptedException, ExecutionException, TimeoutException {
        final Future<SingleEnvironmentVersion> mockedFuture = Mockito.mock(Future.class);
        Mockito.when(mockedFuture.get(Mockito.anyLong(), Mockito.eq(TimeUnit.SECONDS)))
                .thenReturn(new SingleEnvironmentVersion(TRUNK_HISTORY.get(0), TRUNK_REVISION))
                .thenReturn(new SingleEnvironmentVersion(QA_HISTORY.get(0), QA_REVISION))
                .thenReturn(new SingleEnvironmentVersion(PRODUCTION_HISTORY.get(0), PROD_REVISION));
        Mockito.when(executorService.submit(Mockito.any(Callable.class)))
                .thenReturn(mockedFuture);

        final EnvironmentVersion environmentVersion = proctorPromoter.getEnvironmentVersion(TEST_NAME);
        Assertions.assertThat(environmentVersion.getTestName()).isEqualTo(TEST_NAME);
        Assertions.assertThat(environmentVersion.getTrunk()).isEqualTo(TRUNK_HISTORY.get(0));
        Assertions.assertThat(environmentVersion.getTrunkVersion()).isEqualTo(TRUNK_REVISION);
        Assertions.assertThat(environmentVersion.getQa()).isEqualTo(QA_HISTORY.get(0));
        Assertions.assertThat(environmentVersion.getQaVersion()).isEqualTo(QA_REVISION);
        Assertions.assertThat(environmentVersion.getProduction()).isEqualTo(PRODUCTION_HISTORY.get(0));
        Assertions.assertThat(environmentVersion.getProductionVersion()).isEqualTo(PROD_REVISION);
    }
}
