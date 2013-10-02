package com.indeed.proctor.consumer.spring;

import com.google.common.base.Supplier;
import com.indeed.proctor.common.Proctor;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ShowTestMatrixHandler implements HttpRequestHandler{

    private Supplier<Proctor> proctorSupplier;

    public ShowTestMatrixHandler(Supplier<Proctor> proctorSupplier) {
        this.proctorSupplier = proctorSupplier;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");

        final PrintWriter writer = response.getWriter();

        Proctor proctor = proctorSupplier.get();
        if(proctor == null) {
            writer.println("Did not determine a Proctor instance");
        } else {
            proctor.appendTestMatrix(writer);
        }
    }
}
