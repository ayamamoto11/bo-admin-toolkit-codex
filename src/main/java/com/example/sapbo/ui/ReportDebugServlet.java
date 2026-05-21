package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.reports.ReportDebugModule;
import com.example.sapbo.modules.reports.ReportDebugResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/reports/debug")
public class ReportDebugServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            BoCredentials credentials = new BoCredentials(
                    request.getParameter("cms"),
                    request.getParameter("username"),
                    request.getParameter("password"));
            int reportId = Integer.parseInt(request.getParameter("reportId"));

            try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
                ReportDebugModule module = new ReportDebugModule(connectionManager);
                ReportDebugResult debugResult = module.inspectReport(reportId);
                request.setAttribute("debugResult", debugResult);
                request.setAttribute("cms", credentials.getCms());
                request.getRequestDispatcher("/debug.jsp").forward(request, response);
            }
        } catch (NumberFormatException exception) {
            request.setAttribute("error", "Report ID must be a number.");
            request.getRequestDispatcher("/debug-form.jsp").forward(request, response);
        } catch (IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            request.getRequestDispatcher("/debug-form.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to inspect SAP BusinessObjects report: " + exception.getMessage());
            request.getRequestDispatcher("/debug-form.jsp").forward(request, response);
        }
    }
}
