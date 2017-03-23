package com.indeed.proctor.consumer.spring;

import com.google.common.base.Supplier;
import com.indeed.proctor.common.Proctor;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * To get basic showGroups functionality mount this controller and implement determineGroups(HttpServletRequest request).
 * All request handlers are guarded by PrivilegedIPs.isPrivileged
 *
 * eg @RequestMapping(value = "/private", method = RequestMethod.GET)
 * Will get the following routes:
 *  private/showGroups
 *  private/showRandomGroups
 *  private/showTestMatrix
 *
 * You can force yourself into a group at any point using prforceGroups=xx
 * http://www.indeed.com/private/showGroups?prforceGroups=btnuitst3,testx0
 *
 * @author parker
 */
public abstract class AbstractShowTestGroupsController implements ShowGroupsHandler.GroupsSupplier {

    private final ShowGroupsHandler showGroupsHandler;
    private final ShowRandomGroupsHandler randomGroupsHandler;
    private final ShowTestMatrixHandler showTestMatrixHandler;

    public AbstractShowTestGroupsController(final Supplier<Proctor> proctorSupplier) {
        this.showGroupsHandler = new ShowGroupsHandler(this);
        this.randomGroupsHandler = new ShowRandomGroupsHandler(proctorSupplier);
        this.showTestMatrixHandler = new ShowTestMatrixHandler(proctorSupplier);
    }

    public AbstractShowTestGroupsController(final List<Supplier<Proctor>> proctorSuppliers) {
        this.showGroupsHandler = new ShowGroupsHandler(this);
        this.randomGroupsHandler = new ShowRandomGroupsHandler(proctorSuppliers);
        this.showTestMatrixHandler = new ShowTestMatrixHandler(proctorSuppliers);
    }

    @RequestMapping(value = "/showGroups")
    public void showGroups(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        handle(request, response, showGroupsHandler);
    }

    @RequestMapping(value = "/showRandomGroups")
    public void showRandomGroups(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        handle(request, response, randomGroupsHandler);
    }

    @RequestMapping(value = "/showTestMatrix")
    public void showTestMatrix(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        handle(request, response, showTestMatrixHandler);
    }

    private void handle(final HttpServletRequest request, final HttpServletResponse response, final HttpRequestHandler handler) throws IOException, ServletException {
        if(isAccessAllowed(request)) {
            handler.handleRequest(request, response);
        } else {
            response.sendError(getAccessDeniedStatusCode());
        }
    }

    /**
     * Override this if the show groups controller should only be shown to specific types of requests.
     * eg. restricted by IP
     * @param request request
     * @return trun if access allowed
     */
    public abstract boolean isAccessAllowed(final HttpServletRequest request);

    protected int getAccessDeniedStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }

}
