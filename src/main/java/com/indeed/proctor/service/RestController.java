package com.indeed.proctor.service;

import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collections;
import java.util.Map;

@Controller
public class RestController {

    private final AbstractProctorLoader loader;

    private final ServiceConfig serviceConfig;
    private final Extractor extractor;
    private final Converter converter;

    public RestController() throws Exception
    {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath("/var/lucene/proctor/proctor-tests-matrix.json");
        factory.setSpecificationResource("/var/lucene/proctor/spec.json");
        loader = factory.getLoader();
        loader.load();

        final ObjectMapper mapper = new ObjectMapper();
        serviceConfig = mapper.readValue(new File("/var/lucene/proctor/service-config.json"), ServiceConfig.class);
        serviceConfig.correctDefaultSourceKeys();
        extractor = new Extractor(serviceConfig);
        converter = new Converter(serviceConfig);
    }

    @RequestMapping(value="/groups/identify", method= RequestMethod.GET)
    public @ResponseBody JsonResult groupsIdentify(final HttpServletRequest request) {

        final Proctor proctor = loader.get();

        final RawParameters raw = extractor.extract(request);
        final ConvertedParameters param = converter.convert(raw);

        final ProctorResult result = proctor.determineTestGroups(
                param.getIdentifiers(), param.getContext(), Collections.<String, Integer>emptyMap());

        return new JsonResult(result, param.getTest(), param.getContext());
    }
}
