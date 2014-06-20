<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
<%--@elvariable id="testDefinitionHistory" type="java.util.List<com.indeed.proctor.store.Revision>"--%>
<%--@elvariable id="branch" type="com.indeed.proctor.webapp.db.Environment"--%>
<%--@elvariable id="version" type="com.indeed.proctor.common.EnvironmentVersion"--%>
<%--@elvariable id="devApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="qaApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="productionApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>

<layout:base title="${testName} | Proctor" session="${session}" >

    <div>
        <h2><span class="mrm">${fn:escapeXml(testName)}</span> (<a href="/proctor/definition/${proctor:urlencode(testName)}/edit">edit</a>)</h2>
        <c:if test="${!empty testDefinition.description}"><h3 class="subheader">${fn:escapeXml(testDefinition.description)}</h3></c:if>
    </div>
    <div class="js-tabs-container">
        <dl class="tabs contained">
          <dd class="active"><a href="#tab-details">Details</a></dd>
          <dd><a href="#tab-history">History</a></dd>
          <dd><a href="#tab-json">JSON</a></dd>
          <dd><a href="#tab-usage">Usage</a></dd>
          <dd><a href="#tab-delete">Delete</a></dd>
        </dl>

        <ul class="tabs-content contained">
          <li class="active" id="tab-details">
              <ui:definition-view testName="${testName}" definition="${testDefinition}"/>
          </li>
            <li id="tab-history">
              <ui:definition-history branch="${branch}" testName="${testName}" testDefinitionHistory="${testDefinitionHistory}" version="${version}"/>
          </li>
          <li id="tab-json">
              <!-- TODO use prettified JSON library for code display -->
              <pre class="prettify code json"><c:out value="${testDefinitionJson}" /></pre>
          </li>
          <li id="tab-usage">
            <ui:definition-usage testName="${testName}" devApplications="${devApplications}" qaApplications="${qaApplications}" productionApplications="${productionApplications}"/>
          </li>
            <li id="tab-delete">
                <ui:definition-delete-form testName="${testName}"
                                            src="${branch.name}"
                                            srcRevision="${testDefinitionVersion.revision}"
                />
            </li>
        </ul>
    </div>

    <layout:javascript
            useCompiledJavascript="${session.useCompiledJavaScript}"
            compiledJavascriptSrc="/static/scripts/app/details-compiled.js"
            nonCompiledJavascriptSrc="/static/scripts/app/details.js"/>
    <script type="text/javascript">
        //<![CDATA[
        indeed.proctor.app.details.start();
        //]]>
    </script>

</layout:base>
