package com.example.sapbo.ui;

import com.example.sapbo.modules.reports.ReportPropertyUpdateExcelExporter;
import com.example.sapbo.modules.reports.ReportPropertyUpdateRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/report-property-updates/export")
public class ReportPropertyUpdateExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<ReportPropertyUpdateRecord> records =
                (List<ReportPropertyUpdateRecord>) request.getSession().getAttribute("reportPropertyUpdateRecords");
        if (records == null) {
            response.sendRedirect(request.getContextPath() + "/report-property-updates");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"report-property-updates.xlsx\"");
        new ReportPropertyUpdateExcelExporter().export(records, response.getOutputStream());
    }
}
