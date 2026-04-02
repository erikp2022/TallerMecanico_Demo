package com.taller.mecanico.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Bloquea el acceso a vistas bajo {@code /views/*} sin sesión autenticada.
 * Solo {@code login.xhtml} es accesible sin autenticación.
 */
public class AuthFilter implements Filter {

    private static final String LOGIN_VIEW = "/views/login.xhtml";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.endsWith(LOGIN_VIEW) || path.endsWith("login.xhtml")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            String ctx = req.getContextPath();
            ((HttpServletResponse) response).sendRedirect(ctx + LOGIN_VIEW);
            return;
        }
        chain.doFilter(request, response);
    }
}
