package com.example.sapbo.ui;

import com.example.sapbo.modules.users.UserGroupInventoryExcelExporter;
import com.example.sapbo.modules.users.UserGroupInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/users-groups/export")
public class UserGroupInventoryExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<UserGroupInventoryRecord> usersAndGroups =
                (List<UserGroupInventoryRecord>) request.getSession().getAttribute("userGroupInventory");

        if (usersAndGroups == null) {
            response.sendRedirect(request.getContextPath() + "/menu.jsp");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"users-groups-inventory.xlsx\"");
        new UserGroupInventoryExcelExporter().export(usersAndGroups, response.getOutputStream());
    }
}
