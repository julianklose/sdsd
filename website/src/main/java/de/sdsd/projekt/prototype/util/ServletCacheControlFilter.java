package de.sdsd.projekt.prototype.util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * The Class ServletCacheControlFilter.
 */
public class ServletCacheControlFilter implements Filter {

    /**
     * Do filter.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
    }

    /**
     * Inits the.
     *
     * @param arg0 the arg 0
     * @throws ServletException the servlet exception
     */
    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }
}
