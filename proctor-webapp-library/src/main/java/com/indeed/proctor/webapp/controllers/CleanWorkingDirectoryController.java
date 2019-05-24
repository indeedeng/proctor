package com.indeed.proctor.webapp.controllers;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.jobs.BackgroundJob;
import com.indeed.proctor.webapp.jobs.BackgroundJobFactory;
import com.indeed.proctor.webapp.jobs.BackgroundJobManager;
import com.indeed.proctor.webapp.model.BackgroundJobResponseModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * @author parker
 */
@RequestMapping({"/", "/proctor"})
@Controller
public class CleanWorkingDirectoryController extends AbstractController {

    private final BackgroundJobManager jobManager;
    private final BackgroundJobFactory jobFactory;

    @Autowired
    public CleanWorkingDirectoryController(final WebappConfiguration configuration,
                                           @Qualifier("trunk") final ProctorStore trunkStore,
                                           @Qualifier("qa") final ProctorStore qaStore,
                                           @Qualifier("production") final ProctorStore productionStore,
                                           final BackgroundJobManager jobManager,
                                           final BackgroundJobFactory jobFactory) {
        super(configuration, trunkStore, qaStore, productionStore);
        this.jobManager = jobManager;
        this.jobFactory = jobFactory;
    }

    @RequestMapping(value = "/rpc/svn/clean-working-directory", method = RequestMethod.POST)
    public View cleanWorkingDirectory(final HttpServletRequest request,
                                      @RequestParam(required = false) final String username) {
        final BackgroundJob<Boolean> job = createCleanWorkingDirectoryJob(username);
        jobManager.submit(job);

        if (isAJAXRequest(request)) {
            final JsonResponse<BackgroundJobResponseModel> response =
                    new JsonResponse<BackgroundJobResponseModel>(new BackgroundJobResponseModel(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }
    }

    private BackgroundJob<Boolean> createCleanWorkingDirectoryJob(final String username) {
        return jobFactory.createBackgroundJob(
                String.format("Cleaning workspace for %s", username),
                username,
                BackgroundJob.JobType.WORKING_DIRECTORY_CLEANING,
                new BackgroundJobFactory.Executor<Boolean>() {
                    @Override
                    public Boolean execute(final BackgroundJob job) {
                        boolean success = true;
                        for (final Environment env : new Environment[] { Environment.WORKING, Environment.QA, Environment.PRODUCTION }) {
                            success &= cleanUserWorkspace(env, determineStoreFromEnvironment(env), job);
                        }
                        return success;
                    }
                    private boolean cleanUserWorkspace(final Environment environment,
                                                       final ProctorStore store,
                                                       final BackgroundJob job) {
                        job.log(String.format("Cleaning %s workspace for user %s", environment.getName(), username));
                        return store.cleanUserWorkspace(username);
                    }
                }
        );
    }
}
