<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SAP BO Administration Utilities</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell">
    <section class="panel login-panel">
        <h1>SAP BO Administration Utilities</h1>
        <p>CMS: <%= HtmlEscaper.escape(session.getAttribute("cms")) %></p>
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div>
        <% } %>
        <div class="module-actions">
            <a class="button loading-trigger" data-loading-message="Building WebI report inventory..."
               href="${pageContext.request.contextPath}/reports">WebI Report Inventory</a>
            <a class="button loading-trigger" data-loading-message="Building universe inventory..."
               href="${pageContext.request.contextPath}/universes">Universe Inventory</a>
            <a class="button loading-trigger" data-loading-message="Building users and groups inventory..."
               href="${pageContext.request.contextPath}/users-groups">Users and Groups Inventory</a>
            <a class="button loading-trigger" data-loading-message="Building connection inventory..."
               href="${pageContext.request.contextPath}/connections">Connection Inventory</a>
            <a class="button loading-trigger" data-loading-message="Finding AD users without Enterprise aliases..."
               href="${pageContext.request.contextPath}/enterprise-aliases">Enterprise Alias Creator</a>
            <a class="button loading-trigger" data-loading-message="Opening report property update tool..."
               href="${pageContext.request.contextPath}/report-property-updates">Report Property Updates</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/debug-tools.jsp">Debug Tools</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/index.jsp">New Login</a>
        </div>
    </section>
</main>
<div class="loading-overlay" id="loadingOverlay" aria-hidden="true">
    <div class="loading-box">
        <strong id="loadingMessage">Processing...</strong>
        <div class="progress-track">
            <div class="progress-bar"></div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/loading.js"></script>
</body>
</html>
