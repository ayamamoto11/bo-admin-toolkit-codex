<%@ page import="com.example.sapbo.modules.universes.UniverseInventoryRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<UniverseInventoryRecord> universes =
            (List<UniverseInventoryRecord>) request.getAttribute("universes");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Universe Inventory</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Universe Inventory</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button" href="${pageContext.request.contextPath}/universes/export">Export Excel</a>
        </div>
    </section>

    <section class="panel">
        <% if (universes == null || universes.isEmpty()) { %>
            <p>No universes found.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Universe ID</th>
                        <th>Universe Name</th>
                        <th>Universe CUID</th>
                        <th>Universe Type</th>
                        <th>Parent Folder ID</th>
                        <th>Folder Path</th>
                        <th>Connection ID</th>
                        <th>Connection Name</th>
                        <th>Connection CUID</th>
                        <th>Connection Kind</th>
                        <th>Connection Type</th>
                        <th>Database</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (UniverseInventoryRecord universe : universes) { %>
                        <tr>
                            <td><%= universe.getUniverseId() %></td>
                            <td><%= HtmlEscaper.escape(universe.getUniverseName()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getUniverseCuid()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getUniverseType()) %></td>
                            <td><%= universe.getParentFolderId() %></td>
                            <td><%= HtmlEscaper.escape(universe.getFolderPath()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getConnectionId()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getConnectionName()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getConnectionCuid()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getConnectionKind()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getConnectionType()) %></td>
                            <td><%= HtmlEscaper.escape(universe.getDatabaseName()) %></td>
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
