package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.core.DebugResult;
import com.example.sapbo.modules.universes.UniverseDebugModule;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/universes/debug")
public class UniverseDebugServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            int universeId = Integer.parseInt(request.getParameter("universeId"));
            UniverseDebugModule module = new UniverseDebugModule(connectionManager);
            DebugResult debugResult = module.inspectUniverse(universeId);
            request.setAttribute("debugTitle", "Universe Debug");
            request.setAttribute("debugResult", debugResult);
            request.setAttribute("cms", credentials.getCms());
            request.getRequestDispatcher("/object-debug.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            request.setAttribute("error", "Universe ID must be a number.");
            request.getRequestDispatcher("/debug-tools.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to inspect universe: " + exception.getMessage());
            request.getRequestDispatcher("/debug-tools.jsp").forward(request, response);
        }
    }
}
