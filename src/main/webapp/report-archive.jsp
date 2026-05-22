<%@ page import="com.example.sapbo.modules.reports.ReportArchiveRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<ReportArchiveRecord> records = (List<ReportArchiveRecord>) request.getAttribute("records");
    String mode = String.valueOf(request.getAttribute("mode"));
    Object readyCountObject = request.getAttribute("readyCount");
    int readyCount = readyCountObject == null ? 0 : Integer.parseInt(String.valueOf(readyCountObject));
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Report Archive</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Report Archive</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/report-archive">New Archive</a>
            <a class="button" href="${pageContext.request.contextPath}/report-archive/export">Export Excel</a>
        </div>
    </section>
    <section class="panel">
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div>
        <% } %>
        <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/report-archive" class="options-form">
            <input type="hidden" name="action" value="workbook">
            <div class="form-grid">
                <label>Archive folder name<input type="text" name="archiveFolderName" placeholder="z_Archive2026" required></label>
                <label>Excel workbook<input type="file" name="workbook" accept=".xlsx" required></label>
            </div>
            <button class="loading-trigger" data-loading-message="Validating archive workbook..." type="submit">Validate Excel</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/report-archive" class="options-form">
            <input type="hidden" name="action" value="keyword">
            <div class="form-grid">
                <label>Archive folder name<input type="text" name="archiveFolderName" placeholder="z_Archive2026" required></label>
                <label>Keyword<input type="text" name="keyword" placeholder="#TOARCHIVEMAY2026" required></label>
            </div>
            <button class="loading-trigger" data-loading-message="Finding reports by keyword..." type="submit">Validate Keyword</button>
        </form>
        <% if ("validate".equals(mode) && readyCount > 0) { %>
            <form method="post" action="${pageContext.request.contextPath}/report-archive" class="options-form">
                <input type="hidden" name="action" value="apply">
                <button class="loading-trigger" data-loading-message="Creating folders and moving reports..." type="submit">Archive <%= readyCount %> Valid Reports</button>
            </form>
        <% } %>
        <% if (records == null || records.isEmpty()) { %>
            <p>Upload an `.xlsx` workbook with CUID, Report Name, and Folder Name or Folder Path columns, or enter a keyword to find matching WebI reports.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Row</th><th>CUID</th><th>Report Name</th><th>Current Folder Path</th><th>Archive Folder</th><th>Target Folder Path</th><th>Final Report Name</th><th>Status</th><th>Message</th></tr></thead>
                    <tbody>
                    <% for (ReportArchiveRecord record : records) { %>
                        <tr>
                            <td><%= record.getRowNumber() %></td>
                            <td><%= HtmlEscaper.escape(record.getCuid()) %></td>
                            <td><%= HtmlEscaper.escape(record.getReportName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getCurrentFolderPath()) %></td>
                            <td><%= HtmlEscaper.escape(record.getArchiveFolderName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getTargetFolderPath()) %></td>
                            <td><%= HtmlEscaper.escape(record.getFinalReportName()) %></td>
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
<div class="loading-overlay" id="loadingOverlay" aria-hidden="true"><div class="loading-box"><strong id="loadingMessage">Processing...</strong><div class="progress-track"><div class="progress-bar"></div></div></div></div>
<script src="${pageContext.request.contextPath}/loading.js"></script>
</body>
</html>
