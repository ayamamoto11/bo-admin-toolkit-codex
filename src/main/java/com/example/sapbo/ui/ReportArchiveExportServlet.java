package com.example.sapbo.ui;

import com.example.sapbo.modules.reports.ReportArchiveExcelExporter;
import com.example.sapbo.modules.reports.ReportArchiveRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/report-archive/export")
public class ReportArchiveExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<ReportArchiveRecord> records = (List<ReportArchiveRecord>) request.getSession().getAttribute("reportArchiveRecords");
        if (records == null) {
            response.sendRedirect(request.getContextPath() + "/report-archive");
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"report-archive-results.xlsx\"");
        new ReportArchiveExcelExporter().export(records, response.getOutputStream());
    }
}
