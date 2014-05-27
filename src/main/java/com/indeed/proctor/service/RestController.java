package com.indeed.proctor.service;

import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.common.Proctor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class RestController {

    private final AbstractProctorLoader loader;

    public RestController()
    {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath("/var/lucene/proctor/proctor-tests-matrix.json");
        factory.setSpecificationResource("/var/lucene/proctor/spec.json");
        loader = factory.getLoader();
        loader.load();
    }

    @RequestMapping(value="/groups/identify", method= RequestMethod.GET)
    public @ResponseBody String groupsIdentify() {
        return "Hello, world!";
    }
}
