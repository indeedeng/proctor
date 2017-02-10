package com.indeed.proctor.webapp.controllers;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.store.ProctorStore;
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

    @Autowired
    public CleanWorkingDirectoryController(final WebappConfiguration configuration,
                                           @Qualifier("trunk") final ProctorStore trunkStore,
                                           @Qualifier("qa") final ProctorStore qaStore,
                                           @Qualifier("production") final ProctorStore productionStore,
                                           final BackgroundJobManager jobManager) {
        super(configuration, trunkStore, qaStore, productionStore);
        this.jobManager = jobManager;
    }

    @RequestMapping(value = "/rpc/svn/clean-working-directory", method = RequestMethod.POST)
    public View cleanWorkingDirectory(final HttpServletRequest request,
                                      @RequestParam(required = false) final String username) {
        final BackgroundJob<Boolean> job = createCleanWorkingDirectoryJob(username);
        jobManager.submit(job);

        if (isAJAXRequest(request)) {
            final JsonResponse<Map> response = new JsonResponse<Map>(BackgroundJobRpcController.buildJobJson(job), true, job.getTitle());
            return new JsonView(response);
        } else {
            // redirect to a status page for the job id
            return new RedirectView("/proctor/rpc/jobs/list?id=" + job.getId());
        }
    }

    private BackgroundJob<Boolean> createCleanWorkingDirectoryJob(final String username) {
        return new BackgroundJob<Boolean>() {
            @Override
            public String getTitle() {
                return String.format("Cleaning workspace for %s", username);
            }

            @Override
            public JobType getJobType() {
                return JobType.WORKING_DIRECTORY_CLEANING;
            }

            @Override
            protected Boolean execute() {
                boolean success = true;
                for(final Environment env : new Environment[] { Environment.WORKING, Environment.QA, Environment.PRODUCTION }) {
                    success &= cleanUserWorkspace(env, determineStoreFromEnvironment(env));
                }
                return success;
            }

            private boolean cleanUserWorkspace(final Environment environment,
                                               final ProctorStore store) {
                log(String.format("Cleaning %s workspace for user %s", environment.getName(), username));
                return store.cleanUserWorkspace(username);
            }
        };
    }
}
