<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Report Debug</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell">
    <section class="panel login-panel">
        <h1>Report Debug</h1>
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div>
        <% } %>
        <form method="post" action="${pageContext.request.contextPath}/reports/debug">
            <label for="cms">CMS</label>
            <input id="cms" name="cms" type="text" placeholder="server-name:6400" required>

            <label for="username">User</label>
            <input id="username" name="username" type="text" autocomplete="username" required>

            <label for="password">Password</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>

            <label for="reportId">Report ID</label>
            <input id="reportId" name="reportId" type="number" value="16824" required>

            <button type="submit">Inspect Report</button>
        </form>
    </section>
</main>
</body>
</html>
