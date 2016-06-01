package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping({"/api", "/proctor/api"})
public class TestMatrixApiController extends AbstractController {

    private static final Logger LOGGER = Logger.getLogger(TestMatrixApiController.class);


    @Autowired
    public TestMatrixApiController(final WebappConfiguration configuration,
                                   @Qualifier("trunk") final ProctorStore trunkStore,
                                   @Qualifier("qa") final ProctorStore qaStore,
                                   @Qualifier("production") final ProctorStore productionStore) {
        super(configuration, trunkStore, qaStore, productionStore);
    }

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public View helloWorld() {
        final Map<String, String> result = ImmutableMap.of("hello", "world");
        return new JsonView(result);
    }

    @RequestMapping(value = "/{branchOrRevision}/matrix", method = RequestMethod.GET)
    public JsonView getTestMatrix(
            @PathVariable final String branchOrRevision
    ) throws StoreException {
            final TestMatrixVersion testMatrixVersion = queryMatrixFromBranchOrRevision(branchOrRevision);
            return new JsonView(testMatrixVersion);
    }

    @RequestMapping(value = "/{branch}/matrix/history", method = RequestMethod.GET)
    public JsonView getTestMatrixHistory(
            @PathVariable final String branch,
            @RequestParam(required = false, value = "start", defaultValue = "0") final int start,
            @RequestParam(required = false, value = "limit", defaultValue = "32") final int limit
    ) throws StoreException {
            final List<Revision> matrixHistory = queryMatrixHistory(determineEnvironmentFromParameter(branch), start, limit);
            return new JsonView(matrixHistory);
    }

    @RequestMapping(value = "/{branchOrRevision}/definition/{testName}", method = RequestMethod.GET)
    public JsonView getTestDefinition(
            @PathVariable final String branchOrRevision,
            @PathVariable final String testName
    ) throws StoreException {
            final TestDefinition testDefinition = queryTestDefinition(branchOrRevision, testName);
            return new JsonView(testDefinition);
    }

    @RequestMapping(value = "/{branchOrRevision}/definition/{testName}/history", method = RequestMethod.GET)
    public JsonView getTestDefinitionHistory(
            @PathVariable final String branchOrRevision,
            @PathVariable final String testName,
            @RequestParam(required = false, value = "start", defaultValue = "0") final int start,
            @RequestParam(required = false, value = "limit", defaultValue = "32") final int limit
    ) throws StoreException {
        final List<Revision> revisions = queryTestDefiniionHistory(branchOrRevision, testName, start, limit);
        return new JsonView(revisions);
    }

    @ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {Exception.class})
    public View handleStoreException(final Exception e) {
        LOGGER.error(e);
        return new JsonView(ImmutableMap.of("error", e.getLocalizedMessage()));
    }
}
