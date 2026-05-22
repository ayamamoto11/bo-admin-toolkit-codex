package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.modules.users.UserGroupInventoryModule;
import com.example.sapbo.modules.users.UserGroupInventoryRecord;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/users-groups")
public class UserGroupInventoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            UserGroupInventoryModule module = new UserGroupInventoryModule(connectionManager);
            List<UserGroupInventoryRecord> usersAndGroups = module.findUsersAndGroups();
            request.getSession().setAttribute("userGroupInventory", usersAndGroups);
            request.setAttribute("usersAndGroups", usersAndGroups);
            request.setAttribute("cms", credentials.getCms());
            request.getRequestDispatcher("/users-groups.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to query SAP BusinessObjects users and groups: "
                    + exception.getMessage());
            request.getRequestDispatcher("/menu.jsp").forward(request, response);
        }
    }
}
