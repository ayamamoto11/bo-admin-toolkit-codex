package com.example.sapbo.ui;

import com.example.sapbo.core.DebugResult;
import com.example.sapbo.core.DebugResultExcelExporter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/debug/export")
public class ObjectDebugExportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DebugResult debugResult = (DebugResult) request.getSession().getAttribute("objectDebugResult");
        String debugTitle = (String) request.getSession().getAttribute("objectDebugTitle");

        if (debugResult == null) {
            response.sendRedirect(request.getContextPath() + "/debug-tools.jsp");
            return;
        }

        String fileName = buildFileName(debugTitle, debugResult.getObjectId());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        new DebugResultExcelExporter().export(debugResult, debugTitle, response.getOutputStream());
    }

    private String buildFileName(String debugTitle, int objectId) {
        String prefix = debugTitle == null || debugTitle.trim().isEmpty()
                ? "object-debug"
                : debugTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (prefix.isEmpty()) {
            prefix = "object-debug";
        }

        return prefix + "-" + objectId + ".xlsx";
    }
}
