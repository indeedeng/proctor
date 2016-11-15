package com.indeed.proctor.webapp.controllers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.View;

import java.util.List;

@Controller
@RequestMapping({"/api/v1", "/proctor/api/v1"})
public class TestMatrixApiController extends AbstractController {

    private static final Logger LOGGER = Logger.getLogger(TestMatrixApiController.class);

    @Autowired
    public TestMatrixApiController(final WebappConfiguration configuration,
                                   @Qualifier("trunk") final ProctorStore trunkStore,
                                   @Qualifier("qa") final ProctorStore qaStore,
                                   @Qualifier("production") final ProctorStore productionStore) {
        super(configuration, trunkStore, qaStore, productionStore);
    }

    @RequestMapping(value = "/{branchOrRevision}/matrix", method = RequestMethod.GET)
    public JsonView getTestMatrix(
            @PathVariable final String branchOrRevision
    ) throws StoreException {
        final TestMatrixVersion testMatrixVersion = queryMatrixFromBranchOrRevision(branchOrRevision);
        Preconditions.checkNotNull(testMatrixVersion, String.format("Branch or revision %s not correct", branchOrRevision));
        return new JsonView(testMatrixVersion);
    }

    @RequestMapping(value = "/{branch}/matrix/history", method = RequestMethod.GET)
    public JsonView getTestMatrixHistory(
            @PathVariable final String branch,
            @RequestParam(required = false, value = "start", defaultValue = "0") final int start,
            @RequestParam(required = false, value = "limit", defaultValue = "32") final int limit
    ) throws StoreException {
        final Environment environment = Environment.fromName(branch);
        Preconditions.checkNotNull(environment, String.format("Branch %s not correct", branch));
        return new JsonView(queryMatrixHistory(environment, start, limit));
    }

    @RequestMapping(value = "/{branchOrRevision}/definition/{testName}", method = RequestMethod.GET)
    public JsonView getTestDefinition(
            @PathVariable final String branchOrRevision,
            @PathVariable final String testName
    ) throws StoreException {
        final TestDefinition testDefinition = queryTestDefinition(branchOrRevision, testName);
        Preconditions.checkNotNull(testDefinition,
                String.format("Branch or revision %s not correct, or test %s not found", branchOrRevision, testName));
        return new JsonView(testDefinition);
    }

    @RequestMapping(value = "/{branchOrRevision}/definition/{testName}/history", method = RequestMethod.GET)
    public JsonView getTestDefinitionHistory(
            @PathVariable final String branchOrRevision,
            @PathVariable final String testName,
            @RequestParam(required = false, value = "start", defaultValue = "0") final int start,
            @RequestParam(required = false, value = "limit", defaultValue = "32") final int limit
    ) throws StoreException {
        final List<Revision> revisions = queryTestDefinitionHistory(branchOrRevision, testName, start, limit);
        Preconditions.checkState(!revisions.isEmpty(), String.format("Branch or revision %s not correct, or test %s not found", branchOrRevision, testName));
        return new JsonView(revisions);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {Exception.class})
    public View handleStoreException(final Exception e) {
        LOGGER.error(e);
        return new JsonView(ImmutableMap.of("error", e.getLocalizedMessage()));
    }
}
