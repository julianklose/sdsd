/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package de.sdsd.projekt.api;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

/**
 * Embedded HTTP/1.1 file server based on a classic (blocking) I/O model.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public abstract class HttpFileServer {

	/**
	 * Creates the.
	 *
	 * @param docRoot the doc root
	 * @param port    the port
	 * @return the http server
	 */
	public static HttpServer create(String docRoot, int port) {
		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(15000).setTcpNoDelay(true).build();

		final HttpServer server = ServerBootstrap.bootstrap().setListenerPort(port).setServerInfo("Test/1.1")
				.setSocketConfig(socketConfig).setSslContext(null).setExceptionLogger(new StdErrorExceptionLogger())
				.registerHandler("*", new HttpFileHandler(docRoot)).create();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.shutdown(5, TimeUnit.SECONDS);
			}
		});

		return server;
	}

	/**
	 * The Class StdErrorExceptionLogger.
	 */
	static class StdErrorExceptionLogger implements ExceptionLogger {

		/**
		 * Log.
		 *
		 * @param ex the ex
		 */
		@Override
		public void log(final Exception ex) {
			if (ex instanceof SocketTimeoutException) {
				System.err.println("Connection timed out");
			} else if (ex instanceof ConnectionClosedException) {
				System.err.println(ex.getMessage());
			} else {
				ex.printStackTrace();
			}
		}

	}

	/**
	 * The Class HttpFileHandler.
	 */
	static class HttpFileHandler implements HttpRequestHandler {

		/** The doc root. */
		private final String docRoot;

		/**
		 * Instantiates a new http file handler.
		 *
		 * @param docRoot the doc root
		 */
		public HttpFileHandler(final String docRoot) {
			super();
			this.docRoot = docRoot;
		}

		/**
		 * Handle.
		 *
		 * @param request  the request
		 * @param response the response
		 * @param context  the context
		 * @throws HttpException the http exception
		 * @throws IOException   Signals that an I/O exception has occurred.
		 */
		@Override
		public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
				throws HttpException, IOException {

			String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
			if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
				throw new MethodNotSupportedException(method + " method not supported");
			}
			String target = request.getRequestLine().getUri();

			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				byte[] entityContent = EntityUtils.toByteArray(entity);
				System.out.println("Incoming entity content (bytes): " + entityContent.length);
			}

			final File file = new File(this.docRoot, URLDecoder.decode(target, "UTF-8"));
			if (!file.exists()) {

				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				StringEntity entity = new StringEntity(
						"<html><body><h1>File" + file.getPath() + " not found</h1></body></html>",
						ContentType.create("text/html", "UTF-8"));
				response.setEntity(entity);
				System.out.println("File " + file.getPath() + " not found");

			} else if (!file.canRead() || file.isDirectory()) {

				response.setStatusCode(HttpStatus.SC_FORBIDDEN);
				StringEntity entity = new StringEntity("<html><body><h1>Access denied</h1></body></html>",
						ContentType.create("text/html", "UTF-8"));
				response.setEntity(entity);
				System.out.println("Cannot read file " + file.getPath());

			} else {
				HttpCoreContext coreContext = HttpCoreContext.adapt(context);
				HttpConnection conn = coreContext.getConnection(HttpConnection.class);
				response.setStatusCode(HttpStatus.SC_OK);
				FileEntity body = new FileEntity(file, ContentType.create("text/html", (Charset) null));
				response.setEntity(body);
				System.out.println(conn + ": serving file " + file.getPath());
			}
		}

	}

}
