package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.reports.ReportInventoryModule;
import com.example.sapbo.modules.reports.ReportInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/reports")
public class ReportInventoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try {
            try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
                ReportInventoryModule module = new ReportInventoryModule(connectionManager);
                List<ReportInventoryRecord> reports = module.findWebIntelligenceReports();
                request.getSession().setAttribute("reportInventory", reports);
                request.setAttribute("reports", reports);
                request.setAttribute("cms", credentials.getCms());
                request.getRequestDispatcher("/reports.jsp").forward(request, response);
            }
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to query SAP BusinessObjects: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }
}
