package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.reports.ReportPropertyUpdateModule;
import com.example.sapbo.modules.reports.ReportPropertyUpdateRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@MultipartConfig
@WebServlet("/report-property-updates")
public class ReportPropertyUpdateServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        request.setAttribute("cms", credentials.getCms());
        request.setAttribute("records", Collections.emptyList());
        request.setAttribute("mode", "upload");
        request.getRequestDispatcher("/report-property-updates.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("apply".equalsIgnoreCase(action)) {
            applyUpdates(request, response, credentials);
            return;
        }

        validateUpload(request, response, credentials);
    }

    private void validateUpload(
            HttpServletRequest request,
            HttpServletResponse response,
            BoCredentials credentials) throws ServletException, IOException {
        String identifierType = request.getParameter("identifierType");
        Part workbookPart = request.getPart("workbook");
        if (workbookPart == null || workbookPart.getSize() == 0) {
            request.setAttribute("error", "Please choose an Excel workbook to validate.");
            doGet(request, response);
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials);
             InputStream workbookStream = workbookPart.getInputStream()) {
            ReportPropertyUpdateModule module = new ReportPropertyUpdateModule(connectionManager);
            List<ReportPropertyUpdateRecord> records = module.validate(workbookStream, identifierType);
            request.getSession().setAttribute("reportPropertyUpdateRecords", records);
            request.getSession().setAttribute("reportPropertyUpdateIdentifierType", identifierType);
            request.setAttribute("records", records);
            request.setAttribute("cms", credentials.getCms());
            request.setAttribute("mode", "validate");
            request.setAttribute("readyCount", countReady(records));
            request.getRequestDispatcher("/report-property-updates.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to validate report update workbook: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (RuntimeException exception) {
            request.setAttribute("error", "Unable to read report update workbook: " + exception.getMessage());
            doGet(request, response);
        } catch (IOException exception) {
            request.setAttribute("error", "Unable to read report update workbook: " + exception.getMessage());
            doGet(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyUpdates(
            HttpServletRequest request,
            HttpServletResponse response,
            BoCredentials credentials) throws ServletException, IOException {
        List<ReportPropertyUpdateRecord> records =
                (List<ReportPropertyUpdateRecord>) request.getSession().getAttribute("reportPropertyUpdateRecords");
        String identifierType =
                (String) request.getSession().getAttribute("reportPropertyUpdateIdentifierType");

        if (records == null || records.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/report-property-updates");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            ReportPropertyUpdateModule module = new ReportPropertyUpdateModule(connectionManager);
            List<ReportPropertyUpdateRecord> results = module.applyUpdates(records, identifierType);
            request.getSession().setAttribute("reportPropertyUpdateRecords", results);
            request.setAttribute("records", results);
            request.setAttribute("cms", credentials.getCms());
            request.setAttribute("mode", "results");
            request.setAttribute("readyCount", 0);
            request.getRequestDispatcher("/report-property-updates.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to update report properties: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }

    private int countReady(List<ReportPropertyUpdateRecord> records) {
        int readyCount = 0;
        for (ReportPropertyUpdateRecord record : records) {
            if (record.isReady()) {
                readyCount++;
            }
        }

        return readyCount;
    }
}
