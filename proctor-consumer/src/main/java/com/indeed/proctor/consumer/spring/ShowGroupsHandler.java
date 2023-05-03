package com.indeed.proctor.consumer.spring;

import com.indeed.proctor.consumer.AbstractGroups;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ShowGroupsHandler implements HttpRequestHandler {

    private GroupsSupplier groupsSupplier;

    public ShowGroupsHandler(GroupsSupplier groupSupplier) {
        this.groupsSupplier = groupSupplier;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");

        final PrintWriter writer = response.getWriter();

        AbstractGroups grps = groupsSupplier.determineGroups(request);
        if (grps == null) {
            writer.println("Did not determine any groups");
        } else {
            final StringBuilder sb = new StringBuilder();
            grps.appendTestGroups(sb, '\n');
            writer.print(sb.toString());
        }
    }

    public static interface GroupsSupplier {
        AbstractGroups determineGroups(HttpServletRequest request);
    }
}
