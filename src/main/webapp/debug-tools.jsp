<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Debug Tools</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell">
    <section class="panel login-panel">
        <h1>Debug Tools</h1>
        <p>CMS: <%= HtmlEscaper.escape(session.getAttribute("cms")) %></p>
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/universes/debug">
            <label for="universeId">Universe ID</label>
            <input id="universeId" name="universeId" type="number" required>
            <button type="submit">Inspect Universe</button>
        </form>

        <hr>

        <form method="post" action="${pageContext.request.contextPath}/users-groups/debug">
            <label for="userId">User ID</label>
            <input id="userId" name="userId" type="number" required>
            <button type="submit">Inspect User</button>
        </form>

        <div class="module-actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
        </div>
    </section>
</main>
</body>
</html>
