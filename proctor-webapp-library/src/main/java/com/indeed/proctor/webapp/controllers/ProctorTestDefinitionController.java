package com.indeed.proctor.webapp.controllers;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.ProctorSpecificationSource;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.jobs.BackgroundJob;
import com.indeed.proctor.webapp.jobs.DeleteJob;
import com.indeed.proctor.webapp.jobs.EditAndPromoteJob;
import com.indeed.proctor.webapp.jobs.MatrixChecker;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.BackgroundJobResponseModel;
import com.indeed.proctor.webapp.model.ProctorClientApplication;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import com.indeed.proctor.webapp.model.SessionViewModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.tags.UtilityFunctions;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import com.indeed.proctor.webapp.views.JsonView;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author parker
 */
@Controller
@RequestMapping({"/definition", "/proctor/definition"})
public class ProctorTestDefinitionController extends AbstractController {
    private static final Logger LOGGER = Logger.getLogger(ProctorTestDefinitionController.class);

    private final ProctorPromoter promoter;
    private final ProctorSpecificationSource specificationSource;
    private final int verificationTimeout;
    private final MatrixChecker matrixChecker;
    private final DeleteJob deleteJob;
    private final EditAndPromoteJob editAndPromoteJob;
    private final boolean requireAuth;

    private static enum Views {
        DETAILS("definition/details"),
        EDIT("definition/edit"),
        CREATE("definition/edit");

        private final String name;

        private Views(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Autowired
    public ProctorTestDefinitionController(final WebappConfiguration configuration,
                                           @Qualifier("trunk") final ProctorStore trunkStore,
                                           @Qualifier("qa") final ProctorStore qaStore,
                                           @Qualifier("production") final ProctorStore productionStore,
                                           final ProctorPromoter promoter,
                                           final ProctorSpecificationSource specificationSource,
                                           final MatrixChecker matrixChecker,
                                           final DeleteJob deleteJob,
                                           final EditAndPromoteJob editAndPromoteJob,
                                           @Value("${requireAuth:true}") final boolean requireAuth
    ) {
        super(configuration, trunkStore, qaStore, productionStore);
        this.promoter = promoter;
        this.matrixChecker = matrixChecker;
        this.deleteJob = deleteJob;
        this.editAndPromoteJob = editAndPromoteJob;
        this.verificationTimeout = configuration.getVerifyHttpTimeout();
        this.specificationSource = specificationSource;
        this.requireAuth = requireAuth;
        Preconditions.checkArgument(verificationTimeout > 0, "verificationTimeout > 0");
    }


    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(final Model model) {

        final TestDefinition definition = new TestDefinition(
                "" /* version */,
                null /* rule */,
                TestType.USER /* testType */,
                "" /* salt */,
                Collections.<TestBucket>emptyList(),
                Lists.<Allocation>newArrayList(
                        new Allocation(null, Collections.<Range>emptyList())
                ),
                Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap(),
                "" /* description */
        );
        final List<RevisionDefinition> history = Collections.emptyList();
        final EnvironmentVersion version = null;
        return doView(Environment.WORKING, Views.CREATE, "", definition, history, version, requireAuth, model);
    }

    @RequestMapping(value = "/{testName}", method = RequestMethod.GET)
    public String show(
            HttpServletResponse response,
            @PathVariable final String testName,
            @RequestParam(required = false) final String branch,
            @RequestParam(required = false, defaultValue = "", value = "r") final String revision,
            @RequestParam(required = false, value = "alloc_hist") final String loadAllocHistParam,
            @CookieValue(value = "loadAllocationHistory", defaultValue = "") String loadAllocHistCookie,
            final Model model
    ) throws StoreException {
        final Environment theEnvironment = determineEnvironmentFromParameter(branch);
        final ProctorStore store = determineStoreFromEnvironment(theEnvironment);

        // Git performance suffers when there are many concurrent operations
        // Only request full test history for one test at a time
        final EnvironmentVersion version;
        final List<RevisionDefinition> history;
        final TestDefinition definition;
        synchronized (this) {
            version = promoter.getEnvironmentVersion(testName);

            if (revision.length() > 0) {
                definition = TestDefinitionUtil.getTestDefinition(store, testName, revision);
            } else {
                definition = TestDefinitionUtil.getTestDefinition(store, testName);
            }

            if (definition == null) {
                LOGGER.info("Unknown test definition : " + testName + " revision " + revision);
                // unknown testdefinition
                if (testNotExistsInAnyEnvs(theEnvironment, testName, revision)) {
                    return doErrorView("Test " + testName + " does not exist in any environment", null, HttpServletResponse.SC_NOT_FOUND, response, model);
                }
                final String errorMsg = "Test \"" + testName + "\" " +
                        (revision.isEmpty() ? "" : "of revision " + revision + " ") +
                        "does not exist in " + branch + " branch! Please check other branches.";
                return doView(theEnvironment, Views.DETAILS, errorMsg, new TestDefinition(), new ArrayList<>(), version, requireAuth, model);
            }
            final boolean loadAllocHistory = shouldLoadAllocationHistory(loadAllocHistParam, loadAllocHistCookie, response);
            history = TestDefinitionUtil.makeRevisionDefinitionList(store, testName, version.getRevision(theEnvironment), loadAllocHistory);
        }

        return doView(theEnvironment, Views.DETAILS, testName, definition, history, version, requireAuth, model);
    }

    private boolean testNotExistsInAnyEnvs(final Environment theEnvironment, final String testName, final String revision) {
        return Stream.of(Environment.values())
                .filter(env -> !theEnvironment.equals(env))
                .allMatch(env -> getTestDefinition(env, testName, revision) == null);
    }

    private boolean shouldLoadAllocationHistory(String loadAllocHistParam, String loadAllocHistCookie, HttpServletResponse response) {
        if (loadAllocHistParam != null) {
            if (loadAllocHistParam.equals("true") || loadAllocHistParam.equals("1")) {
                final Cookie lahCookie = new Cookie("loadAllocationHistory", "true");
                final int thirtyMinutes = 60 * 30;
                lahCookie.setMaxAge(thirtyMinutes);
                lahCookie.setPath("/");
                response.addCookie(lahCookie);
                return true;
            } else {
                final Cookie deletionCookie = new Cookie("loadAllocationHistory", "");
                deletionCookie.setMaxAge(0);
                deletionCookie.setPath("/");
                response.addCookie(deletionCookie);
                return false;
            }
        } else if (loadAllocHistCookie.equals("true") || loadAllocHistCookie.equals("false")) {
            return Boolean.parseBoolean(loadAllocHistCookie);
        } else {
            return false;
        }
    }

    @RequestMapping(value = "/{testName}/edit", method = RequestMethod.GET)
    public String doEditGet(
            @PathVariable String testName,
            final Model model,
            final HttpServletResponse response
    ) throws StoreException {
        final Environment theEnvironment = Environment.WORKING; // only allow editing of TRUNK!
        final ProctorStore store = determineStoreFromEnvironment(theEnvironment);
        final EnvironmentVersion version = promoter.getEnvironmentVersion(testName);

        final TestDefinition definition = TestDefinitionUtil.getTestDefinition(store, testName);
        if (definition == null) {
            LOGGER.info("Unknown test definition : " + testName);
            // unknown testdefinition
            return doErrorView("Test " + testName + " does not exist in TRUNK", null, HttpServletResponse.SC_NOT_FOUND, response, model);
        }

        return doView(theEnvironment, Views.EDIT, testName, definition, Collections.<RevisionDefinition>emptyList(), version, requireAuth, model);
    }

    @ApiOperation(value = "Delete proctor test")
    @RequestMapping(value = "/{testName}/delete", method = RequestMethod.POST, params = {"username, password"})
    public View doDeletePost(
            @PathVariable final String testName,
            @RequestParam final String username,
            @RequestParam final String password,
            @RequestParam(required = false) String src,
            @RequestParam(required = false) final String srcRevision,
            @RequestParam(required = false, defaultValue = "") final String comment,
            final HttpServletRequest request
    ) {
        final Environment theEnvironment = determineEnvironmentFromParameter(src);
        final BackgroundJob job = deleteJob.doDelete(
                testName,
                username,
                password,
                username,
                theEnvironment,
                srcRevision,
                comment,
                new HashMap<>(request.getParameterMap())
        );
        if (isAJAXRequest(request)) {
            final JsonResponse<BackgroundJobResponseModel> response = new JsonResponse<>(new BackgroundJobResponseModel(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }

    }

    @ApiOperation(value = "Promote proctor test")
    @RequestMapping(value = "/{testName}/promote", method = RequestMethod.POST, params = {"username, password"})
    public View doPromotePost(
            @PathVariable final String testName,
            @RequestParam final String username,
            @RequestParam final String password,
            @RequestParam(required = false) final String src,
            @RequestParam(required = false) final String srcRevision,
            @RequestParam(required = false) final String dest,
            @RequestParam(required = false) final String destRevision,
            final HttpServletRequest request
    ) {
        final Environment source = determineEnvironmentFromParameter(src);
        final Environment destination = determineEnvironmentFromParameter(dest);
        final BackgroundJob job = editAndPromoteJob.doPromote(
                testName,
                username,
                password,
                username,
                source,
                srcRevision,
                destination,
                destRevision,
                new HashMap<>(request.getParameterMap())
        );
        if (isAJAXRequest(request)) {
            final JsonResponse<BackgroundJobResponseModel> response = new JsonResponse<>(new BackgroundJobResponseModel(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            return new RedirectView("/proctor/definition/" + UtilityFunctions.urlEncode(testName) + "?branch=" + destination.getName());
        }
    }

    @ApiOperation(value = "Edit proctor test")
    @RequestMapping(value = "/{testName}/edit", method = RequestMethod.POST, params = {"username, password"})
    public View doEditPost(
            @PathVariable final String testName,
            @RequestParam final String username,
            @RequestParam final String password,
            @RequestParam(required = false, defaultValue = "false") final boolean isCreate,
            @RequestParam(required = false, defaultValue = "") final String comment,
            @RequestParam(required = false) final String testDefinition, // testDefinition is JSON representation of test-definition
            @RequestParam(required = false, defaultValue = "") final String previousRevision,
            @RequestParam(required = false, defaultValue = "trunk") final String autopromoteTarget,
            final HttpServletRequest request
    ) {
        final Environment autopromoteTargetEnv = Optional.ofNullable(Environment.fromName(autopromoteTarget)).orElse(Environment.WORKING);

        final BackgroundJob job = editAndPromoteJob.doEdit(
                testName,
                username,
                password,
                username,
                isCreate,
                comment,
                testDefinition,
                previousRevision,
                autopromoteTargetEnv,
                new HashMap<>(request.getParameterMap())
        );
        if (isAJAXRequest(request)) {
            final JsonResponse<BackgroundJobResponseModel> response = new JsonResponse<>(new BackgroundJobResponseModel(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }
    }

    @RequestMapping(value = "/{testName}/verify", method = RequestMethod.GET)
    @ResponseBody
    public String doVerifyGet
            (
                    @PathVariable String testName,
                    @RequestParam(required = false) String src,
                    @RequestParam(required = false) String srcRevision,
                    @RequestParam(required = false) String dest,
                    final HttpServletRequest request,
                    final Model model
            ) {
        final Environment srcBranch = determineEnvironmentFromParameter(src);
        final Environment destBranch = determineEnvironmentFromParameter(dest);

        if (srcBranch == destBranch) {
            return "source == destination";
        }

        final TestDefinition d = getTestDefinition(srcBranch, testName, srcRevision);
        if (d == null) {
            return "could not find " + testName + " on " + srcBranch + " with revision " + srcRevision;
        }

        final MatrixChecker.CheckMatrixResult result = matrixChecker.checkMatrix(destBranch, testName, d);
        if (result.isValid()) {
            return "check success";
        } else {
            return "failed: " + Joiner.on("\n").join(result.getErrors());
        }
    }

    @ApiOperation(value = "Proctor test specification", response = TestSpecification.class)
    @RequestMapping(value = "/{testName}/specification", method = RequestMethod.GET)
    public View doSpecificationGet(
            @PathVariable String testName,
            @RequestParam(required = false) final String branch
    ) {
        final Environment theEnvironment = determineEnvironmentFromParameter(branch);
        final ProctorStore store = determineStoreFromEnvironment(theEnvironment);

        final TestDefinition definition = TestDefinitionUtil.getTestDefinition(store, testName);
        if (definition == null) {
            LOGGER.info("Unknown test definition : " + testName);
            // unknown testdefinition
            throw new NullPointerException("Unknown test definition");
        }

        JsonView view;
        try {
            final TestSpecification specification = ProctorUtils.generateSpecification(definition);
            view = new JsonView(specification);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not generate Test Specification", e);
            view = new JsonView(new JsonResponse(e.getMessage(), false, "Could not generate Test Specification"));
        }
        return view;
    }

    private String doView(final Environment b,
                          final Views view,
                          final String testName,
                          // TODO (parker) 7/27/12 - add Revisioned (that has Revision + testName)
                          final TestDefinition definition,
                          final List<RevisionDefinition> history,
                          final EnvironmentVersion version,
                          final boolean requireAuth,
                          Model model) {
        model.addAttribute("testName", testName);
        model.addAttribute("testDefinition", definition);
        model.addAttribute("isCreate", view == Views.CREATE);
        model.addAttribute("branch", b);
        model.addAttribute("version", version);
        model.addAttribute("requireAuth", requireAuth);

        final Map<String, Object> specialConstants;
        if (definition.getSpecialConstants() != null) {
            specialConstants = definition.getSpecialConstants();
        } else {
            specialConstants = Collections.<String, Object>emptyMap();
        }
        model.addAttribute("specialConstants", specialConstants);

        model.addAttribute("session",
                SessionViewModel.builder()
                        .setUseCompiledCSS(getConfiguration().isUseCompiledCSS())
                        .setUseCompiledJavaScript(getConfiguration().isUseCompiledJavaScript())
                        // todo get the appropriate js compile / non-compile url
                        .build());

        boolean emptyClients = true;
        for (final Environment environment : Environment.values()) {
            emptyClients &= specificationSource.loadAllSpecifications(environment).keySet().isEmpty();
        }
        model.addAttribute("emptyClients", emptyClients);

        final ConsumableTestDefinition consumableTestDefinition = ProctorUtils.convertToConsumableTestDefinition(definition);

        final Set<AppVersion> devApplications = specificationSource.activeClients(Environment.WORKING, testName);
        final Set<AppVersion> devDynamicClients =
                filterDynamicClients(Environment.WORKING, testName, consumableTestDefinition, devApplications);
        model.addAttribute("devApplications", devApplications);
        model.addAttribute("devDynamicClients", devDynamicClients);

        final Set<AppVersion> qaApplications = specificationSource.activeClients(Environment.QA, testName);
        final Set<AppVersion> qaDynamicClients =
                filterDynamicClients(Environment.QA, testName, consumableTestDefinition, qaApplications);
        model.addAttribute("qaApplications", qaApplications);
        model.addAttribute("qaDynamicClients", qaDynamicClients);

        final Set<AppVersion> productionApplications = specificationSource.activeClients(Environment.PRODUCTION, testName);
        final Set<AppVersion> productionDynamicClients =
                filterDynamicClients(Environment.PRODUCTION, testName, consumableTestDefinition, productionApplications);
        model.addAttribute("productionApplications", productionApplications);
        model.addAttribute("productionDynamicClients", productionDynamicClients);

        try {
            // convert to artifact?
            final StringWriter sw = new StringWriter();
            ProctorUtils.serializeTestDefinition(sw, definition);
            model.addAttribute("testDefinitionJson", sw.toString());
        } catch (JsonGenerationException e) {
            LOGGER.error("Could not generate JSON", e);
        } catch (JsonMappingException e) {
            LOGGER.error("Could not generate JSON", e);
        } catch (IOException e) {
            LOGGER.error("Could not generate JSON", e);
        }

        try {
            final StringWriter swSpecification = new StringWriter();
            ProctorUtils.serializeTestSpecification(swSpecification, ProctorUtils.generateSpecification(definition));
            model.addAttribute("testSpecificationJson", swSpecification.toString());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not generate Test Specification", e);
        } catch (JsonGenerationException e) {
            LOGGER.error("Could not generate JSON", e);
        } catch (JsonMappingException e) {
            LOGGER.error("Could not generate JSON", e);
        } catch (IOException e) {
            LOGGER.error("Could not generate JSON", e);
        }

        model.addAttribute("testDefinitionHistory", history);
        final Revision testDefinitionVersion = version == null ? EnvironmentVersion.FULL_UNKNOWN_REVISION : version.getFullRevision(b);
        model.addAttribute("testDefinitionVersion", testDefinitionVersion);

        // TODO (parker) 8/9/12 - Add common model for TestTypes and other Drop Downs
        model.addAttribute("testTypes", Arrays.asList(TestType.values()));

        return view.getName();
    }

    /**
     * This needs to be moved to a separate checker class implementing some interface
     */
    private URL getSpecificationUrl(final ProctorClientApplication client) {
        final String urlStr = client.getBaseApplicationUrl() + "/private/proctor/specification";
        try {
            return new URL(urlStr);
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Somehow created a malformed URL: " + urlStr, e);
        }
    }

    private TestDefinition getTestDefinition(final Environment environment, final String testName, final String revision) {
        final ProctorStore store = determineStoreFromEnvironment(environment);
        return TestDefinitionUtil.getTestDefinition(store, promoter, environment, testName, revision);
    }

    private Set<AppVersion> filterDynamicClients(final Environment environment,
                                                 final String testName,
                                                 final ConsumableTestDefinition testDefinition,
                                                 final Set<AppVersion> applications) {
        final Map<AppVersion, ProctorSpecification> specifications =
                specificationSource.loadAllSuccessfulSpecifications(environment);
        return applications.stream()
                .filter(appVersion -> isDynamicTest(testName, testDefinition, specifications.get(appVersion)))
                .collect(Collectors.toSet());
    }

    private static boolean isDynamicTest(final String testName,
                                         final ConsumableTestDefinition testDefinition,
                                         @Nullable final ProctorSpecification specification) {
        if (specification == null) {
            return false;
        }
        final DynamicFilters filters = specification.getDynamicFilters();
        final Set<String> requiedTests = specification.getTests().keySet();
        final Set<String> dynamicTests = filters.determineTests(ImmutableMap.of(testName, testDefinition), requiedTests);
        return dynamicTests.contains(testName);
    }
}
