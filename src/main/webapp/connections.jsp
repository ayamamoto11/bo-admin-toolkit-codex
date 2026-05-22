<%@ page import="com.example.sapbo.modules.connections.ConnectionInventoryRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<ConnectionInventoryRecord> connections =
            (List<ConnectionInventoryRecord>) request.getAttribute("connections");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Connection Inventory</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Connection Inventory</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button" href="${pageContext.request.contextPath}/connections/export">Export Excel</a>
        </div>
    </section>
    <section class="panel">
        <% if (connections == null || connections.isEmpty()) { %>
            <p>No connections found.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead><tr>
                        <th>Connection ID</th><th>Connection Name</th><th>Connection CUID</th><th>Kind</th>
                        <th>Parent Folder ID</th><th>Folder Path</th><th>Description</th><th>Connection Type</th>
                        <th>Database</th><th>Server</th><th>Data Source</th><th>Network Layer</th><th>User Name</th><th>Metadata Summary</th>
                    </tr></thead>
                    <tbody>
                    <% for (ConnectionInventoryRecord connection : connections) { %>
                        <tr>
                            <td><%= connection.getConnectionId() %></td>
                            <td><%= HtmlEscaper.escape(connection.getConnectionName()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getConnectionCuid()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getConnectionKind()) %></td>
                            <td><%= connection.getParentFolderId() %></td>
                            <td><%= HtmlEscaper.escape(connection.getFolderPath()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getDescription()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getConnectionType()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getDatabaseName()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getServerName()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getDataSource()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getNetworkLayer()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getUserName()) %></td>
                            <td><%= HtmlEscaper.escape(connection.getMetadataSummary()) %></td>
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
