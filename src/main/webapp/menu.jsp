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
            <a class="button" href="${pageContext.request.contextPath}/reports">WebI Report Inventory</a>
            <a class="button" href="${pageContext.request.contextPath}/universes">Universe Inventory</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/index.jsp">New Login</a>
        </div>
    </section>
</main>
</body>
</html>
