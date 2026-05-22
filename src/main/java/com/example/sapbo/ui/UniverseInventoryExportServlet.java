package com.example.sapbo.ui;

import com.example.sapbo.modules.universes.UniverseInventoryExcelExporter;
import com.example.sapbo.modules.universes.UniverseInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/universes/export")
public class UniverseInventoryExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<UniverseInventoryRecord> universes =
                (List<UniverseInventoryRecord>) request.getSession().getAttribute("universeInventory");

        if (universes == null) {
            response.sendRedirect(request.getContextPath() + "/menu.jsp");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"universe-inventory.xlsx\"");
        new UniverseInventoryExcelExporter().export(universes, response.getOutputStream());
    }
}
