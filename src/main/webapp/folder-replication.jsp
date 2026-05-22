<%@ page import="com.example.sapbo.modules.folders.FolderReplicationRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<FolderReplicationRecord> records = (List<FolderReplicationRecord>) request.getAttribute("records");
    String mode = String.valueOf(request.getAttribute("mode"));
    Object readyCountObject = request.getAttribute("readyCount");
    int readyCount = readyCountObject == null ? 0 : Integer.parseInt(String.valueOf(readyCountObject));
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Folder Replication</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div><h1>Folder Replication</h1><p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p></div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button secondary" href="${pageContext.request.contextPath}/folder-replication">New Replication</a>
            <a class="button" href="${pageContext.request.contextPath}/folder-replication/export">Export Excel</a>
        </div>
    </section>
    <section class="panel">
        <% if (request.getAttribute("error") != null) { %><div class="alert"><%= HtmlEscaper.escape(request.getAttribute("error")) %></div><% } %>
        <form method="post" action="${pageContext.request.contextPath}/folder-replication" class="options-form">
            <input type="hidden" name="action" value="validate">
            <div class="form-grid">
                <label>Replication scope<select name="scope"><option value="all">All public folder structure</option><option value="specific">Specific folder and subfolders</option></select></label>
                <label>Destination root folder<input type="text" name="destinationRootName" placeholder="Z_Archive2026" required></label>
                <label>Specific folder identifier type<select name="identifierType"><option value="id">ID / SI_ID</option><option value="cuid">CUID / SI_CUID</option></select></label>
                <label>Specific folder ID or CUID<input type="text" name="identifier" placeholder="Only needed for specific scope"></label>
            </div>
            <button class="loading-trigger" data-loading-message="Validating folder structure..." type="submit">Validate Folder Structure</button>
        </form>
        <% if ("validate".equals(mode) && readyCount > 0) { %>
            <form method="post" action="${pageContext.request.contextPath}/folder-replication" class="options-form">
                <input type="hidden" name="action" value="replicate">
                <button class="loading-trigger" data-loading-message="Replicating folder structure..." type="submit">Replicate <%= readyCount %> Missing Folders</button>
            </form>
        <% } %>
        <% if (records == null || records.isEmpty()) { %>
            <p>Choose all folders or enter one folder ID/CUID, then validate the destination structure before creating anything.</p>
        <% } else { %>
            <div class="table-wrap"><table>
                <thead><tr><th>Source Folder ID</th><th>Source Folder CUID</th><th>Source Folder Name</th><th>Source Folder Path</th><th>Destination Root</th><th>Destination Folder Path</th><th>Status</th><th>Message</th></tr></thead>
                <tbody>
                <% for (FolderReplicationRecord record : records) { %>
                    <tr><td><%= record.getSourceFolderId() %></td><td><%= HtmlEscaper.escape(record.getSourceFolderCuid()) %></td><td><%= HtmlEscaper.escape(record.getSourceFolderName()) %></td><td><%= HtmlEscaper.escape(record.getSourceFolderPath()) %></td><td><%= HtmlEscaper.escape(record.getDestinationRootName()) %></td><td><%= HtmlEscaper.escape(record.getDestinationFolderPath()) %></td><td><%= HtmlEscaper.escape(record.getStatus()) %></td><td><%= HtmlEscaper.escape(record.getMessage()) %></td></tr>
                <% } %>
                </tbody>
            </table></div>
        <% } %>
    </section>
</main>
<div class="loading-overlay" id="loadingOverlay" aria-hidden="true"><div class="loading-box"><strong id="loadingMessage">Processing...</strong><div class="progress-track"><div class="progress-bar"></div></div></div></div>
<script src="${pageContext.request.contextPath}/loading.js"></script>
</body>
</html>
