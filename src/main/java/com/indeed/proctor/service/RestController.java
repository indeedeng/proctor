package com.indeed.proctor.service;

import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
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
    public @ResponseBody JsonResult groupsIdentify(
            @RequestParam Map<String, String> queryParams) {

        final Proctor proctor = loader.get();

        final RawQueryParameters raw = new RawQueryParameters(queryParams);
        final ConvertedIdentifyParameters param = new ConvertedIdentifyParameters(raw);

        final ProctorResult result = proctor.determineTestGroups(
                param.getIdentifiers(), param.getContext(), Collections.<String, Integer>emptyMap());

        return new JsonResult(result, param.getTest(), param.getContext(), new Audit());
    }
}
