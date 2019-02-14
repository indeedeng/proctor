package com.indeed.proctor.webapp;

import com.indeed.proctor.store.ProctorStore;

import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class WebappUtils {

    public static ProctorStore getProctorStore(final ServletContext sc) {
        final WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        return wac.getBean("proctorStore", ProctorStore.class);
    }

}
