package com.indeed.proctor.consumer.spring;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.Proctor;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

public class ShowTestMatrixHandler implements HttpRequestHandler{
    private final List<Supplier<Proctor>> proctorSuppliers;

    public ShowTestMatrixHandler(final Supplier<Proctor> proctorSupplier) {
        this.proctorSuppliers = ImmutableList.<Supplier<Proctor>>of(proctorSupplier);
    }

    public ShowTestMatrixHandler(final List<Supplier<Proctor>> proctorSuppliers) {
        this.proctorSuppliers = ImmutableList.copyOf(proctorSuppliers);
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");

        // Only output matching test names if not null.
        final Collection<String> testNameFilter = ShowHandlerParamUtil.getTestQueryParameters(request);

        final PrintWriter writer = response.getWriter();

        if(proctorSuppliers.isEmpty()) {
            writer.print("No Proctor instances found.");
            return;
        }

        writer.print("[");
        boolean firstTime = true;

        for(final Supplier<Proctor> proctorSupplier : proctorSuppliers) {
            if(firstTime) {
                firstTime = false;
            } else {
                writer.print(",");
            }
            final Proctor proctor = proctorSupplier.get();
            if (proctor == null) {
                writer.println("Did not determine a Proctor instance");
            } else if (testNameFilter != null) {
                proctor.appendTestMatrixFiltered(writer, testNameFilter);
            } else {
                proctor.appendTestMatrix(writer);
            }
        }

        writer.print("]");
    }
}
