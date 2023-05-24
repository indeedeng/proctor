package com.indeed.proctor.consumer.spring;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.StringProctorLoader;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.AbstractGroups;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SampleRandomGroupsHttpHandlerTest {
    private static final String PROCTOR_MATRIX_JSON = "proctor-matrix.json";
    private static final String PROCTOR_SPECIFICATION_JSON = "proctor-specification.json";

    private static final AbstractProctorLoader PROCTOR_LOADER;

    static {
        PROCTOR_LOADER = getProctorLoader();
        PROCTOR_LOADER.load();
    }

    @Test
    public void testGetTestType() {
        final SampleRandomGroupsHttpHandler.ContextSupplier<Object> mockedSupplier =
                mock(SampleRandomGroupsHttpHandler.ContextSupplier.class);
        final SampleRandomGroupsHttpHandler<Object> handler =
                new SampleRandomGroupsHttpHandler<>(PROCTOR_LOADER, mockedSupplier);

        assertThat(handler.getTestType(ImmutableSet.of("account1_tst")))
                .isEqualTo(TestType.ACCOUNT);
        assertThat(handler.getTestType(ImmutableSet.of("account1_tst", "account2_tst")))
                .isEqualTo(TestType.ACCOUNT);
        assertThatThrownBy(() -> handler.getTestType(ImmutableSet.of("account1_tst", "email_tst")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith(
                        "Target test group list contains tests of multiple test types: ");
        assertThatThrownBy(
                        () -> handler.getTestType(ImmutableSet.of("account1_tst", "unknown_tst")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unrecognized test name: 'unknown_tst'");

        verifyNoMoreInteractions(mockedSupplier);
    }

    @Test
    public void testRunSampling() {
        final SampleRandomGroupsHttpHandler.ContextSupplier<Object> mockedSupplier =
                mock(SampleRandomGroupsHttpHandler.ContextSupplier.class);
        final AbstractGroups mockedGroups = mock(AbstractGroups.class);
        final Object context = new Object();

        when(mockedGroups.getAsProctorResult()).thenReturn(ProctorResult.EMPTY);
        when(mockedSupplier.getRandomGroups(eq(context), any())).thenReturn(mockedGroups);

        final SampleRandomGroupsHttpHandler<Object> handler =
                new SampleRandomGroupsHttpHandler<>(PROCTOR_LOADER, mockedSupplier);
        // check if the keys in the return value are correct
        assertThat(
                        handler.runSampling(
                                context, ImmutableSet.of("account1_tst"), TestType.ACCOUNT, 10))
                .containsOnlyKeys("account1_tst-1", "account1_tst0", "account1_tst1");
        // check if methods of mocked instances are correctly called
        verify(mockedGroups, times(10)).getAsProctorResult();
        verify(mockedSupplier, times(10)).getRandomGroups(eq(context), any());
        verifyNoMoreInteractions(mockedSupplier, mockedGroups);
    }

    @Test
    public void testGetTargetTestGroups() {
        final SampleRandomGroupsHttpHandler.ContextSupplier<Object> mockedSupplier =
                mock(SampleRandomGroupsHttpHandler.ContextSupplier.class);
        final SampleRandomGroupsHttpHandler<Object> handler =
                new SampleRandomGroupsHttpHandler<>(PROCTOR_LOADER, mockedSupplier);

        assertThat(handler.getTargetTestGroups(ImmutableSet.of("account1_tst", "email_tst")))
                .containsExactlyInAnyOrder(
                        "account1_tst-1",
                        "account1_tst0",
                        "account1_tst1",
                        "email_tst0",
                        "email_tst1",
                        "email_tst2",
                        "email_tst3",
                        "email_tst4");

        verifyNoMoreInteractions(mockedSupplier);
    }

    private static AbstractProctorLoader getProctorLoader() {
        return new StringProctorLoader(
                getProctorSpecification(), PROCTOR_MATRIX_JSON, getProctorMatrixJsonAsString());
    }

    private static String getProctorMatrixJsonAsString() {
        try (final InputStream inputStream =
                        SampleRandomGroupsHttpHandlerTest.class.getResourceAsStream(
                                PROCTOR_MATRIX_JSON);
                final Reader reader = new InputStreamReader(inputStream)) {
            return CharStreams.toString(reader);
        } catch (final IOException e) {
            throw new RuntimeException("failed to read " + PROCTOR_MATRIX_JSON);
        }
    }

    private static ProctorSpecification getProctorSpecification() {
        try (final InputStream inputStream =
                SampleRandomGroupsHttpHandlerTest.class.getResourceAsStream(
                        PROCTOR_SPECIFICATION_JSON)) {
            return ProctorUtils.readSpecification(inputStream);
        } catch (final IOException e) {
            throw new RuntimeException("failed to read " + PROCTOR_SPECIFICATION_JSON);
        }
    }
}
