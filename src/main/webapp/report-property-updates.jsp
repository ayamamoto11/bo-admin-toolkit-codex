<%@ page import="com.example.sapbo.modules.reports.ReportPropertyUpdateRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<ReportPropertyUpdateRecord> records =
            (List<ReportPropertyUpdateRecord>) request.getAttribute("records");
    String mode = String.valueOf(request.getAttribute("mode"));
    Object readyCountObject = request.getAttribute("readyCount");
    int readyCount = readyCountObject == null ? 0 : Integer.parseInt(String.valueOf(readyCountObject));
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Report Property Updates</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Report Property Updates</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/report-property-updates">New Upload</a>
            <a class="button" href="${pageContext.request.contextPath}/report-property-updates/export">Export Excel</a>
        </div>
    </section>

    <section class="panel">
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div>
        <% } %>

        <form method="post" enctype="multipart/form-data"
              action="${pageContext.request.contextPath}/report-property-updates" class="options-form">
            <input type="hidden" name="action" value="validate">
            <div class="form-grid">
                <label>
                    Identifier column
                    <select name="identifierType">
                        <option value="id">ID / SI_ID</option>
                        <option value="cuid">CUID / SI_CUID</option>
                    </select>
                </label>
                <label>
                    Excel workbook
                    <input type="file" name="workbook" accept=".xlsx" required>
                </label>
            </div>
            <button class="loading-trigger" data-loading-message="Validating report update workbook..." type="submit">
                Validate Excel
            </button>
        </form>

        <% if ("validate".equals(mode) && readyCount > 0) { %>
            <form method="post" action="${pageContext.request.contextPath}/report-property-updates" class="options-form">
                <input type="hidden" name="action" value="apply">
                <button class="loading-trigger" data-loading-message="Updating report properties..." type="submit">
                    Run Updates for <%= readyCount %> Valid Rows
                </button>
            </form>
        <% } %>

        <% if (records == null || records.isEmpty()) { %>
            <p>Upload an `.xlsx` workbook with columns for ID or CUID plus optional Name, Description, and Keyword.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Row</th>
                        <th>Identifier</th>
                        <th>Report ID</th>
                        <th>Report CUID</th>
                        <th>Current Name</th>
                        <th>New Name</th>
                        <th>New Description</th>
                        <th>New Keyword</th>
                        <th>Status</th>
                        <th>Message</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (ReportPropertyUpdateRecord record : records) { %>
                        <tr>
                            <td><%= record.getRowNumber() %></td>
                            <td><%= HtmlEscaper.escape(record.getIdentifier()) %></td>
                            <td><%= HtmlEscaper.escape(record.getReportId()) %></td>
                            <td><%= HtmlEscaper.escape(record.getReportCuid()) %></td>
                            <td><%= HtmlEscaper.escape(record.getCurrentName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getNewName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getNewDescription()) %></td>
                            <td><%= HtmlEscaper.escape(record.getNewKeyword()) %></td>
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
