package com.indeed.proctor.pipet.core.web;

import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.pipet.core.config.JsonPipetConfig;
import com.indeed.proctor.pipet.core.var.VariableConfiguration;
import com.indeed.proctor.pipet.core.var.ConvertedParameters;
import com.indeed.proctor.pipet.core.var.Converter;
import com.indeed.proctor.pipet.core.var.Extractor;
import com.indeed.proctor.pipet.core.var.RawParameters;
import com.indeed.proctor.pipet.core.config.JsonContextVarConfig;
import com.indeed.proctor.pipet.core.config.JsonVarConfig;
import com.indeed.proctor.pipet.core.model.JsonEmptyDataResponse;
import com.indeed.proctor.pipet.core.model.JsonMeta;
import com.indeed.proctor.pipet.core.model.JsonResponse;
import com.indeed.proctor.pipet.core.model.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

@Controller
public class RestController {
    private final AbstractProctorLoader loader;

    private final JsonPipetConfig jsonPipetConfig;
    private final Extractor extractor;
    private final Converter converter;

    @Autowired
    public RestController(final VariableConfiguration configuration,
                          final AbstractProctorLoader loader) {

        this.jsonPipetConfig = configuration.getJsonConfig();
        this.extractor = configuration.getExtractor();
        this.converter = configuration.getConverter();
        this.loader = loader;
    }

    @RequestMapping(value="/groups/identify", method=RequestMethod.GET)
    public JsonResponseView groupsIdentify(final HttpServletRequest request, final Model model) {
        final Proctor proctor = tryLoadProctor();

        final RawParameters raw = extractor.extract(request);
        final ConvertedParameters param = converter.convert(raw);

        final ProctorResult result;
        if (param.getTest() == null) {
            // Get all existing tests.
            // This can log many errors if not all context variables in the test matrix were provided.
            result = proctor.determineTestGroups(
                    param.getIdentifiers(), param.getContext(), param.getForceGroups(), Collections.<String>emptyList());
        } else if (param.getTest().isEmpty()) {
            // Get zero tests.
            result = ProctorResult.EMPTY;
        } else {
            // Get tests specified in parameter.
            result = proctor.determineTestGroups(
                    param.getIdentifiers(), param.getContext(), param.getForceGroups(), param.getTest());
        }

        final JsonResult jsonResult = new JsonResult(
                result, param.getContext(), loader.getLastAudit());
        model.addAttribute(new JsonResponse<JsonResult>(jsonResult, new JsonMeta(HttpStatus.OK.value())));
        return new JsonResponseView();
    }

    /**
     * Returns the entire test matrix in JSON format.
     */
    @RequestMapping(value="/proctor/matrix", method=RequestMethod.GET)
    public void proctorMatrix(final HttpServletResponse response) throws IOException {
        final Proctor proctor = tryLoadProctor();
        final PrintWriter writer = response.getWriter();
        response.setContentType("application/json;charset=UTF-8");
        proctor.appendTestMatrix(writer);
    }

    /**
     * Returns the audit of the test matrix in JSON format.
     */
    @RequestMapping(value="/proctor/matrix/audit", method=RequestMethod.GET)
    public JsonResponseView proctorMatrixAudit(final Model model) {
        model.addAttribute(new JsonResponse<Audit>(loader.getLastAudit(), new JsonMeta(HttpStatus.OK.value())));
        return new JsonResponseView();
    }

    /**
     * Returns the test definition for a specific test in JSON format.
     */
    @RequestMapping(value="/proctor/matrix/definition/{testName}", method=RequestMethod.GET)
    public JsonResponseView proctorMatrixDefinition(
            final Model model,
            @PathVariable String testName) {

        final Proctor proctor = tryLoadProctor();

        final ConsumableTestDefinition testDef = proctor.getTestDefinition(testName);
        if (testDef == null) {
            throw new NotFoundException(String.format("'%s' test definition not found in test matrix.", testName));
        }

        model.addAttribute(new JsonResponse<ConsumableTestDefinition>(testDef, new JsonMeta(HttpStatus.OK.value())));
        return new JsonResponseView();
    }

    /**
     * Returns the configured context variable parsers from the pipet configuration file.
     */
    @RequestMapping(value="/config/context")
    public JsonResponseView configContext(final Model model) {
        model.addAttribute(new JsonResponse<Map<String, JsonContextVarConfig>>(
                jsonPipetConfig.getContext(), new JsonMeta(HttpStatus.OK.value())));
        return new JsonResponseView();
    }

    /**
     * Returns the configured identifiers from the pipet configuration file.
     */
    @RequestMapping(value="/config/identifiers")
    public JsonResponseView configIdentifiers(final Model model) {
        model.addAttribute(new JsonResponse<Map<String, JsonVarConfig>>(
                jsonPipetConfig.getIdentifiers(), new JsonMeta(HttpStatus.OK.value())));
        return new JsonResponseView();
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequestException(final BadRequestException e) {
        ModelAndView mav = new ModelAndView(new JsonResponseView());
        mav.addObject(new JsonEmptyDataResponse(new JsonMeta(HttpStatus.BAD_REQUEST.value(), e.getMessage())));
        return mav;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFoundException(final NotFoundException e) {
        ModelAndView mav = new ModelAndView(new JsonResponseView());
        mav.addObject(new JsonEmptyDataResponse(new JsonMeta(HttpStatus.NOT_FOUND.value(), e.getMessage())));
        return mav;
    }

    @ExceptionHandler(InternalServerException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleInternalServerException(final InternalServerException e) {
        ModelAndView mav = new ModelAndView(new JsonResponseView());
        mav.addObject(new JsonEmptyDataResponse(new JsonMeta(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage())));
        return mav;
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
}
