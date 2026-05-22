package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.connections.ConnectionInventoryModule;
import com.example.sapbo.modules.connections.ConnectionInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/connections")
public class ConnectionInventoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            ConnectionInventoryModule module = new ConnectionInventoryModule(connectionManager);
            List<ConnectionInventoryRecord> connections = module.findConnections();
            request.getSession().setAttribute("connectionInventory", connections);
            request.setAttribute("connections", connections);
            request.setAttribute("cms", credentials.getCms());
            request.getRequestDispatcher("/connections.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to query SAP BusinessObjects connections: "
                    + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }
}
