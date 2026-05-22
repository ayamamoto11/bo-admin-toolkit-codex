package com.example.sapbo.ui;

import com.example.sapbo.modules.connections.ConnectionInventoryExcelExporter;
import com.example.sapbo.modules.connections.ConnectionInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/connections/export")
public class ConnectionInventoryExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<ConnectionInventoryRecord> connections =
                (List<ConnectionInventoryRecord>) request.getSession().getAttribute("connectionInventory");

        if (connections == null) {
            response.sendRedirect(request.getContextPath() + "/menu.jsp");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"connection-inventory.xlsx\"");
        new ConnectionInventoryExcelExporter().export(connections, response.getOutputStream());
    }
}
