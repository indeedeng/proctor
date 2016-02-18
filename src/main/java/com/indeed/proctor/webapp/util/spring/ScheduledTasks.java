package com.indeed.proctor.webapp.util.spring;

import com.indeed.util.varexport.VarExporter;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.webapp.RemoteProctorSpecificationSource;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author parker
 * This class is here to get around a circular dependency in the applicationContext.xml:
 *
 * Specifically, the SvnProctorStoreFactory requires a ScheduledExecutorService to intialize its SvnProctorStores.
 *
 * And we'd like to schedule the ProctorPromoter (but that needs the ProctorStore to be initialized).
 *
 */
@Component
public class ScheduledTasks implements InitializingBean {

    private final ScheduledExecutorService scheduledExecutorService;
    private final ProctorPromoter promoter;
    private final RemoteProctorSpecificationSource proctorSpecificationSource;

    @Autowired
    public ScheduledTasks(final ScheduledExecutorService scheduledExecutorService,
                          final ProctorPromoter promoter,
                          final RemoteProctorSpecificationSource proctorSpecificationSource) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.promoter = promoter;
        this.proctorSpecificationSource = proctorSpecificationSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduledExecutorService.scheduleWithFixedDelay(promoter, 60, 5, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleWithFixedDelay(proctorSpecificationSource, 1, 10, TimeUnit.MINUTES);

        if(scheduledExecutorService instanceof ThreadPoolExecutor) {
            VarExporter.forNamespace(getClass().getSimpleName()).export(new ThreadPoolExecutorVarExports((ThreadPoolExecutor) scheduledExecutorService), "pool-");
        }
    }
}
