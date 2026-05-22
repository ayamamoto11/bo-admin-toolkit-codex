package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.core.DebugResult;
import com.example.sapbo.modules.users.UserDebugModule;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/users-groups/debug")
public class UserDebugServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BoCredentials credentials = (BoCredentials) request.getSession().getAttribute("boCredentials");
        if (credentials == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            UserDebugModule module = new UserDebugModule(connectionManager);
            DebugResult debugResult = module.inspectUser(userId);
            request.setAttribute("debugTitle", "User Debug");
            request.setAttribute("debugResult", debugResult);
            request.setAttribute("cms", credentials.getCms());
            request.getRequestDispatcher("/object-debug.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            request.setAttribute("error", "User ID must be a number.");
            request.getRequestDispatcher("/debug-tools.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to inspect user: " + exception.getMessage());
            request.getRequestDispatcher("/debug-tools.jsp").forward(request, response);
        }
    }
}
