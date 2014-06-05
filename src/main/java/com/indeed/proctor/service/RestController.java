package com.indeed.proctor.service;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.service.var.ContextVariable;
import com.indeed.proctor.service.var.Identifier;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Controller
public class RestController {

    private final AbstractProctorLoader loader;

    private final JsonServiceConfig jsonServiceConfig;
    private final Extractor extractor;
    private final Converter converter;

    public RestController() throws IOException
    {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath("/var/lucene/proctor/proctor-tests-matrix.json");
        factory.setSpecificationResource("/var/lucene/proctor/spec.json");
        loader = factory.getLoader();
        loader.load();

        final ObjectMapper mapper = new ObjectMapper();
        jsonServiceConfig = mapper.readValue(new File("/var/lucene/proctor/service-config.json"), JsonServiceConfig.class);

        // Populate context vars and identifiers for use by the extractor and converter.
        final List<ContextVariable> contextList = Lists.newArrayList();
        for (Map.Entry<String, JsonContextVarConfig> e : jsonServiceConfig.getContext().entrySet()) {
            contextList.add(new ContextVariable(e.getKey(), e.getValue()));
        }
        final List<Identifier> identifierList = Lists.newArrayList();
        for (Map.Entry<String, JsonVarConfig> e : jsonServiceConfig.getIdentifiers().entrySet()) {
            identifierList.add(new Identifier(e.getKey(), e.getValue()));
        }

        extractor = new Extractor(contextList, identifierList);
        converter = new Converter(contextList);
    }

    @RequestMapping(value="/groups/identify", method=RequestMethod.GET)
    public @ResponseBody JsonResponse<JsonResult> groupsIdentify(final HttpServletRequest request) {
        final Proctor proctor = tryLoadProctor();

        final RawParameters raw = extractor.extract(request);
        final ConvertedParameters param = converter.convert(raw);

        final ProctorResult result = proctor.determineTestGroups(
                param.getIdentifiers(), param.getContext(), param.getForceGroups());

        final JsonResult jsonResult = new JsonResult(
                result, param.getTest(), param.getContext(), loader.getLastAudit());
        return new JsonResponse<JsonResult>(jsonResult, new JsonMeta(HttpStatus.OK.value()));
    }

    /**
     * Returns the entire test matrix in JSON format.
     */
    @RequestMapping(value="/proctor/matrix", method=RequestMethod.GET)
    public @ResponseBody JsonResponse<Map<String, Object>> proctorMatrix() {
        return new JsonResponse<Map<String, Object>>(getJsonMatrix(), new JsonMeta(HttpStatus.OK.value()));
    }

    /**
     * Returns the audit of the test matrix in JSON format.
     */
    @RequestMapping(value="/proctor/matrix/audit", method=RequestMethod.GET)
    public @ResponseBody JsonResponse<Audit> proctorMatrixAudit() {
        return new JsonResponse<Audit>(loader.getLastAudit(), new JsonMeta(HttpStatus.OK.value()));
    }

    /**
     * Returns the test definition for a specific test in JSON format.
     */
    @RequestMapping(value="/proctor/matrix/definition/{testName}", method=RequestMethod.GET)
    public @ResponseBody JsonResponse<ConsumableTestDefinition> proctorMatrixAuditDefinition(
            @PathVariable String testName) {

        final Proctor proctor = tryLoadProctor();

        final ConsumableTestDefinition testDef = proctor.getTestDefinition(testName);
        if (testDef == null) {
            throw new NotFoundException(String.format("'%s' test definition not found in test matrix.", testName));
        }

        return new JsonResponse<ConsumableTestDefinition>(testDef, new JsonMeta(HttpStatus.OK.value()));
    }

    /**
     * Returns the configured identifiers from the service configuration file.
     */
    @RequestMapping(value="/config/identifiers")
    public @ResponseBody JsonResponse<Map<String, JsonVarConfig>> configIdentifiers() {
        return new JsonResponse<Map<String, JsonVarConfig>>(
                jsonServiceConfig.getIdentifiers(), new JsonMeta(HttpStatus.OK.value()));
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public JsonResponse handleBadRequestException(final BadRequestException e) {
        return new JsonEmptyDataResponse(new JsonMeta(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    public JsonResponse handleInternalServerException(final NotFoundException e) {
        return new JsonEmptyDataResponse(new JsonMeta(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    @ExceptionHandler(InternalServerException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonResponse handleInternalServerException(final InternalServerException e) {
        return new JsonEmptyDataResponse(new JsonMeta(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
    }

    /**
     * Try to load proctor. If that is impossible, throw an InternalServerException.
     */
    private Proctor tryLoadProctor() throws InternalServerException {
        final Proctor proctor = loader.get();
        if (proctor == null) {
            throw new InternalServerException(
                    String.format("Could not get Proctor from loader: %s", loader.getLastLoadErrorMessage()));
        }
        return proctor;
    }

    /**
     * Read the test matrix from Proctor and return it as a JSON simple Map data type.
     */
    private Map<String, Object> getJsonMatrix() {
        final Proctor proctor = tryLoadProctor();

        try {
            // Proctor only exposes the test matrix as a string written to a Writer.
            // As a workaround, we'll de-serialize the string and serialize it again.
            // It's possible to interpret that string as a raw JSON string if its in a POJO so that it isn't put into a
            // JSON string. But it's easier just to de-serialize it and let Spring serialize it again. This way, we
            // don't need to write another class, and we're sure the JSON we serve is valid and has no pretty formatting.
            final ObjectMapper mapper = new ObjectMapper();
            final StringWriter writer = new StringWriter();
            proctor.appendTestMatrix(writer);
            // Reading values into generic types requires this weird TypeReference construct. See Jackson docs on binding.
            return mapper.readValue(writer.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new InternalServerException(
                    String.format("Error while handling JSON test matrix: %s", e.getMessage()));
        }
    }
}
