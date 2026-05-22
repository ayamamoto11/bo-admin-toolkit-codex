<%@ page import="com.example.sapbo.modules.reports.ReportInventoryRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<ReportInventoryRecord> reports = (List<ReportInventoryRecord>) request.getAttribute("reports");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>WebI Report Inventory</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Web Intelligence Report Inventory</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button" href="${pageContext.request.contextPath}/reports/export">Export Excel</a>
        </div>
    </section>

    <section class="panel">
        <% if (reports == null || reports.isEmpty()) { %>
            <p>No Web Intelligence reports found.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Report Name</th>
                        <th>Object ID</th>
                        <th>CUID</th>
                        <th>Owner ID</th>
                        <th>Parent Folder ID</th>
                        <th>Folder Path</th>
                        <th>Data Provider Type</th>
                        <th>Data Provider Name</th>
                        <th>Universe ID</th>
                        <th>Universe</th>
                        <th>Universe Type</th>
                        <th>Universe CUID</th>
                        <th>Universe Path</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (ReportInventoryRecord report : reports) { %>
                        <tr>
                            <td><%= HtmlEscaper.escape(report.getReportName()) %></td>
                            <td><%= report.getObjectId() %></td>
                            <td><%= HtmlEscaper.escape(report.getCuid()) %></td>
                            <td><%= report.getOwnerId() %></td>
                            <td><%= report.getParentFolderId() %></td>
                            <td><%= HtmlEscaper.escape(report.getFolderPath()) %></td>
                            <td><%= HtmlEscaper.escape(report.getDataProviderType()) %></td>
                            <td><%= HtmlEscaper.escape(report.getDataProviderName()) %></td>
                            <td><%= HtmlEscaper.escape(report.getUniverseId()) %></td>
                            <td><%= HtmlEscaper.escape(report.getUniverseName()) %></td>
                            <td><%= HtmlEscaper.escape(report.getUniverseType()) %></td>
                            <td><%= HtmlEscaper.escape(report.getUniverseCuid()) %></td>
                            <td><%= HtmlEscaper.escape(report.getUniversePath()) %></td>
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
