package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.folders.FolderReplicationModule;
import com.example.sapbo.modules.folders.FolderReplicationRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/folder-replication")
public class FolderReplicationServlet extends HttpServlet {
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
        request.setAttribute("mode", "setup");
        request.getRequestDispatcher("/folder-replication.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if ("replicate".equalsIgnoreCase(request.getParameter("action"))) {
            replicate(request, response, credentials);
        } else {
            validate(request, response, credentials);
        }
    }

    private void validate(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials) throws ServletException, IOException {
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            FolderReplicationModule module = new FolderReplicationModule(connectionManager);
            List<FolderReplicationRecord> records = module.validate(request.getParameter("scope"),
                    request.getParameter("identifierType"), request.getParameter("identifier"),
                    request.getParameter("destinationRootName"));
            forwardResults(request, response, credentials, records, "validate");
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to validate folder replication: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (RuntimeException exception) {
            request.setAttribute("error", "Unable to validate folder replication: " + exception.getMessage());
            doGet(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private void replicate(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials) throws ServletException, IOException {
        List<FolderReplicationRecord> records = (List<FolderReplicationRecord>) request.getSession().getAttribute("folderReplicationRecords");
        if (records == null || records.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/folder-replication");
            return;
        }
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            FolderReplicationModule module = new FolderReplicationModule(connectionManager);
            forwardResults(request, response, credentials, module.replicate(records), "results");
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to replicate folders: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        } catch (RuntimeException exception) {
            request.setAttribute("error", "Unable to replicate folders: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }

    private void forwardResults(HttpServletRequest request, HttpServletResponse response, BoCredentials credentials,
            List<FolderReplicationRecord> records, String mode) throws ServletException, IOException {
        request.getSession().setAttribute("folderReplicationRecords", records);
        request.setAttribute("records", records);
        request.setAttribute("cms", credentials.getCms());
        request.setAttribute("mode", mode);
        request.setAttribute("readyCount", countReady(records));
        request.getRequestDispatcher("/folder-replication.jsp").forward(request, response);
    }

    private int countReady(List<FolderReplicationRecord> records) {
        int readyCount = 0;
        for (FolderReplicationRecord record : records) {
            if (record.isReady()) {
                readyCount++;
            }
        }
        return readyCount;
    }
}
