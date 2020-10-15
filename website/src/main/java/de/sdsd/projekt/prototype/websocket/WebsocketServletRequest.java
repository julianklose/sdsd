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
	
	/** The cookies. */
	private final Cookie[] cookies;
	
	/** The token. */
	private final String token;
	
	/**
	 * Instantiates a new websocket servlet request.
	 *
	 * @param sdsdSessionId the sdsd session id
	 * @param token the token
	 */
	public WebsocketServletRequest(@Nullable String sdsdSessionId, @Nullable String token) {
		cookies = sdsdSessionId != null ? new Cookie[] { new Cookie("SDSDSESSION", sdsdSessionId) } : new Cookie[0];
		this.token = token;
	}
	
	/**
	 * Gets the cookies.
	 *
	 * @return the cookies
	 */
	@Override
	public Cookie[] getCookies() {
		return cookies;
	}
	
	/**
	 * Gets the header.
	 *
	 * @param name the name
	 * @return the header
	 */
	@Override
	public String getHeader(String name) {
		return name.equals("token") ? token : null;
	}

	/**
	 * Gets the attribute.
	 *
	 * @param name the name
	 * @return the attribute
	 */
	@Override
	public Object getAttribute(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the attribute names.
	 *
	 * @return the attribute names
	 */
	@Override
	public Enumeration<String> getAttributeNames() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the character encoding.
	 *
	 * @return the character encoding
	 */
	@Override
	public String getCharacterEncoding() {
		throw new NotSupportedException();
	}

	/**
	 * Sets the character encoding.
	 *
	 * @param env the new character encoding
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		throw new NotSupportedException();

	}

	/**
	 * Gets the content length.
	 *
	 * @return the content length
	 */
	@Override
	public int getContentLength() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the content length long.
	 *
	 * @return the content length long
	 */
	@Override
	public long getContentLengthLong() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the content type.
	 *
	 * @return the content type
	 */
	@Override
	public String getContentType() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the input stream.
	 *
	 * @return the input stream
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new NotSupportedException();
	}

	/**
	 * Gets the parameter.
	 *
	 * @param name the name
	 * @return the parameter
	 */
	@Override
	public String getParameter(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the parameter names.
	 *
	 * @return the parameter names
	 */
	@Override
	public Enumeration<String> getParameterNames() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the parameter values.
	 *
	 * @param name the name
	 * @return the parameter values
	 */
	@Override
	public String[] getParameterValues(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the parameter map.
	 *
	 * @return the parameter map
	 */
	@Override
	public Map<String, String[]> getParameterMap() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the protocol.
	 *
	 * @return the protocol
	 */
	@Override
	public String getProtocol() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the scheme.
	 *
	 * @return the scheme
	 */
	@Override
	public String getScheme() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the server name.
	 *
	 * @return the server name
	 */
	@Override
	public String getServerName() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the server port.
	 *
	 * @return the server port
	 */
	@Override
	public int getServerPort() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the reader.
	 *
	 * @return the reader
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		throw new NotSupportedException();
	}

	/**
	 * Gets the remote addr.
	 *
	 * @return the remote addr
	 */
	@Override
	public String getRemoteAddr() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the remote host.
	 *
	 * @return the remote host
	 */
	@Override
	public String getRemoteHost() {
		throw new NotSupportedException();
	}

	/**
	 * Sets the attribute.
	 *
	 * @param name the name
	 * @param o the o
	 */
	@Override
	public void setAttribute(String name, Object o) {
		throw new NotSupportedException();

	}

	/**
	 * Removes the attribute.
	 *
	 * @param name the name
	 */
	@Override
	public void removeAttribute(String name) {
		throw new NotSupportedException();

	}

	/**
	 * Gets the locale.
	 *
	 * @return the locale
	 */
	@Override
	public Locale getLocale() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the locales.
	 *
	 * @return the locales
	 */
	@Override
	public Enumeration<Locale> getLocales() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is secure.
	 *
	 * @return true, if is secure
	 */
	@Override
	public boolean isSecure() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the request dispatcher.
	 *
	 * @param path the path
	 * @return the request dispatcher
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the real path.
	 *
	 * @param path the path
	 * @return the real path
	 */
	@Override
	public String getRealPath(String path) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the remote port.
	 *
	 * @return the remote port
	 */
	@Override
	public int getRemotePort() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the local name.
	 *
	 * @return the local name
	 */
	@Override
	public String getLocalName() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the local addr.
	 *
	 * @return the local addr
	 */
	@Override
	public String getLocalAddr() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the local port.
	 *
	 * @return the local port
	 */
	@Override
	public int getLocalPort() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the servlet context.
	 *
	 * @return the servlet context
	 */
	@Override
	public ServletContext getServletContext() {
		throw new NotSupportedException();
	}

	/**
	 * Start async.
	 *
	 * @return the async context
	 * @throws IllegalStateException the illegal state exception
	 */
	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new NotSupportedException();
	}

	/**
	 * Start async.
	 *
	 * @param servletRequest the servlet request
	 * @param servletResponse the servlet response
	 * @return the async context
	 * @throws IllegalStateException the illegal state exception
	 */
	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is async started.
	 *
	 * @return true, if is async started
	 */
	@Override
	public boolean isAsyncStarted() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is async supported.
	 *
	 * @return true, if is async supported
	 */
	@Override
	public boolean isAsyncSupported() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the async context.
	 *
	 * @return the async context
	 */
	@Override
	public AsyncContext getAsyncContext() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the dispatcher type.
	 *
	 * @return the dispatcher type
	 */
	@Override
	public DispatcherType getDispatcherType() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the auth type.
	 *
	 * @return the auth type
	 */
	@Override
	public String getAuthType() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the date header.
	 *
	 * @param name the name
	 * @return the date header
	 */
	@Override
	public long getDateHeader(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the headers.
	 *
	 * @param name the name
	 * @return the headers
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the header names.
	 *
	 * @return the header names
	 */
	@Override
	public Enumeration<String> getHeaderNames() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the int header.
	 *
	 * @param name the name
	 * @return the int header
	 */
	@Override
	public int getIntHeader(String name) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the method.
	 *
	 * @return the method
	 */
	@Override
	public String getMethod() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the path info.
	 *
	 * @return the path info
	 */
	@Override
	public String getPathInfo() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the path translated.
	 *
	 * @return the path translated
	 */
	@Override
	public String getPathTranslated() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the context path.
	 *
	 * @return the context path
	 */
	@Override
	public String getContextPath() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the query string.
	 *
	 * @return the query string
	 */
	@Override
	public String getQueryString() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the remote user.
	 *
	 * @return the remote user
	 */
	@Override
	public String getRemoteUser() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is user in role.
	 *
	 * @param role the role
	 * @return true, if is user in role
	 */
	@Override
	public boolean isUserInRole(String role) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the user principal.
	 *
	 * @return the user principal
	 */
	@Override
	public Principal getUserPrincipal() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the requested session id.
	 *
	 * @return the requested session id
	 */
	@Override
	public String getRequestedSessionId() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the request URI.
	 *
	 * @return the request URI
	 */
	@Override
	public String getRequestURI() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the request URL.
	 *
	 * @return the request URL
	 */
	@Override
	public StringBuffer getRequestURL() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the servlet path.
	 *
	 * @return the servlet path
	 */
	@Override
	public String getServletPath() {
		throw new NotSupportedException();
	}

	/**
	 * Gets the session.
	 *
	 * @param create the create
	 * @return the session
	 */
	@Override
	public HttpSession getSession(boolean create) {
		throw new NotSupportedException();
	}

	/**
	 * Gets the session.
	 *
	 * @return the session
	 */
	@Override
	public HttpSession getSession() {
		throw new NotSupportedException();
	}

	/**
	 * Change session id.
	 *
	 * @return the string
	 */
	@Override
	public String changeSessionId() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is requested session id valid.
	 *
	 * @return true, if is requested session id valid
	 */
	@Override
	public boolean isRequestedSessionIdValid() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is requested session id from cookie.
	 *
	 * @return true, if is requested session id from cookie
	 */
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is requested session id from URL.
	 *
	 * @return true, if is requested session id from URL
	 */
	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new NotSupportedException();
	}

	/**
	 * Checks if is requested session id from url.
	 *
	 * @return true, if is requested session id from url
	 */
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new NotSupportedException();
	}

	/**
	 * Authenticate.
	 *
	 * @param response the response
	 * @return true, if successful
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ServletException the servlet exception
	 */
	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new NotSupportedException();
	}

	/**
	 * Login.
	 *
	 * @param username the username
	 * @param password the password
	 * @throws ServletException the servlet exception
	 */
	@Override
	public void login(String username, String password) throws ServletException {
		throw new NotSupportedException();
	}

	/**
	 * Logout.
	 *
	 * @throws ServletException the servlet exception
	 */
	@Override
	public void logout() throws ServletException {
		throw new NotSupportedException();
	}

	/**
	 * Gets the parts.
	 *
	 * @return the parts
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ServletException the servlet exception
	 */
	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new NotSupportedException();
	}

	/**
	 * Gets the part.
	 *
	 * @param name the name
	 * @return the part
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ServletException the servlet exception
	 */
	@Override
	public Part getPart(String name) throws IOException, ServletException {
		throw new NotSupportedException();
	}

	/**
	 * Upgrade.
	 *
	 * @param <T> the generic type
	 * @param handlerClass the handler class
	 * @return the t
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ServletException the servlet exception
	 */
	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new NotSupportedException();
	}

}
