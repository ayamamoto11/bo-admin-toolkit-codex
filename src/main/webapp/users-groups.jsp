<%@ page import="com.example.sapbo.modules.users.UserGroupInventoryRecord" %>
<%@ page import="com.example.sapbo.ui.HtmlEscaper" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<UserGroupInventoryRecord> usersAndGroups =
            (List<UserGroupInventoryRecord>) request.getAttribute("usersAndGroups");
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Users and Groups Inventory</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles.css">
</head>
<body>
<main class="page-shell wide">
    <section class="toolbar">
        <div>
            <h1>Users and Groups Inventory</h1>
            <p>CMS: <%= HtmlEscaper.escape(request.getAttribute("cms")) %></p>
        </div>
        <div class="actions">
            <a class="button secondary" href="${pageContext.request.contextPath}/menu.jsp">Modules</a>
            <a class="button" href="${pageContext.request.contextPath}/users-groups/export">Export Excel</a>
        </div>
    </section>

    <section class="panel">
        <% if (usersAndGroups == null || usersAndGroups.isEmpty()) { %>
            <p>No users or groups found.</p>
        <% } else { %>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Object Type</th>
                        <th>Object ID</th>
                        <th>Object Name</th>
                        <th>Object CUID</th>
                        <th>Parent Group ID</th>
                        <th>Parent Group Name</th>
                        <th>Group Path</th>
                        <th>Disabled</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (UserGroupInventoryRecord record : usersAndGroups) { %>
                        <tr>
                            <td><%= HtmlEscaper.escape(record.getObjectType()) %></td>
                            <td><%= record.getObjectId() %></td>
                            <td><%= HtmlEscaper.escape(record.getObjectName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getObjectCuid()) %></td>
                            <td><%= HtmlEscaper.escape(record.getParentGroupId()) %></td>
                            <td><%= HtmlEscaper.escape(record.getParentGroupName()) %></td>
                            <td><%= HtmlEscaper.escape(record.getGroupPath()) %></td>
                            <td><%= HtmlEscaper.escape(record.getDisabled()) %></td>
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
