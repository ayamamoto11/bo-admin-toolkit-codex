package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.aliases.EnterpriseAliasModule;
import com.example.sapbo.modules.aliases.EnterpriseAliasRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/enterprise-aliases")
public class EnterpriseAliasServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            EnterpriseAliasModule module = new EnterpriseAliasModule(connectionManager);
            List<EnterpriseAliasRecord> candidates = module.findCandidates();
            request.getSession().setAttribute("enterpriseAliasRecords", candidates);
            request.setAttribute("records", candidates);
            request.setAttribute("cms", credentials.getCms());
            request.setAttribute("mode", "preview");
            request.getRequestDispatcher("/enterprise-aliases.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to query SAP BusinessObjects users for Enterprise aliases: "
                    + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        String passwordMode = request.getParameter("passwordMode");
        String manualPassword = request.getParameter("manualPassword");
        boolean disableAlias = "true".equalsIgnoreCase(request.getParameter("disableAlias"));
        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            EnterpriseAliasModule module = new EnterpriseAliasModule(connectionManager);
            List<EnterpriseAliasRecord> results = module.createAliases(passwordMode, manualPassword, disableAlias);
            request.getSession().setAttribute("enterpriseAliasRecords", results);
            request.setAttribute("records", results);
            request.setAttribute("cms", credentials.getCms());
            request.setAttribute("mode", "results");
            request.getRequestDispatcher("/enterprise-aliases.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to create Enterprise aliases: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }
}
