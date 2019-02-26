<%@ page import="com.indeed.proctor.webapp.extensions.renderer.EditPageRenderer" %>
<%@ page import="com.indeed.proctor.webapp.tags.RenderHelpButtonTagHandler" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags/ui" %>
<%@ taglib prefix="layout" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="proctor" uri="http://tags.indeed.com/proctor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="session" type="com.indeed.proctor.webapp.model.SessionViewModel"--%>
<%--@elvariable id="testName" type="java.lang.String"--%>
<%--@elvariable id="testDefinition" type="com.indeed.proctor.common.model.TestDefinition"--%>
<%--@elvariable id="testDefinitionJson" type="java.lang.String"--%>
<%--@elvariable id="testDefinitionVersion" type="com.indeed.proctor.store.Revision"--%>
<%--@elvariable id="testDefinitionHistory" type="java.util.List<com.indeed.proctor.webapp.model.RevisionDefinition>"--%>
<%--@elvariable id="testTypes" type="java.util.List<com.indeed.proctor.common.model.TestType>"--%>
<%--@elvariable id="isCreate" type="java.lang.Boolean"--%>
<%--@elvariable id="branch" type="com.indeed.proctor.webapp.db.Environment"--%>
<%--@elvariable id="version" type="com.indeed.proctor.common.EnvironmentVersion"--%>
<%--@elvariable id="requireAuth" type="java.lang.Boolean"--%>
<%--@elvariable id="devApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="qaApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="productionApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="specialConstants" type="java.util.Map<String, Object>"--%>


<layout:base title="${testName} | Proctor" session="${session}">

    <c:choose>
        <c:when test="${isCreate}">
            <h2>Create a new test</h2>
        </c:when>
        <c:otherwise>
            <div>
                <h2><span class="mrm">${fn:escapeXml(testName)}</span> (<a href="/proctor/definition/${proctor:urlencode(testName)}?branch=${branch.name}">view</a>)</h2>
                <c:if test="${!empty testDefinition.description}"><h3 class="subheader">${fn:escapeXml(testDefinition.description)}</h3></c:if>
            </div>
        </c:otherwise>
    </c:choose>

<c:set var="action" value="/proctor/definition/${proctor:urlencode(testName)}/edit"/>
<c:if test="${isCreate}">
<c:set var="action" value="/proctor/definition/${proctor:urlencode('{testName}')}/edit"/>
</c:if>
<form class="js-edit-definition-form" action="${action}" method="POST">

    <proctor:renderEditPageInjectionTemplates position="<%=EditPageRenderer.EditPagePosition.TOP_FORM%>" testName="${testName}" testDefinitionJson="${testDefinitionJson}" isCreate="${isCreate}"/>

    <ui:grid-row >
        <ui:grid-columns width="two"><h4>Basic</h4></ui:grid-columns>
        <ui:grid-columns width="ten">
            <div class="js-basic-editor panel">
                <c:if test="${isCreate}">
                    <ui:grid-row>
                        <ui:grid-columns width="two">
                            <label class="right inline">Test Name</label>
                        </ui:grid-columns>
                        <ui:grid-columns width="ten">
                            <input name="testName" type="text" placeholder="test Name" value=""/>
                        </ui:grid-columns>
                    </ui:grid-row>
                </c:if>

                <ui:grid-row>
                    <ui:grid-columns width="two">
                        <label class="right inline">Description</label>
                    </ui:grid-columns>
                    <ui:grid-columns width="ten">
                        <input class="json" name="description" type="text" placeholder="e.g. [ISSUE] - Description"
                               value="${fn:escapeXml(testDefinition.description)}"/>
                    </ui:grid-columns>
                </ui:grid-row>
                <ui:grid-row>
                    <ui:grid-columns width="two">
                        <label class="right inline">Test Type</label>
                    </ui:grid-columns>
                    <c:if test="${!isCreate}">
                        <ui:grid-columns width="eight">
                            <span class="inline">
                              <select class="json" name="testType" class="two" disabled="disabled">
                                  <c:forEach items="${testTypes}" var="testType">
                                  <option value="${fn:escapeXml(testType)}" <c:if test="${testType == testDefinition.testType}">selected="selected"</c:if> >${fn:escapeXml(testType)}</option>
                                  </c:forEach>
                              </select>
                            </span>
                        </ui:grid-columns>
                        <ui:grid-columns width="one">
                            <div class="tiny button alert radius" onclick="enableTestTypeField();">Enable</div>
                        </ui:grid-columns>
                    </c:if>
                    <c:if test="${isCreate}">
                        <ui:grid-columns width="nine">
                            <span class="inline">
                              <select class="json" name="testType" class="two">
                                  <c:forEach items="${testTypes}" var="testType">
                                      <option value="${fn:escapeXml(testType)}" <c:if test="${testType == testDefinition.testType}">selected="selected"</c:if> >${fn:escapeXml(testType)}</option>
                                  </c:forEach>
                              </select>
                            </span>
                        </ui:grid-columns>
                    </c:if>
                    <ui:grid-columns width="one">
                        <div class="text-center">
                            <span class="inline"><proctor:renderHelpButton helpType="<%=RenderHelpButtonTagHandler.HelpType.TEST_TYPE%>" /></span>
                        </div>
                    </ui:grid-columns>
                </ui:grid-row>
                <ui:grid-row>
                    <ui:grid-columns width="two">
                        <label class="right inline">Salt</label>
                    </ui:grid-columns>
                    <ui:grid-columns width="ten">
                        <input class="json" name="salt" type="text" value="${fn:escapeXml(testDefinition.salt)}"/>
                    </ui:grid-columns>
                </ui:grid-row>
                <ui:grid-row>
                    <ui:grid-columns width="two">
                        <label class="right inline">Rule</label>
                    </ui:grid-columns>
                    <ui:grid-columns width="nine">
                        <div class="row collapse">
                            <c:set var="pfix" value="\${"/>
                            <div class="one columns"><span class="prefix">${pfix}</span></div>
                            <div class="ten columns"><input class="json" name="rule" type="text" value="${fn:escapeXml(testDefinition.rule)}"/></div>
                            <div class="one columns"><span class="postfix">}</span></div>
                        </div>
                    </ui:grid-columns>
                    <ui:grid-columns width="one">
                        <div class="text-center">
                            <span class="inline"><proctor:renderHelpButton helpType="<%=RenderHelpButtonTagHandler.HelpType.RULE%>" /></span>
                        </div>
                    </ui:grid-columns>
                </ui:grid-row>
                <ui:grid-row>
                    <ui:grid-columns width="two">
                        <label class="right inline">Silent</label>
                    </ui:grid-columns>
                    <ui:grid-columns width="ten">
                        <span class="inline">
                            <label>
                                <input class="json" name="silent" type="checkbox" value="true"
                                       <c:if test="${testDefinition.silent == 'true'}">checked="checked"</c:if>>
                                <c:choose>
                                    <c:when test="${isCreate}">Do not log this test</c:when>
                                    <c:otherwise>Stop logging this test</c:otherwise>
                                </c:choose>
                            </label>
                        </span>
                    </ui:grid-columns>
                </ui:grid-row>
            </div>
        </ui:grid-columns>
    </ui:grid-row>
    <div class="row">
        <div class="two columns"><h4>Constants</h4></div>
        <div class="ten columns">
            <div class="panel">
                <ui:constants-edit constants="${testDefinition.constants}" inputPath="constants"/>
            </div>
        </div>
    </div>
    <div class="row specialConstantsRow">
        <div class="two columns"><h4>Special Constants</h4></div>
        <div class="ten columns">
            <div class="panel specialConstantsPanel">
             <proctor:renderEditPageInjectionTemplates position="<%=EditPageRenderer.EditPagePosition.SPECIAL_CONSTANTS%>" testName="${testName}" testDefinitionJson="${testDefinitionJson}" isCreate="${isCreate}"/>
            </div>
        </div>
    </div>

    <div class="row">
      <div class="two columns"><h4>Buckets</h4></div>
      <div class="ten columns">
          <ui:buckets-edit definition="${testDefinition}" />
      </div>
    </div>
  <div class="row">
      <div class="two columns">
          <h4>Allocations</h4>
          <ui:expand-collapse more="show legend" less="hide" isMoreExpanded="false">
              <ui:bucket-index definition="${testDefinition}" />
          </ui:expand-collapse>
      </div>
      <div class="ten columns">
          <div class="js-allocations-editor">
          <c:forEach items="${testDefinition.allocations}" var="allocation" varStatus="status">
            <ui:allocation-edit definition="${testDefinition}" allocation="${allocation}" allocationIndex="${status.index}"/>
          </c:forEach>
          </div>
      </div>
    </div>
    <ui:grid-row>
        <ui:grid-columns width="two"></ui:grid-columns>
        <ui:grid-columns width="ten">
            <div class="panel js-save-info">
                <div id="silent-warning" <c:if test="${testDefinition.silent == 'false'}">style="display:none;"</c:if>>Logging is currently disabled for this test. If you want to enable logging, please uncheck the 'Silent' checkbox at the top of the page.</div>
                <c:if test="${requireAuth}">
                    <ui:grid-row>
                        <ui:grid-columns width="three"><label class="right inline">SCM</label></ui:grid-columns>
                        <ui:grid-columns width="four"><input placeholder="Username" type="text" name="username" /></ui:grid-columns>
                        <ui:grid-columns width="five"><input placeholder="Password" type="password" name="password" /></ui:grid-columns>
                    </ui:grid-row>
                </c:if>
                <ui:grid-row>
                    <ui:grid-columns width="three"><label class="right inline">Comment</label></ui:grid-columns>
                    <ui:grid-columns width="nine"><input placeholder="(optional) description of change" type="text" name="comment" /></ui:grid-columns>
                </ui:grid-row>
                <proctor:renderEditPageInjectionTemplates position="<%=EditPageRenderer.EditPagePosition.BOTTOM_FORM%>" testName="${testName}" testDefinitionJson="${testDefinitionJson}" isCreate="${isCreate}"/>
                <div class="ui-form-buttons">
                    <input type="submit" class="button js-save-form" value="Save">
                    <span class="button tiny secondary js-clean-workspace">clean workspace</span>
                    <div style="display:none;" class="mam save-msg-container alert-box"></div>
                    <label for="autopromote-checkbox">
                        <input id="autopromote-checkbox" type="checkbox" class="mrs mts" value="autopromote">Enable Autopromote
                    </label>
                </div>
            </div>
        </ui:grid-columns>
    </ui:grid-row>
</form>

    <proctor:renderEditPageInjectionTemplates position="<%=EditPageRenderer.EditPagePosition.SCRIPT%>" testName="${testName}" testDefinitionJson="${testDefinitionJson}" isCreate="${isCreate}"/>

    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/editor-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/editor.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.editor.start(
                '${testName}',
                ${testDefinitionJson},
                "${version.trunkRevision}",
                ${isCreate}
        );

        function enableTestTypeField()
        {
            var el = document.getElementsByName("testType")[0];
            if (el.disabled) {
                el.disabled = false;
                var errormessage = 'You should only be correcting the test type, not changing it.';
                var parent = el.parentNode;
                var small = document.createElement('small');
                small.setAttribute('class', 'error');
                small.innerHTML = errormessage;
                parent.appendChild(small);
            }
        }


        //Hide Special Constants if there are none
        var specialConstantsPanel = document.getElementsByClassName('specialConstantsPanel')[0];
        var specialConstantsRow = document.getElementsByClassName('specialConstantsRow')[0];
        if (specialConstantsPanel.innerHTML.trim() == "") {
            document.getElementsByClassName('specialConstantsRow')[0].style.display='none';
        }
        //]]>
    </script>

</layout:base>
