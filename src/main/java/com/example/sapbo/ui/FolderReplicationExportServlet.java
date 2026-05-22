package com.example.sapbo.ui;

import com.example.sapbo.modules.folders.FolderReplicationExcelExporter;
import com.example.sapbo.modules.folders.FolderReplicationRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/folder-replication/export")
public class FolderReplicationExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<FolderReplicationRecord> records = (List<FolderReplicationRecord>) request.getSession().getAttribute("folderReplicationRecords");
        if (records == null) {
            response.sendRedirect(request.getContextPath() + "/folder-replication");
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"folder-replication-results.xlsx\"");
        new FolderReplicationExcelExporter().export(records, response.getOutputStream());
    }
}
