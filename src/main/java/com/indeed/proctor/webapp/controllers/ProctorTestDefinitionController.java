package com.indeed.proctor.webapp.controllers;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.controllers.BackgroundJob.ResultUrl;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.ProctorSpecificationSource;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.extensions.DefinitionChangeLog;
import com.indeed.proctor.webapp.extensions.PostDefinitionEditChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionCreateChange;
import com.indeed.proctor.webapp.extensions.PostDefinitionCreateChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionEditChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionDeleteChange;
import com.indeed.proctor.webapp.extensions.PostDefinitionDeleteChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionPromoteChange;
import com.indeed.proctor.webapp.extensions.PostDefinitionPromoteChange;
import com.indeed.proctor.webapp.extensions.RevisionCommitCommentFormatter;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.ProctorClientApplication;
import com.indeed.proctor.webapp.model.SessionViewModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.tags.TestDefinitionFunctions;
import com.indeed.proctor.webapp.tags.UtilityFunctions;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.proctor.webapp.views.JsonView;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * @author parker
 */
@Controller
@RequestMapping({"/definition", "/proctor/definition"})
public class ProctorTestDefinitionController extends AbstractController {
    private static final Logger LOGGER = Logger.getLogger(ProctorTestDefinitionController.class);
    private static final Revision UNKNOWN_VERSION = new Revision("", "[unknown]", new Date(0), "History unknown");

    private static final Pattern ALPHA_NUMERIC_PATTERN = Pattern.compile("^[a-z0-9_]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_TEST_NAME_PATTERN = ALPHA_NUMERIC_PATTERN;

    private final ProctorPromoter promoter;

    private final ProctorSpecificationSource specificationSource;
    private final int verificationTimeout;
    private final ExecutorService verifierExecutor;

    private final BackgroundJobManager jobManager;

    /*
       TODO: preDefinitionChanges and postDefinitionChanges should be included in the autowird constructor.
       Four constructors would need to be made, which leads to type erasure problems.
     */
    @Autowired(required=false)
    private List<PreDefinitionEditChange> preDefinitionEditChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PostDefinitionEditChange> postDefinitionEditChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PreDefinitionCreateChange> preDefinitionCreateChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PostDefinitionCreateChange> postDefinitionCreateChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PreDefinitionDeleteChange> preDefinitionDeleteChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PostDefinitionDeleteChange> postDefinitionDeleteChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PreDefinitionPromoteChange> preDefinitionPromoteChanges = Collections.emptyList();
    @Autowired(required=false)
    private List<PostDefinitionPromoteChange> postDefinitionPromoteChanges = Collections.emptyList();
    @Autowired(required=false)
    private RevisionCommitCommentFormatter revisionCommitCommentFormatter;



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
                                           final BackgroundJobManager jobManager) {
        super(configuration, trunkStore, qaStore, productionStore);
        this.promoter = promoter;
        this.jobManager = jobManager;

        this.verificationTimeout = configuration.getVerifyHttpTimeout();
        this.specificationSource = specificationSource;
        Preconditions.checkArgument(verificationTimeout > 0, "verificationTimeout > 0");
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("proctor-verifiers-Thread-%d")
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
        this.verifierExecutor = Executors.newFixedThreadPool(configuration.getVerifyExecutorThreads(), threadFactory);

    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(
        final Model model
    ) {

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
        final List<Revision> history = Collections.emptyList();
        final EnvironmentVersion version = null;
        return doView(Environment.WORKING, Views.CREATE, "", definition, history, version, model);
    }

    @RequestMapping(value = "/{testName}", method = RequestMethod.GET)
    public String show(
        @PathVariable final String testName,
        @RequestParam(required = false) final String branch,
        @RequestParam(required = false, defaultValue = "", value = "r") final String revision,
        final Model model
    ) {
        final Environment theEnvironment = determineEnvironmentFromParameter(branch);
        final ProctorStore store = determineStoreFromEnvironment(theEnvironment);

        final TestDefinition definition;
        if (revision.length() > 0) {
            definition = getTestDefinition(store, testName, revision);
        } else {
            definition = getTestDefinition(store, testName);
        }

        if (definition == null) {
            LOGGER.info("Unknown test definition : " + testName + " revision " + revision);
            // unknown testdefinition
            return "404";
        }
        final List<Revision> history = getTestHistory(store, testName);
        final EnvironmentVersion version = promoter.getEnvironmentVersion(testName);
        return doView(theEnvironment, Views.DETAILS, testName, definition, history, version, model);
    }

    @RequestMapping(value = "/{testName}/edit", method = RequestMethod.GET)
    public String doEditGet(
        @PathVariable String testName,
        final Model model
    ) {
        final Environment theEnvironment = Environment.WORKING; // only allow editing of TRUNK!
        final ProctorStore store = determineStoreFromEnvironment(theEnvironment);

        final TestDefinition definition = getTestDefinition(store, testName);
        if (definition == null) {
            LOGGER.info("Unknown test definition : " + testName);
            // unknown testdefinition
            return "404";
        }
        final List<Revision> history = getTestHistory(store, testName);
        final EnvironmentVersion version = promoter.getEnvironmentVersion(testName);
        return doView(theEnvironment, Views.EDIT, testName, definition, history, version, model);
    }

    @RequestMapping(value = "/{testName}/delete", method = RequestMethod.POST)
    public View doDeletePost(
        @PathVariable final String testName,
        @RequestParam(required = false) String src,
        @RequestParam(required = false) final String srcRevision,

        @RequestParam(required = false) final String username,
        @RequestParam(required = false) final String password,
        @RequestParam(required = false) final String comment,
        final HttpServletRequest request,
        final Model model
    ) {
        final Environment theEnvironment = determineEnvironmentFromParameter(src);

        Map<String, String[]> requestParameterMap = new HashMap<String, String[]>();
        requestParameterMap.putAll(request.getParameterMap());
        final BackgroundJob<Boolean> job = createDeleteBackgroundJob(testName,
                                                                     theEnvironment,
                                                                     srcRevision,
                                                                     username,
                                                                     password,
                                                                     comment,
                                                                     requestParameterMap);
        jobManager.submit(job);

        if (isAJAXRequest(request)) {
            final JsonResponse<Map> response = new JsonResponse<Map>(BackgroundJobRpcController.buildJobJson(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }

    }

    private BackgroundJob<Boolean> createDeleteBackgroundJob(
        final String testName,
        final Environment source,
        final String srcRevision,

        final String username,
        final String password,
        final String comment,
        final Map<String, String[]> requestParameterMap


    ) {
        LOGGER.info(String.format("Deleting test %s branch: %s user: %s ", testName, source, username));
        return new BackgroundJob<Boolean>() {
            @Override
            public String getTitle() {
                return String.format("(%s) deleting %s branch: %s ", username, testName, source);
            }

            @Override
            public Boolean call() throws Exception {
                final ProctorStore store = determineStoreFromEnvironment(source);
                final TestDefinition definition = getTestDefinition(store, testName);
                if (definition == null) {
                    log("Unknown test definition : " + testName);
                    return false;
                }

                try {
                    validateUsernamePassword(username, password);

                    final Revision prevVersion;
                    log("(svn) getting svn history for '" + testName + "'");
                    final List<Revision> history = getTestHistory(store, testName, 1);
                    if (history.size() > 0) {
                        prevVersion = history.get(0);
                        if (!prevVersion.getRevision().equals(srcRevision)) {
                            throw new IllegalArgumentException("Test has been updated since r" + srcRevision + " currently at r" + prevVersion.getRevision());
                        }
                    } else {
                        throw new IllegalArgumentException("Could not get any history for " + testName);
                    }

                    final String fullComment = formatFullComment(comment, requestParameterMap);


                    //PreDefinitionDeleteChanges
                    log("Executing pre delete extension tasks.");
                    for (final PreDefinitionDeleteChange preDefinitionDeleteChange: preDefinitionDeleteChanges) {
                        final DefinitionChangeLog definitionChangeLog = preDefinitionDeleteChange.preDelete(definition, requestParameterMap);
                        logDefinitionChangeLog(definitionChangeLog, preDefinitionDeleteChange.getClass().getSimpleName(), this);
                    }

                    log("(svn) delete " + testName);
                    store.deleteTestDefinition(username, password, srcRevision, testName, definition, fullComment);
                    addUrl("/proctor?branch=" + source.getName(), "View Result");

                    //PostDefinitionDeleteChanges
                    log("Executing post delete extension tasks.");
                    for (final PostDefinitionDeleteChange postDefinitionDeleteChange: postDefinitionDeleteChanges) {
                        final DefinitionChangeLog definitionChangeLog = postDefinitionDeleteChange.postDelete(requestParameterMap);
                        logDefinitionChangeLog(definitionChangeLog, postDefinitionDeleteChange.getClass().getSimpleName(), this);
                    }


                } catch (StoreException.TestUpdateException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Deletion Failed: " + getTitle(), exp);
                } catch (IllegalArgumentException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Deletion Failed: " + getTitle(), exp);
                } catch (Exception e) {
                    logFailedJob(this, e);
                    LOGGER.error("Deletion Failed: " + getTitle(), e);
                }
                return null;
            }
        };
    }


    @RequestMapping(value = "/{testName}/promote", method = RequestMethod.POST)
    public View doPromotePost(
        @PathVariable final String testName,
        @RequestParam(required = false) final String username,
        @RequestParam(required = false) final String password,

        @RequestParam(required = false) final String src,
        @RequestParam(required = false) final String srcRevision,
        @RequestParam(required = false) final String dest,
        @RequestParam(required = false) final String destRevision,
        final HttpServletRequest request,
        final Model model
    ) {
        final Environment source = determineEnvironmentFromParameter(src);
        final Environment destination = determineEnvironmentFromParameter(dest);

        final Map<String, String[]> requestParameterMap = new HashMap<String, String[]>();
        requestParameterMap.putAll(request.getParameterMap());
        final BackgroundJob job = doPromoteInternal(testName, username, password, source, srcRevision, destination, destRevision, requestParameterMap);
        jobManager.submit(job);

        if (isAJAXRequest(request)) {
            final JsonResponse<Map> response = new JsonResponse<Map>(BackgroundJobRpcController.buildJobJson(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            return new RedirectView("/proctor/definition/" + UtilityFunctions.urlEncode(testName) + "?branch=" + destination.getName());
        }
    }

    private BackgroundJob doPromoteInternal(final String testName,
                                            final String username,
                                            final String password,
                                            final Environment source,
                                            final String srcRevision,
                                            final Environment destination,
                                            final String destRevision,
                                            final Map<String, String[]> requestParameterMap
    ) {
        return new BackgroundJob<Void>() {
            @Override
            public String getTitle() {
                return String.format("(%s) promoting %s %s r%s to %s", username, testName, source, srcRevision, destination);
            }

            @Override
            public Void call() throws Exception {
                final Map<String, String> metadata = Collections.emptyMap();
                /*
                    Valid permutations:
                    TRUNK -> QA
                    TRUNK -> PRODUCTION
                    QA -> PRODUCTION
                 */
                try {
                    validateUsernamePassword(username, password);

                    // TODO (parker) 9/5/12 - Verify that promoting to the destination branch won't cause issues
                    final ProctorStore srcStore = determineStoreFromEnvironment(source);
                    final TestDefinition testDefintion = getTestDefinition(srcStore, testName, srcRevision);
                    //            if(d == null) {
                    //                return "could not find " + testName + " on " + source + " with revision " + srcRevision;
                    //            }

                    final CheckMatrixResult result = checkMatrix(destination, testName, testDefintion);
                    if (!result.isValid()) {
                        throw new IllegalArgumentException(String.format("Test Promotion not compatible, errors: %s", Joiner.on("\n").join(result.getErrors())));
                    } else {
                        final Map<Environment, PromoteAction> actions = PROMOTE_ACTIONS.get(source);
                        if (actions == null || !actions.containsKey(destination)) {
                            throw new IllegalArgumentException("Invalid combination of source and destination: source=" + source + " dest=" + destination);
                        }
                        final PromoteAction action = actions.get(destination);
                        final BackgroundJob job = this;

                        //PreDefinitionPromoteChanges
                        log("Executing pre promote extension tasks.");
                        for (final PreDefinitionPromoteChange preDefinitionPromoteChange: preDefinitionPromoteChanges) {
                            final DefinitionChangeLog definitionChangeLog = preDefinitionPromoteChange.prePromote(testDefintion, requestParameterMap, source, destination);
                            logDefinitionChangeLog(definitionChangeLog, preDefinitionPromoteChange.getClass().getSimpleName(), this);
                        }

                        //Promote Change
                        final boolean success = action.promoteTest(job, testName, srcRevision, destRevision, username, password, metadata);

                        //PostDefinitionPromoteChanges
                        log("Executing post promote extension tasks.");
                        for (final PostDefinitionPromoteChange postDefinitionPromoteChange: postDefinitionPromoteChanges) {
                            final DefinitionChangeLog definitionChangeLog = postDefinitionPromoteChange.postPromote(requestParameterMap, source, destination);
                            logDefinitionChangeLog(definitionChangeLog, postDefinitionPromoteChange.getClass().getSimpleName(), this);
                        }


                        log(String.format("Promoted %s from %s (r%s) to %s (r%s)", testName, source.getName(), srcRevision, destination.getName(), destRevision));
                        addUrl("/proctor/definition/" + UtilityFunctions.urlEncode(testName) + "?branch=" + destination.getName(), "view " + testName + " on " + destination.getName());
                    }
                } catch (ProctorPromoter.TestPromotionException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Promotion Failed: " + getTitle(), exp);
                } catch (StoreException.TestUpdateException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Promotion Failed: " + getTitle(), exp);
                } catch (IllegalArgumentException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Promotion Failed: " + getTitle(), exp);
                } catch (Exception exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Promotion Failed: " + getTitle(), exp);
                }

                return null;
            }
        };
    }

    private void logDefinitionChangeLog(DefinitionChangeLog definitionChangeLog, String changeName, BackgroundJob backgroundJob) {
        if (definitionChangeLog != null) {
            final List<ResultUrl> urls = definitionChangeLog.getUrls();
            if (urls != null) {
                for (final ResultUrl url : urls) {
                    backgroundJob.addUrl(url);
                }
            }

            final List<String> changeLog = definitionChangeLog.getLog();
            if (changeLog != null) {
                for (final String logMessage : changeLog) {
                    backgroundJob.log(logMessage);
                }
            }

            if (definitionChangeLog.isErrorsFound()) {
                throw new RuntimeException(changeName + " failed with the following errors: " + definitionChangeLog.getErrors());
            }
        }
    }

    private static interface PromoteAction {
        Environment getSource();

        Environment getDestination();

        boolean promoteTest(BackgroundJob job,
                            final String testName,
                            final String srcRevision,
                            final String destRevision,
                            final String username,
                            final String password,
                            final Map<String, String> metadata) throws IllegalArgumentException, ProctorPromoter.TestPromotionException, StoreException.TestUpdateException;
    }

    private abstract class PromoteActionBase implements PromoteAction {
        final Environment src;
        final Environment destination;


        protected PromoteActionBase(final Environment src,
                                    final Environment destination) {
            this.destination = destination;
            this.src = src;
        }

        @Override
        public boolean promoteTest(final BackgroundJob job,
                                   final String testName,
                                   final String srcRevision,
                                   final String destRevision,
                                   final String username,
                                   final String password,
                                   final Map<String, String> metadata) throws IllegalArgumentException, ProctorPromoter.TestPromotionException, StoreException.TestUpdateException, StoreException.TestUpdateException {
            try {
                doPromotion(job, testName, srcRevision, destRevision, username, password, metadata);
                return true;
            } catch (Exception t) {
                Throwables.propagateIfInstanceOf(t, ProctorPromoter.TestPromotionException.class);
                Throwables.propagateIfInstanceOf(t, StoreException.TestUpdateException.class);
                throw Throwables.propagate(t);
            }
        }

        @Override
        public final Environment getSource() {
            return src;
        }

        @Override
        public final Environment getDestination() {
            return destination;
        }

        abstract void doPromotion(BackgroundJob job, String testName, String srcRevision, String destRevision,
                                  String username, String password, Map<String, String> metadata)
                throws ProctorPromoter.TestPromotionException, StoreException;
    }

    private final PromoteAction TRUNK_TO_QA = new PromoteActionBase(Environment.WORKING,
                                                                    Environment.QA) {
        @Override
        void doPromotion(final BackgroundJob job,
                         final String testName,
                         final String srcRevision,
                         final String destRevision,
                         final String username,
                         final String password,
                         final Map<String, String> metadata)
                throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(svn) promote %sr%s (trunk to qa)", testName, srcRevision));
            promoter.promoteTrunkToQa(testName, srcRevision, destRevision, username, password, metadata);
        }
    };

    private final PromoteAction TRUNK_TO_PRODUCTION = new PromoteActionBase(Environment.WORKING,
                                                                            Environment.PRODUCTION) {
        @Override
        void doPromotion(final BackgroundJob job,
                         final String testName,
                         final String srcRevision,
                         final String destRevision,
                         final String username,
                         final String password,
                         final Map<String, String> metadata)
                throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(svn) promote %sr%s (trunk to production)", testName, srcRevision));
            promoter.promoteTrunkToProduction(testName, srcRevision, destRevision, username, password, metadata);
        }
    };

    private final PromoteAction QA_TO_PRODUCTION = new PromoteActionBase(Environment.QA,
                                                                         Environment.PRODUCTION) {
        @Override
        void doPromotion(final BackgroundJob job,
                         final String testName,
                         final String srcRevision,
                         final String destRevision,
                         final String username,
                         final String password,
                         final Map<String, String> metadata) throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(svn) promote %sr%s (qa to production)", testName, srcRevision));
            promoter.promoteQaToProduction(testName, srcRevision, destRevision, username, password, metadata);
        }
    };


    private final Map<Environment, Map<Environment, PromoteAction>> PROMOTE_ACTIONS = ImmutableMap.<Environment, Map<Environment, PromoteAction>>builder()
        .put(Environment.WORKING, ImmutableMap.of(Environment.QA, TRUNK_TO_QA, Environment.PRODUCTION, TRUNK_TO_PRODUCTION))
        .put(Environment.QA, ImmutableMap.of(Environment.PRODUCTION, QA_TO_PRODUCTION)).build();


    @RequestMapping(value = "/{testName}/edit", method = RequestMethod.POST)
    public View doEditPost(
        @PathVariable final String testName,
        @RequestParam(required = false) final String username,
        @RequestParam(required = false) final String password,
        @RequestParam(required = false, defaultValue = "false") final boolean isCreate,
        @RequestParam(required = false) final String comment,
        @RequestParam(required = false) final String testDefinition, // testDefinition is JSON representation of test-definition
        @RequestParam(required = false, defaultValue = "") final String previousRevision,
        final HttpServletRequest request,
        final Model model) {

        //TODO: Remove all internal params and just pass request.getParameterMap() to doEditPost() as map of fields and values

        Map<String, String[]> requestParameterMap = new HashMap<String, String[]>();
        requestParameterMap.putAll(request.getParameterMap());
        final BackgroundJob job = doEditPost(testName,
                                             username,
                                             password,
                                             isCreate,
                                             comment,
                                             testDefinition,
                                             previousRevision,
                                             requestParameterMap);
        jobManager.submit(job);
        if (isAJAXRequest(request)) {
            final JsonResponse<Map> response = new JsonResponse<Map>(BackgroundJobRpcController.buildJobJson(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }
    }

    private BackgroundJob<Boolean> doEditPost(
        final String testName,
        final String username,
        final String password,
        final boolean isCreate,
        final String comment,
        final String testDefinitionJson,
        final String previousRevision,
        final Map<String, String[]> requestParameterMap) {

        return new BackgroundJob<Boolean>() {
            @Override
            public String getTitle() {
                return String.format("(%s) %s %s", username, (isCreate ? "Creating" : "Editing"), testName);
            }

            @Override
            public Boolean call() throws Exception {
                final Environment theEnvironment = Environment.WORKING; // only allow editing of TRUNK!
                final ProctorStore store = determineStoreFromEnvironment(theEnvironment);

                try {
                    if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(testDefinitionJson))) {
                        throw new IllegalArgumentException("No new test definition given");
                    }
                    validateUsernamePassword(username, password);
                    validateComment(comment);


                    final Revision prevVersion;
                    if (previousRevision.length() > 0) {
                        log("(svn) getting svn history for '" + testName + "'");
                        final List<Revision> history = getTestHistory(store, testName, 1);
                        if (history.size() > 0) {
                            prevVersion = history.get(0);
                            if (! prevVersion.getRevision().equals(previousRevision)) {
                                throw new IllegalArgumentException("Test has been updated since r" + previousRevision + " currently at r" + prevVersion.getRevision());
                            }
                        } else {
                            prevVersion = null;
                        }
                    } else {
                        // Create flow
                        prevVersion = null;
                        // check that the test name is valid
                        final Matcher m = VALID_TEST_NAME_PATTERN.matcher(testName);
                        if (!m.matches()) {
                            throw new IllegalArgumentException("Test Name must be alpha-numeric underscore, found: '" + testName + "'");
                        }
                    }

                    final TestDefinition testDefinitionToUpdate;
                    log("Parsing test definition json");
                    testDefinitionToUpdate = TestDefinitionFunctions.parseTestDefinition(testDefinitionJson);

                    //  TODO: make these parameters
                    final boolean skipVerification = true;
                    //  TODO: make these parameters
                    final boolean allowInstanceFailure = true;

                    final ProctorStore trunkStore = determineStoreFromEnvironment(Environment.WORKING);
                    log("(svn) loading existing test definition for '" + testName + "'");
                    // Getting the TestDefinition via currentTestMatrix instead of trunkStore.getTestDefinition because the test
                    final TestDefinition existingTestDefinition = trunkStore.getCurrentTestMatrix().getTestMatrixDefinition().getTests().get(testName);
                    if (previousRevision.length() <= 0 && existingTestDefinition != null) {
                        throw new IllegalArgumentException("Current tests exists with name : '" + testName + "'");
                    }

                    if (testDefinitionToUpdate.getTestType() == null && existingTestDefinition != null) {
                        testDefinitionToUpdate.setTestType(existingTestDefinition.getTestType());
                    }
                    log("verifying test definition and buckets");
                    validateBasicInformation(testDefinitionToUpdate, this);

                    final ConsumableTestDefinition consumableTestDefinition = ProctorUtils.convertToConsumableTestDefinition(testDefinitionToUpdate);
                    ProctorUtils.verifyInternallyConsistentDefinition(testName, "edit", consumableTestDefinition);



                    //PreDefinitionEdit
                    if (isCreate) {
                        log("Executing pre create extension tasks.");
                        for (final PreDefinitionCreateChange preDefinitionCreateChange: preDefinitionCreateChanges) {
                            final DefinitionChangeLog definitionChangeLog = preDefinitionCreateChange.preCreate(testDefinitionToUpdate, requestParameterMap);
                            logDefinitionChangeLog(definitionChangeLog, preDefinitionCreateChange.getClass().getSimpleName(), this);
                        }
                    } else {
                        log("Executing pre edit extension tasks.");
                        for (final PreDefinitionEditChange preDefinitionEditChange: preDefinitionEditChanges) {
                            final DefinitionChangeLog definitionChangeLog = preDefinitionEditChange.preEdit(existingTestDefinition, testDefinitionToUpdate, requestParameterMap);
                            logDefinitionChangeLog(definitionChangeLog, preDefinitionEditChange.getClass().getSimpleName(), this);
                        }
                    }


                    final String fullComment = formatFullComment(comment, requestParameterMap);

                    //Change definition
                    final Map<String, String> metadata = Collections.emptyMap();
                    if (existingTestDefinition == null) {
                        log("(svn) adding test definition");
                        trunkStore.addTestDefinition(username, password, testName, testDefinitionToUpdate, metadata, fullComment);
                        promoter.refreshWorkingVersion(testName);
                    } else {
                        log("(svn) updating test definition");
                        trunkStore.updateTestDefinition(username, password, previousRevision, testName, testDefinitionToUpdate, metadata, fullComment);
                        promoter.refreshWorkingVersion(testName);
                    }



                    //PostDefinitionEdit
                    if (isCreate) {
                        log("Executing post create extension tasks.");
                        for (final PostDefinitionCreateChange postDefinitionCreateChange : postDefinitionCreateChanges) {
                            final DefinitionChangeLog definitionChangeLog = postDefinitionCreateChange.postCreate(testDefinitionToUpdate, requestParameterMap);
                            logDefinitionChangeLog(definitionChangeLog, postDefinitionCreateChange.getClass().getSimpleName(), this);

                        }
                    } else {
                        log("Executing post edit extension tasks.");
                        for (final PostDefinitionEditChange postDefinitionEditChange : postDefinitionEditChanges) {
                            final DefinitionChangeLog definitionChangeLog = postDefinitionEditChange.postEdit(testDefinitionToUpdate, requestParameterMap);
                            logDefinitionChangeLog(definitionChangeLog, postDefinitionEditChange.getClass().getSimpleName(), this);
                        }
                    }

                    log("COMPLETE");
                    addUrl("/proctor/definition/" + UtilityFunctions.urlEncode(testName) + "?branch=" + theEnvironment.getName(), "View Result");
                    return true;
                } catch (final StoreException.TestUpdateException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Edit Failed: " + getTitle(), exp);
                } catch (IncompatibleTestMatrixException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Edit Failed: " + getTitle(), exp);
                } catch (IllegalArgumentException exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Edit Failed: " + getTitle(), exp);
                } catch (Exception exp) {
                    logFailedJob(this, exp);
                    LOGGER.error("Edit Failed: " + getTitle(), exp);
                }
                return false;
            }
        };
    }

    private String formatFullComment(final String comment, final Map<String,String[]> requestParameterMap) {
        if (revisionCommitCommentFormatter != null) {
            return revisionCommitCommentFormatter.formatComment(comment, requestParameterMap);
        }
        else return comment.trim();
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

        final ProctorStore source = determineStoreFromEnvironment(srcBranch);

        final TestDefinition d = getTestDefinition(source, testName, srcRevision);
        if (d == null) {
            return "could not find " + testName + " on " + srcBranch + " with revision " + srcRevision;
        }

        final CheckMatrixResult result = checkMatrix(destBranch, testName, d);
        if (result.isValid()) {
            return "check success";
        } else {
            return "failed: " + Joiner.on("\n").join(result.getErrors());
        }
    }




    private CheckMatrixResult checkMatrix(final Environment checkAgainst,
                                          final String testName,
                                          final TestDefinition potential) {
        final TestMatrixVersion tmv = new TestMatrixVersion();
        tmv.setAuthor("author");
        tmv.setVersion("");
        tmv.setDescription("fake matrix for validation of " + testName);
        tmv.setPublished(new Date());

        final TestMatrixDefinition tmd = new TestMatrixDefinition(ImmutableMap.<String, TestDefinition>of(testName, potential));
        tmv.setTestMatrixDefinition(tmd);

        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(tmv);
        // Verify
        final Map<AppVersion, IncompatibleTestMatrixException> matrixErrors = Maps.newLinkedHashMap();
        final Map<AppVersion, Throwable> exceptions = Maps.newLinkedHashMap();
        final Map<AppVersion, Future<AppVersion>> futures = Maps.newLinkedHashMap();
        final Set<AppVersion> appVersionsToCheck = Sets.newLinkedHashSet();

        final Set<String> limitToTests = Collections.singleton(testName);

        final Map<AppVersion, ProctorSpecification> toVerify = specificationSource.loadAllSuccessfulSpecifications(checkAgainst);
        for (Map.Entry<AppVersion, ProctorSpecification> entry : toVerify.entrySet()) {
            final AppVersion appVersion = entry.getKey();
            final ProctorSpecification specification = entry.getValue();
            futures.put(appVersion, verifierExecutor.submit(new Callable<AppVersion>() {
                @Override
                public AppVersion call() throws Exception {
                    LOGGER.info("Verifying artifact against : cached " + appVersion);
                    verify(specification, artifact, limitToTests, appVersion.toString());
                    return appVersion;
                }
            }));

        }

        while (!futures.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                LOGGER.error("Oh heavens", e);
            }
            for (final Iterator<Map.Entry<AppVersion, Future<AppVersion>>> iterator = futures.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<AppVersion, Future<AppVersion>> entry = iterator.next();
                final AppVersion version = entry.getKey();
                final Future<AppVersion> future = entry.getValue();
                if (future.isDone()) {
                    iterator.remove();
                    try {
                        final AppVersion appVersion = future.get();
                        appVersionsToCheck.remove(appVersion);
                    } catch (final InterruptedException e) {
                        LOGGER.error("Interrupted getting " + version, e);
                    } catch (final ExecutionException e) {
                        final Throwable cause = e.getCause();
                        if (cause instanceof IncompatibleTestMatrixException) {
                            matrixErrors.put(version, (IncompatibleTestMatrixException) cause);
                        } else {
                            exceptions.put(version, cause);
                            LOGGER.error("Unable to verify " + version, cause);
                        }
                    }
                }
            }
        }

        final boolean greatSuccess = matrixErrors.isEmpty() && appVersionsToCheck.isEmpty();
        final ImmutableList.Builder<String> errors = ImmutableList.builder();
        for (Map.Entry<AppVersion, Throwable> entry : exceptions.entrySet()) {
            final AppVersion appVersion = entry.getKey();
            errors.add(appVersion.toString() + " failed. " + entry.getValue().getMessage());
        }
        for (Map.Entry<AppVersion, IncompatibleTestMatrixException> entry : matrixErrors.entrySet()) {
            final AppVersion client = entry.getKey();
            errors.add(client.toString() + " failed. " + entry.getValue().getMessage());
        }

        return new CheckMatrixResult(greatSuccess, errors.build());
    }

    private void verify(final ProctorSpecification spec,
                        final TestMatrixArtifact testMatrix,
                        final Set<String> retrictToTests,
                        final String matrixSource) throws IncompatibleTestMatrixException {
        final Map<String, TestSpecification> requiredTests;
        if (retrictToTests.isEmpty()) {
            requiredTests = spec.getTests();
        } else {
            requiredTests = Maps.newHashMapWithExpectedSize(retrictToTests.size());
            for (String test : retrictToTests) {
                if (spec.getTests().containsKey(test)) {
                    requiredTests.put(test, spec.getTests().get(test));
                }
            }
        }
        ProctorUtils.verify(testMatrix, matrixSource, requiredTests);
    }


    private static void validateUsernamePassword(String username, String password) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(username)) || CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(password))) {
            throw new IllegalArgumentException("No username or password provided");
        }
    }



    private void validateComment(String comment) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(comment))) {
            throw new IllegalArgumentException("Comment is required.");
        }
    }

    private void validateBasicInformation(final TestDefinition definition,
                                          final BackgroundJob backgroundJob) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(definition.getDescription()))) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(definition.getSalt()))) {
            throw new IllegalArgumentException("Salt is required.");
        }
        if (definition.getTestType() == null) {
            throw new IllegalArgumentException("TestType is required.");
        }

        if (definition.getBuckets().isEmpty()) {
            throw new IllegalArgumentException("Buckets cannot be empty.");
        }

        if (definition.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("Allocations cannot be empty.");
        }

        validateAllocationsAndBuckets(definition, backgroundJob);
    }

    private void validateAllocationsAndBuckets(final TestDefinition definition, final BackgroundJob backgroundJob) throws IllegalArgumentException {
        final Allocation allocation = definition.getAllocations().get(0);
        final List<Range> ranges = allocation.getRanges();
        final TestType testType = definition.getTestType();
        final int controlBucketValue = 0;
        final double DELTA = 1E-6;

        final Map<Integer, Double> totalTestAllocationMap = new HashMap<Integer, Double>();
        for (Range range : ranges) {
            final int bucketValue = range.getBucketValue();
            double bucketAllocation = range.getLength();
            if (totalTestAllocationMap.containsKey(bucketValue)) {
                bucketAllocation += totalTestAllocationMap.get(bucketValue);
            }
            totalTestAllocationMap.put(bucketValue, bucketAllocation);
        }

        final boolean hasControlBucket = totalTestAllocationMap.containsKey(controlBucketValue);
        /* The number of buckets with allocation greater than zero */
        int numActiveBuckets = 0;

        for (Integer bucketValue : totalTestAllocationMap.keySet()) {
            final double totalBucketAllocation = totalTestAllocationMap.get(bucketValue);
            if(totalBucketAllocation > 0) {
                numActiveBuckets++;
            }
        }

        /* if there are 2 buckets with positive allocations, test and control buckets
            should be the same size
        */
        if(numActiveBuckets > 1 && hasControlBucket) {
            final double totalControlBucketAllocation = totalTestAllocationMap.get(controlBucketValue);
            for (Integer bucketValue : totalTestAllocationMap.keySet()) {
                final double totalBucketAllocation = totalTestAllocationMap.get(bucketValue);
                if (totalBucketAllocation > 0) {
                    numActiveBuckets++;
                }
                final double difference = totalBucketAllocation - totalControlBucketAllocation;
                if (bucketValue > 0 && totalBucketAllocation > 0 && Math.abs(difference) >= DELTA) {
                    backgroundJob.log("WARNING: Positive bucket total allocation size not same as control bucket total allocation size. \nBucket #" + bucketValue + "=" + totalBucketAllocation + ", Zero Bucket=" + totalControlBucketAllocation);
                }
            }
        }

        /* If there are 2 buckets with positive allocations, one should be control */
        if (numActiveBuckets > 1 && !hasControlBucket) {
            backgroundJob.log("WARNING: You should have a zero bucket (control).");
        }

        for (TestBucket bucket : definition.getBuckets()) {
            if (testType == TestType.PAGE && bucket.getValue() < 0) {
                throw new IllegalArgumentException("PAGE tests cannot contain negative buckets.");
            }
        }
    }



    private String doView(final Environment b,
                          final Views view,
                          final String testName,
                          // TODO (parker) 7/27/12 - add Revisioned (that has Revision + testName)
                          final TestDefinition definition,
                          final List<Revision> history,
                          final EnvironmentVersion version,
                          Model model) {
        model.addAttribute("testName", testName);
        model.addAttribute("testDefinition", definition);
        model.addAttribute("isCreate", view == Views.CREATE);
        model.addAttribute("branch", b);
        model.addAttribute("version", version);

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

        model.addAttribute("emptyClients", specificationSource.loadAllSpecifications(b).keySet().isEmpty());

        final Set<AppVersion> devApplications = specificationSource.activeClients(Environment.WORKING, testName);
        model.addAttribute("devApplications", devApplications);
        final Set<AppVersion> qaApplications = specificationSource.activeClients(Environment.QA, testName);
        model.addAttribute("qaApplications", qaApplications);
        final Set<AppVersion> productionApplications = specificationSource.activeClients(Environment.PRODUCTION, testName);
        model.addAttribute("productionApplications", productionApplications);

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

        model.addAttribute("testDefinitionHistory", history);
        model.addAttribute("testDefinitionVersion", history.size() > 0 && history.get(0) != null ? history.get(0) : UNKNOWN_VERSION);

        // TODO (parker) 8/9/12 - Add common model for TestTypes and other Drop Downs
        model.addAttribute("testTypes", Arrays.asList(TestType.values()));

        return view.getName();
    }

    private static void logFailedJob(final BackgroundJob job, final Throwable t) {
        job.log("Failed:");
        Throwable cause = t;
        final StringBuilder level = new StringBuilder(10);
        while (cause != null) {
            job.log(level.toString() + cause.getMessage());
            cause = cause.getCause();
            level.append("-- ");
        }
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


    // @Nullable
    private static TestDefinition getTestDefinition(final ProctorStore store, final String testName) {
        try {
            return store.getCurrentTestDefinition(testName);
        } catch (StoreException e) {
            LOGGER.error("Failed to get current test definition for: " + testName, e);
            return null;
        }
    }

    // @Nullable
    private static TestDefinition getTestDefinition(final ProctorStore store, final String testName, final String revision) {
        try {
            return store.getTestDefinition(testName, revision);
        } catch (StoreException e) {
            LOGGER.error("Failed to get current test definition for: " + testName, e);
            return null;
        }
    }

    private static List<Revision> getTestHistory(final ProctorStore store, final String testName) {
        return getTestHistory(store, testName, Integer.MAX_VALUE);
    }

    // @Nonnull
    private static List<Revision> getTestHistory(final ProctorStore store, final String testName, final int limit) {
        final List<Revision> history;
        try {
            history = store.getHistory(testName, 0, limit);
            if (history.size() == 0) {
                LOGGER.info("No version history for [" + testName + "]");
            }
            return history;
        } catch (StoreException e) {
            LOGGER.error("Failed to get current test history for: " + testName, e);
            return null;
        }
    }

    private static class CheckMatrixResult {
        final boolean isValid;
        final List<String> errors;

        private CheckMatrixResult(boolean valid, List<String> errors) {
            isValid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}