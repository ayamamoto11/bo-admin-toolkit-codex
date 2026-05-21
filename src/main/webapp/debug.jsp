<%@ page import="com.example.sapbo.modules.reports.ReportDebugResult" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    ReportDebugResult debugResult = (ReportDebugResult) request.getAttribute("debugResult");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Report Debug</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Report Debug</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/debug-form.jsp">New Debug</a>
            <a class="button" href="${pageContext.request.contextPath}/index.jsp">Inventory</a>
        </div>
    </section>

    <section class="panel">
        <% if (debugResult == null || !debugResult.isFound()) { %>
            <p>Report ID <%= debugResult == null ? "" : debugResult.getReportId() %> was not found.</p>
        <% } else { %>
            <h2><%= HtmlEscaper.escape(debugResult.getReportName()) %></h2>
            <p>ID: <%= debugResult.getReportId() %></p>
            <p>CUID: <%= HtmlEscaper.escape(debugResult.getReportCuid()) %></p>
            <p>Kind: <%= HtmlEscaper.escape(debugResult.getReportKind()) %></p>

            <h2>Relationship Results</h2>
            <% for (ReportDebugResult.RelationshipResult relationship : debugResult.getRelationshipResults()) { %>
                <h3><%= HtmlEscaper.escape(relationship.getRelationshipFunction()) %>
                    <%= HtmlEscaper.escape(relationship.getRelationshipName()) %></h3>
                <p><%= HtmlEscaper.escape(relationship.getQuery()) %></p>
                <% if (!relationship.getError().isEmpty()) { %>
                    <div class="alert"><%= HtmlEscaper.escape(relationship.getError()) %></div>
                <% } else if (relationship.getRows().isEmpty()) { %>
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
                            <% for (ReportDebugResult.DebugRow row : relationship.getRows()) { %>
                                <tr>
                                    <td><%= HtmlEscaper.escape(row.getName()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getValue()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getKind()) %></td>
                                    <td><%= HtmlEscaper.escape(row.getCuid()) %></td>
                                </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            <% } %>

            <h2>Report Properties</h2>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Property</th>
                        <th>Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (ReportDebugResult.DebugRow property : debugResult.getProperties()) { %>
                        <tr>
                            <td><%= HtmlEscaper.escape(property.getName()) %></td>
                            <td><%= HtmlEscaper.escape(property.getValue()) %></td>
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
