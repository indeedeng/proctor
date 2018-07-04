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
<%--@elvariable id="testSpecificationJson" type="java.lang.String"--%>
<%--@elvariable id="testDefinitionVersion" type="com.indeed.proctor.store.Revision"--%>
<%--@elvariable id="testDefinitionHistory" type="java.util.List<com.indeed.proctor.webapp.model.RevisionDefinition>"--%>
<%--@elvariable id="branch" type="com.indeed.proctor.webapp.db.Environment"--%>
<%--@elvariable id="version" type="com.indeed.proctor.common.EnvironmentVersion"--%>
<%--@elvariable id="requireAuth" type="java.lang.Boolean"--%>
<%--@elvariable id="devApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="qaApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="productionApplications" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="devDynamicClients" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="qaDynamicClients" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>
<%--@elvariable id="productionDynamicClients" type="java.util.Set<com.indeed.proctor.webapp.model.AppVersion>"--%>

<layout:base title="${testName} | Proctor" session="${session}" >

    <c:choose>
        <c:when test = "${!fn:startsWith(testName,  'Test \"')}">
            <div>
                <h2><span class="mrm">${fn:escapeXml(testName)}</span> (<a href="/proctor/definition/${proctor:urlencode(testName)}/edit">edit</a>)</h2>
                <c:if test="${!empty testDefinition.description}"><h3 class="subheader">${fn:escapeXml(testDefinition.description)}</h3></c:if>
            </div>
            <div class="js-tabs-container">
                <dl class="tabs contained">
                  <dd class="active"><a href="#tab-details">Details</a></dd>
                  <dd><a href="#tab-history">History</a></dd>
                  <dd><a href="#tab-definition">Definition</a></dd>
                  <dd><a href="#tab-spec">Specification</a></dd>
                  <dd><a href="#tab-usage">Usage</a></dd>
                  <dd><a href="#tab-delete">Delete</a></dd>
                </dl>

                <ul class="tabs-content contained">
                  <li class="active" id="tab-details">
                      <ui:definition-view testName="${testName}" definition="${testDefinition}"/>
                  </li>
                    <li id="tab-history">
                      <ui:definition-history branch="${branch}"
                                             testName="${testName}"
                                             testDefinitionHistory="${testDefinitionHistory}"
                                             version="${version}"
                                             requireAuth="${requireAuth}"
                      />
                  </li>
                  <li id="tab-definition">
                      <!-- TODO use prettified JSON library for code display -->
                      <pre class="prettify code json"><c:out value="${testDefinitionJson}" /></pre>
                  </li>
                  <li id="tab-spec">
                      <!-- TODO use prettified JSON library for code display -->
                      <pre class="prettify code json"><c:out value="${testSpecificationJson}" /></pre>
                  </li>
                  <li id="tab-usage">
                    <ui:definition-usage testName="${testName}"
                                         devApplications="${devApplications}"
                                         qaApplications="${qaApplications}"
                                         productionApplications="${productionApplications}"
                                         devDynamicClients="${devDynamicClients}"
                                         qaDynamicClients="${qaDynamicClients}"
                                         productionDynamicClients="${productionDynamicClients}"
                    />
                  </li>
                    <li id="tab-delete">
                        <ui:definition-delete-form testName="${testName}"
                                                   src="${branch.name}"
                                                   srcRevision="${testDefinitionVersion.revision}"
                                                   requireAuth="${requireAuth}"
                        />
                    </li>
                </ul>
            </div>
        </c:when>
        <c:otherwise>
            <div>
                <h2><span class="mrm">${fn:escapeXml(testName)}</span></h2>
            </div>
        </c:otherwise>
    </c:choose>

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
