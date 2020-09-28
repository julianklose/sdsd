package de.sdsd.projekt.prototype.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import javax.ws.rs.NotSupportedException;

/**
 * ServletRequest to simulate a HTTP request from a websocket connection.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
class WebsocketServletRequest implements HttpServletRequest {
	private final Cookie[] cookies;
	private final String token;
	
	public WebsocketServletRequest(@Nullable String sdsdSessionId, @Nullable String token) {
		cookies = sdsdSessionId != null ? new Cookie[] { new Cookie("SDSDSESSION", sdsdSessionId) } : new Cookie[0];
		this.token = token;
	}
	
	@Override
	public Cookie[] getCookies() {
		return cookies;
	}
	
	@Override
	public String getHeader(String name) {
		return name.equals("token") ? token : null;
	}

	@Override
	public Object getAttribute(String name) {
		throw new NotSupportedException();
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new NotSupportedException();
	}

	@Override
	public String getCharacterEncoding() {
		throw new NotSupportedException();
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		throw new NotSupportedException();

	}

	@Override
	public int getContentLength() {
		throw new NotSupportedException();
	}

	@Override
	public long getContentLengthLong() {
		throw new NotSupportedException();
	}

	@Override
	public String getContentType() {
		throw new NotSupportedException();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new NotSupportedException();
	}

	@Override
	public String getParameter(String name) {
		throw new NotSupportedException();
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new NotSupportedException();
	}

	@Override
	public String[] getParameterValues(String name) {
		throw new NotSupportedException();
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		throw new NotSupportedException();
	}

	@Override
	public String getProtocol() {
		throw new NotSupportedException();
	}

	@Override
	public String getScheme() {
		throw new NotSupportedException();
	}

	@Override
	public String getServerName() {
		throw new NotSupportedException();
	}

	@Override
	public int getServerPort() {
		throw new NotSupportedException();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new NotSupportedException();
	}

	@Override
	public String getRemoteAddr() {
		throw new NotSupportedException();
	}

	@Override
	public String getRemoteHost() {
		throw new NotSupportedException();
	}

	@Override
	public void setAttribute(String name, Object o) {
		throw new NotSupportedException();

	}

	@Override
	public void removeAttribute(String name) {
		throw new NotSupportedException();

	}

	@Override
	public Locale getLocale() {
		throw new NotSupportedException();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isSecure() {
		throw new NotSupportedException();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new NotSupportedException();
	}

	@Override
	public String getRealPath(String path) {
		throw new NotSupportedException();
	}

	@Override
	public int getRemotePort() {
		throw new NotSupportedException();
	}

	@Override
	public String getLocalName() {
		throw new NotSupportedException();
	}

	@Override
	public String getLocalAddr() {
		throw new NotSupportedException();
	}

	@Override
	public int getLocalPort() {
		throw new NotSupportedException();
	}

	@Override
	public ServletContext getServletContext() {
		throw new NotSupportedException();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new NotSupportedException();
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		throw new NotSupportedException();
	}

	@Override
	public boolean isAsyncStarted() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isAsyncSupported() {
		throw new NotSupportedException();
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new NotSupportedException();
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new NotSupportedException();
	}

	@Override
	public String getAuthType() {
		throw new NotSupportedException();
	}

	@Override
	public long getDateHeader(String name) {
		throw new NotSupportedException();
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		throw new NotSupportedException();
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		throw new NotSupportedException();
	}

	@Override
	public int getIntHeader(String name) {
		throw new NotSupportedException();
	}

	@Override
	public String getMethod() {
		throw new NotSupportedException();
	}

	@Override
	public String getPathInfo() {
		throw new NotSupportedException();
	}

	@Override
	public String getPathTranslated() {
		throw new NotSupportedException();
	}

	@Override
	public String getContextPath() {
		throw new NotSupportedException();
	}

	@Override
	public String getQueryString() {
		throw new NotSupportedException();
	}

	@Override
	public String getRemoteUser() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new NotSupportedException();
	}

	@Override
	public Principal getUserPrincipal() {
		throw new NotSupportedException();
	}

	@Override
	public String getRequestedSessionId() {
		throw new NotSupportedException();
	}

	@Override
	public String getRequestURI() {
		throw new NotSupportedException();
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new NotSupportedException();
	}

	@Override
	public String getServletPath() {
		throw new NotSupportedException();
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new NotSupportedException();
	}

	@Override
	public HttpSession getSession() {
		throw new NotSupportedException();
	}

	@Override
	public String changeSessionId() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new NotSupportedException();
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new NotSupportedException();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new NotSupportedException();
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new NotSupportedException();
	}

	@Override
	public void logout() throws ServletException {
		throw new NotSupportedException();
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new NotSupportedException();
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		throw new NotSupportedException();
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new NotSupportedException();
	}

}
