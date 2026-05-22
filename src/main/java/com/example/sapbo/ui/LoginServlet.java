package com.example.sapbo.ui;

import com.crystaldecisions.sdk.exception.SDKException;
import com.example.sapbo.core.BoCredentials;
import com.example.sapbo.core.ConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            BoCredentials credentials = new BoCredentials(
                    request.getParameter("cms"),
                    request.getParameter("username"),
                    request.getParameter("password"));

            try (ConnectionManager connectionManager = new ConnectionManager(credentials)) {
                connectionManager.getSession();
                request.getSession().setAttribute("boCredentials", credentials);
                request.getSession().setAttribute("cms", credentials.getCms());
                response.sendRedirect(request.getContextPath() + "/menu.jsp");
            }
        } catch (IllegalArgumentException exception) {
            request.setAttribute("error", exception.getMessage());
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (SDKException exception) {
            request.setAttribute("error", "Unable to log in to SAP BusinessObjects: " + exception.getMessage());
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }
}
