package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.reports.ReportArchiveModule;
import com.example.sapbo.modules.reports.ReportArchiveRecord;

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
@WebServlet("/report-archive")
public class ReportArchiveServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        request.setAttribute("cms", credentials.getCms());
        request.setAttribute("records", Collections.emptyList());
        request.setAttribute("readyCount", 0);
        request.setAttribute("mode", "upload");
        request.getRequestDispatcher("/report-archive.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        String action = request.getParameter("action");
        if ("apply".equalsIgnoreCase(action)) {
            applyArchive(request, response, credentials);
        } else if ("keyword".equalsIgnoreCase(action)) {
            validateKeyword(request, response, credentials);
        } else {
            validateWorkbook(request, response, credentials);
        }
    }

    private void validateWorkbook(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials) throws ServletException, IOException {
        String archiveFolderName = request.getParameter("archiveFolderName");
        Part workbookPart = request.getPart("workbook");
        if (workbookPart == null || workbookPart.getSize() == 0) {
            request.setAttribute("error", "Please choose an Excel workbook to validate.");
            doGet(request, response);
            return;
        }
        try (ConnectionManager connectionManager = new ConnectionManager(credentials);
             InputStream workbookStream = workbookPart.getInputStream()) {
            ReportArchiveModule module = new ReportArchiveModule(connectionManager);
            forwardResults(request, response, credentials, module.validateWorkbook(workbookStream, archiveFolderName), "validate");
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to validate archive workbook: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (RuntimeException exception) {
            request.setAttribute("error", "Unable to read archive workbook: " + exception.getMessage());
            doGet(request, response);
        } catch (IOException exception) {
            request.setAttribute("error", "Unable to read archive workbook: " + exception.getMessage());
            doGet(request, response);
        }
    }

    private void validateKeyword(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials) throws ServletException, IOException {
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            ReportArchiveModule module = new ReportArchiveModule(connectionManager);
            forwardResults(request, response, credentials,
                    module.validateKeyword(request.getParameter("keyword"), request.getParameter("archiveFolderName")), "validate");
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to validate archive keyword: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyArchive(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials) throws ServletException, IOException {
        List<ReportArchiveRecord> records = (List<ReportArchiveRecord>) request.getSession().getAttribute("reportArchiveRecords");
        if (records == null || records.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/report-archive");
            return;
        }
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            ReportArchiveModule module = new ReportArchiveModule(connectionManager);
            forwardResults(request, response, credentials, module.archiveReports(records), "results");
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to archive reports: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }

    private void forwardResults(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials,
            List<ReportArchiveRecord> records, String mode) throws ServletException, IOException {
        request.getSession().setAttribute("reportArchiveRecords", records);
        request.setAttribute("records", records);
        request.setAttribute("cms", credentials.getCms());
        request.setAttribute("mode", mode);
        request.setAttribute("readyCount", countReady(records));
        request.getRequestDispatcher("/report-archive.jsp").forward(request, response);
    }

    private int countReady(List<ReportArchiveRecord> records) {
        int readyCount = 0;
        for (ReportArchiveRecord record : records) {
            if (record.isReady()) {
                readyCount++;
            }
        }
        return readyCount;
    }
}
