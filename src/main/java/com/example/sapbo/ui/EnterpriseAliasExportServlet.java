package com.example.sapbo.ui;

import com.example.sapbo.modules.aliases.EnterpriseAliasExcelExporter;
import com.example.sapbo.modules.aliases.EnterpriseAliasRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/enterprise-aliases/export")
public class EnterpriseAliasExportServlet extends HttpServlet {
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<EnterpriseAliasRecord> records =
                (List<EnterpriseAliasRecord>) request.getSession().getAttribute("enterpriseAliasRecords");
        if (records == null) {
            response.sendRedirect(request.getContextPath() + "/enterprise-aliases");
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"enterprise-aliases.xlsx\"");
        new EnterpriseAliasExcelExporter().export(records, response.getOutputStream());
    }
}
