<%@ page import="com.example.sapbo.modules.aliases.EnterpriseAliasRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<EnterpriseAliasRecord> records =
            (List<EnterpriseAliasRecord>) request.getAttribute("records");
    String mode = String.valueOf(request.getAttribute("mode"));
    boolean previewMode = "preview".equals(mode);
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Enterprise Alias Creator</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Enterprise Alias Creator</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button secondary loading-trigger" data-loading-message="Refreshing Enterprise alias candidates..."
               href="${pageContext.request.contextPath}/enterprise-aliases">Refresh</a>
            <a class="button" href="${pageContext.request.contextPath}/enterprise-aliases/export">Export Excel</a>
        </div>
    </section>
    <section class="panel">
        <% if (previewMode) { %>
            <form method="post" action="${pageContext.request.contextPath}/enterprise-aliases" class="options-form">
                <div class="form-grid">
                    <label>Password option
                        <select name="passwordMode">
                            <option value="random">Random password</option>
                            <option value="manual">Use password below</option>
                        </select>
                    </label>
                    <label>Manual password
                        <input type="password" name="manualPassword" autocomplete="new-password">
                    </label>
                    <label class="checkbox-label">
                        <input type="checkbox" name="disableAlias" value="true">
                        Disable created Enterprise aliases
                    </label>
                </div>
                <button class="loading-trigger" data-loading-message="Creating Enterprise aliases..." type="submit">
                    Create Enterprise Aliases
                </button>
            </form>
        <% } %>
        <% if (records == null || records.isEmpty()) { %>
            <p>No AD users without an Enterprise alias were found.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>User ID</th><th>Username</th><th>Full Name</th><th>User CUID</th><th>AD Aliases</th>
                        <th>Enterprise Alias</th><th>Password</th><th>Disabled</th><th>Status</th><th>Message</th>
                    </tr></thead>
                    <tbody>
                    <% for (EnterpriseAliasRecord record : records) { %>
                        <tr>
                            <td><%= record.getUserId() %></td>
                            <td><%= HtmlEscaper.escape(record.getUsername()) %></td>
                            <td><%= HtmlEscaper.escape(record.getFullName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getUserCuid()) %></td>
                            <td><%= HtmlEscaper.escape(record.getAdAliases()) %></td>
                            <td><%= HtmlEscaper.escape(record.getEnterpriseAlias()) %></td>
                            <td><%= HtmlEscaper.escape(record.getPassword()) %></td>
                            <td><%= HtmlEscaper.escape(record.getDisabled()) %></td>
                            <td><%= HtmlEscaper.escape(record.getStatus()) %></td>
                            <td><%= HtmlEscaper.escape(record.getMessage()) %></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        <% } %>
    </section>
</main>
<div class="loading-overlay" id="loadingOverlay" aria-hidden="true">
    <div class="loading-box">
        <strong id="loadingMessage">Processing...</strong>
        <div class="progress-track"><div class="progress-bar"></div></div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/loading.js"></script>
</body>
</html>
