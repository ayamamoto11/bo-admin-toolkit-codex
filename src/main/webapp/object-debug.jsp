<%@ page import="com.example.sapbo.core.DebugResult" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    DebugResult debugResult = (DebugResult) request.getAttribute("debugResult");
    String debugTitle = (String) request.getAttribute("debugTitle");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= HtmlEscaper.escape(debugTitle) %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1><%= HtmlEscaper.escape(debugTitle) %></h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/debug-tools.jsp">New Debug</a>
            <% if (debugResult != null && debugResult.isFound()) { %>
                <a class="button" href="${pageContext.request.contextPath}/debug/export">Export Excel</a>
            <% } %>
            <a class="button" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
        </div>
    </section>

    <section class="panel">
        <% if (debugResult == null || !debugResult.isFound()) { %>
            <p>Object ID <%= debugResult == null ? "" : debugResult.getObjectId() %> was not found.</p>
        <% } else { %>
            <h2><%= HtmlEscaper.escape(debugResult.getObjectName()) %></h2>
            <p>ID: <%= debugResult.getObjectId() %></p>
            <p>CUID: <%= HtmlEscaper.escape(debugResult.getObjectCuid()) %></p>
            <p>Kind: <%= HtmlEscaper.escape(debugResult.getObjectKind()) %></p>

            <h2>Query Results</h2>
            <% for (DebugResult.QueryResult queryResult : debugResult.getQueryResults()) { %>
                <h3><%= HtmlEscaper.escape(queryResult.getLabel()) %></h3>
                <p><%= HtmlEscaper.escape(queryResult.getQuery()) %></p>
                <% if (!queryResult.getError().isEmpty()) { %>
                    <div class="alert"><%= HtmlEscaper.escape(queryResult.getError()) %></div>
                <% } else if (queryResult.getRows().isEmpty()) { %>
                    <p>No rows.</p>
                <% } else { %>
                    <div class="table-wrap">
                        <table>
                            <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Kind</th>
                                <th>CUID</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (DebugResult.Row row : queryResult.getRows()) { %>
                                <tr>
                                    <td><%= HtmlEscaper.escape(row.getId()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getName()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getKind()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getCuid()) %></td>
                                </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            <% } %>

            <h2>Properties</h2>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Property</th>
                        <th>Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (DebugResult.Row property : debugResult.getProperties()) { %>
                        <tr>
                            <td><%= HtmlEscaper.escape(property.getId()) %></td>
                            <td><%= HtmlEscaper.escape(property.getName()) %></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
