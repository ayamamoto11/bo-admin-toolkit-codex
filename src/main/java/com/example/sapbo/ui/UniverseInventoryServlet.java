package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.universes.UniverseInventoryModule;
import com.example.sapbo.modules.universes.UniverseInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/universes")
public class UniverseInventoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            UniverseInventoryModule module = new UniverseInventoryModule(connectionManager);
            List<UniverseInventoryRecord> universes = module.findUniverses();
            request.getSession().setAttribute("universeInventory", universes);
            request.setAttribute("universes", universes);
            request.setAttribute("cms", credentials.getCms());
            request.getRequestDispatcher("/universes.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to query SAP BusinessObjects universes: " + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }
}
