package com.indeed.proctor.store.async;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(AsyncProctorStore.class);
    private volatile ProctorStore proctorStore = null;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public AsyncProctorStore(final StoreFactory factory, final String relativePath) {
        executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws ConfigurationException {
                proctorStore = factory.createStore(relativePath);
                return null;
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (proctorStore != null) {
            proctorStore.close();
        }
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getCurrentTestMatrix();
        }
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String test) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getCurrentTestDefinition(test);
        }
    }

    @Override
    public void verifySetup() throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.verifySetup();
        }
    }

    @Override
    public String getLatestVersion() throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getLatestVersion();
        }
    }

    @Override
    public TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getTestMatrix(fetchRevision);
        }
    }

    @Override
    public TestDefinition getTestDefinition(final String test, final String fetchRevision) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getTestDefinition(test, fetchRevision);
        }
    }

    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getMatrixHistory(start, limit);
        }
    }

    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getHistory(test, start, limit);
        }
    }

    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getHistory(test, revision, start, limit);
        }
    }

    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getAllHistories();
        }
    }

    @Override
    public void refresh() throws StoreException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.refresh();
        }
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        if (proctorStore != null) {
            return proctorStore.cleanUserWorkspace(username);
        }

        return false;
    }

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.updateTestDefinition(username, password, previousVersion, testName, testDefinition, metadata, comment);
        }
    }

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.updateTestDefinition(username, password, author, previousVersion, testName, testDefinition, metadata, comment);
        }
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.deleteTestDefinition(username, password, previousVersion, testName, testDefinition, comment);
        }
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.deleteTestDefinition(username, password, author, previousVersion, testName, testDefinition, comment);
        }
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.addTestDefinition(username, password, testName, testDefinition, metadata, comment);
        }
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String author,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            proctorStore.addTestDefinition(username, password, author, testName, testDefinition, metadata, comment);
        }
    }

    @Override
    public String getName() {
        if (proctorStore == null) {
            throw new RuntimeException("Not initialized.");
        } else {
            return proctorStore.getName();
        }
    }
}

