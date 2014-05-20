package com.indeed.proctor.consumer.spring;

import com.google.common.base.Supplier;
import com.indeed.proctor.common.Proctor;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ShowRandomGroupsHandler implements HttpRequestHandler{

    private Supplier<Proctor> proctorSupplier;

    public ShowRandomGroupsHandler(Supplier<Proctor> proctorSupplier) {
        this.proctorSupplier = proctorSupplier;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");

        // Only output matching test names if not null.
        final Collection<String> testNameFilter = ShowHandlerParamUtil.getTestQueryParameters(request);

        final PrintWriter writer = response.getWriter();

        final Proctor proctor = proctorSupplier.get();
        if(proctor == null) {
            writer.println("Did not determine a Proctor instance");
        } else if (testNameFilter != null) {
            proctor.appendTestsNameFiltered(writer, testNameFilter);
        } else {
            proctor.appendAllTests(writer);
        }
    }
}
