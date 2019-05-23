package com.indeed.proctor.webapp.controllers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.TestHistoriesResponseModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

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

    @ApiOperation(
            value = "Show a test matrix for a branch or revision",
            response = TestMatrixVersion.class,
            produces = "application/json"
    )
    @RequestMapping(value = "/{branchOrRevision}/matrix", method = RequestMethod.GET)
    public JsonView getTestMatrix(
            @PathVariable final String branchOrRevision
    ) throws StoreException {
        final TestMatrixVersion testMatrixVersion = queryMatrixFromBranchOrRevision(branchOrRevision);
        Preconditions.checkNotNull(testMatrixVersion, String.format("Branch or revision %s not correct", branchOrRevision));
        return new JsonView(testMatrixVersion);
    }

    @ApiOperation(
            value = "Show a history of all tests starting at a branch or revision",
            response = Revision.class,
            responseContainer = "List",
            produces = "application/json"
    )
    @RequestMapping(value = "/{branch}/matrix/history", method = RequestMethod.GET)
    public JsonView getTestMatrixHistory(
            @ApiParam(allowableValues = "trunk,qa,production") @PathVariable final String branch,
            @RequestParam(required = false, value = "start", defaultValue = "0") final int start,
            @RequestParam(required = false, value = "limit", defaultValue = "32") final int limit
    ) throws StoreException {
        final Environment environment = Environment.fromName(branch);
        Preconditions.checkNotNull(environment, String.format("Branch %s not correct", branch));
        return new JsonView(queryMatrixHistory(environment, start, limit));
    }

    @ApiOperation(
            value = "Show a definition of a test starting at a branch or revision",
            response = TestDefinition.class,
            produces = "application/json"
    )
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

    @ApiOperation(
            value = "Show a history of a test starting at a branch or revision",
            response = Revision.class,
            responseContainer = "List",
            produces = "application/json"
    )
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

    @ApiOperation(
            value = "Show histories of all tests",
            response = TestHistoriesResponseModel.class,
            produces = "application/json"
    )
    @GetMapping("/{branch}/matrix/testHistories")
    public JsonView getTestHistories(
            @ApiParam(allowableValues = "trunk,qa,production") @PathVariable final String branch,
            @RequestParam(required = false, value = "limit", defaultValue = "100") final int limit
    ) throws StoreException, ResourceNotFoundException {
        final Environment environment = Environment.fromName(branch);
        if (environment == null) {
            throw new ResourceNotFoundException("Branch " + branch + " is not a correct branch name. It must be on of (trunk, qa, production).");
        }
        final Map<String, List<Revision>> histories = queryHistories(environment);
        return new JsonView(new TestHistoriesResponseModel(histories, limit));
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {Exception.class})
    public View handleStoreException(final Exception e) {
        LOGGER.warn(e);
        return new JsonView(ImmutableMap.of("error", e.getLocalizedMessage()));
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {ResourceNotFoundException.class})
    public JsonView handleNotFoundException(final ResourceNotFoundException e) {
        LOGGER.warn(e);
        return new JsonView(ImmutableMap.of("error", e.getLocalizedMessage()));
    }

    @VisibleForTesting
    static class ResourceNotFoundException extends Exception {
        ResourceNotFoundException(final String message) {
            super(message);
        }
    }
}
