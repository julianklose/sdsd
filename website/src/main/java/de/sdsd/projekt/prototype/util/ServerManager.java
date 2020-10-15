package de.sdsd.projekt.prototype.util;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.servlet.ServletContextEvent;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.Constants;
import org.apache.tomcat.websocket.server.WsContextListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jabsorb.JSONRPCBridge;
import org.slf4j.LoggerFactory;

/**
 * The Class ServerManager.
 *
 * @author Markus Schr&ouml;der
 */
public class ServerManager {
    
    /** The tomcat server. */
    private TomcatServer tomcatServer;
    
    /** The resource servlet. */
    private ResourceServlet resourceServlet;
    
    /** The tray icon. */
    private TrayIcon trayIcon;
    
    /** The port. */
    private int port;

    //init
    
    /**
     * Inits the server.
     *
     * @param listeningPort the listening port
     * @throws Exception the exception
     */
    public void initServer(int listeningPort) throws Exception {
        this.port = listeningPort;
        // initialize tomcat server
        tomcatServer = new TomcatServer(listeningPort, false);
    }

    /**
     * Inits the resource servlet.
     *
     * @param indexFile the index file
     * @throws Exception the exception
     */
    public void initResourceServlet(String indexFile) throws Exception {
        //serve html/js/css from resources
        resourceServlet = tomcatServer.addResourceServlet("/", indexFile, false);
    }
    
    /**
     * Inits the resource servlet static.
     *
     * @throws Exception the exception
     */
    public void initResourceServletStatic() throws Exception {
        //serve html/js/css from resources
        Context staticContext = tomcatServer.addContext("/static", "static");
        resourceServlet = new ResourceServlet();
        resourceServlet.setDebug(false);
        tomcatServer.addServletToContext(staticContext, "/", "resourceServlet", resourceServlet);
    }

    //sys tray
    
    /**
     * Inits the tray.
     *
     * @param imgResourcePath the img resource path
     * @param tooltip the tooltip
     * @param trayClickURI the tray click URI
     * @param fillPopup the fill popup
     */
    public void initTray(String imgResourcePath, String tooltip, String trayClickURI, Consumer<PopupMenu> fillPopup) {
        URL iconUrl = ServerManager.class.getResource(imgResourcePath);
        Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
        
        final PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(icon, tooltip);
        trayIcon.setPopupMenu(popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> {
            try {
                if(trayClickURI != null)
                    Desktop.getDesktop().browse(new URI(trayClickURI));
            } catch (URISyntaxException | IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        final SystemTray tray = SystemTray.getSystemTray();
        
        MenuItem quitItem = new MenuItem("Quit");
        quitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });
        popup.add(quitItem);
        
        if(fillPopup != null)
            fillPopup.accept(popup);
        
        //show it in tray
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            //System.out.println("TrayIcon could not be added.");
        }
    }
    
    //auth
    
    /**
     * Uses in working directory the tomcat-users.xml file.
     */
    public void withBasicAdminLogin() {
        tomcatServer.setAddAdminBasicLogin(true);
    }
    
    //json rpc
    
    /**
     * Adds the json RPC.
     *
     * @param name the name
     * @param endpoint the endpoint
     */
    public void addJsonRPC(String name, Object endpoint) {
        //JSON RPC used by PRO-OPT Connector
        JSONRPCBridge.getGlobalBridge().registerObject(name, endpoint);
        JSONRPCBridge.getSerializer().setFixupDuplicatePrimitives(false);
        JSONRPCBridge.getSerializer().setFixupDuplicates(false);
        JSONRPCBridge.getSerializer().setMarshallClassHints(false);
        JSONRPCBridge.getSerializer().setMarshallNullAttributes(false);
        tomcatServer.addServlet("/" + name, "/json-rpc", name, new org.jabsorb.JSONRPCServlet());
        //if (settings.isDebug()) {
        //    String endpointURL = "http://localhost:" + settings.getListeningPort() + "/datacatalog/json-rpc";
        //    System.out.println("a JSON RPC endpoint is running at " + endpointURL + " waiting for requests, e.g.:");
        //    System.out.println("{ \"method\": \"DataCatalog.ping\", \"params\": [ ] }");
        //}
    }
    
    //websocket
    
    /** The context path 2 endpoints. */
    public static Map<String, Class[]> contextPath2endpoints = new HashMap<>();
    
    /**
     * Adds the websocket endpoints.
     *
     * @param contextPath the context path
     * @param endpoints the endpoints
     */
    public void addWebsocketEndpoints(String contextPath, Class... endpoints) {
        contextPath2endpoints.put(contextPath, endpoints);
        
        StandardContext sc = (StandardContext) getTomcatServer().getTomcat().addContext(contextPath, null);
        sc.addApplicationListener(WebsocketEndpointInstaller.class.getName());
        
        Tomcat.addServlet(sc, "default", new DefaultServlet());
        sc.addServletMappingDecoded("/", "default");
    }

    /**
     * The Class WebsocketEndpointInstaller.
     */
    public static class WebsocketEndpointInstaller extends WsContextListener {

        /**
         * Context initialized.
         *
         * @param sce the sce
         */
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            super.contextInitialized(sce);

            String contextPath = sce.getServletContext().getContextPath();
            
            Class[] endpoints = contextPath2endpoints.get(contextPath);
            if(endpoints == null) {
                throw new RuntimeException("Could not initialize websocket endpoints for " + contextPath);
            }
            
            ServerContainer sc
                    = (ServerContainer) sce.getServletContext().getAttribute(
                            Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE);
            
            //ServerEndpointConfig.Builder.create(SampleWebSocket.class, "/websocket/echoProgrammatic").configurator(new DefaultServerEndpointConfigurator()).build();
            
            try {
                for (Class endpoint : endpoints) {
                    sc.addEndpoint(endpoint);
                }
            } catch (DeploymentException e) {
                throw new IllegalStateException(e);
            }

        }
    }
    
    //rest
    
    /**
     * Internally calls addServlet with jersy's ServletContainer.
     * @param contextPath e.g. <code>"/rest"</code>
     * @param servletPath e.g. <code>"/*"</code>
     * @param servletName a unique name
     * @param resourceConfigConsumer configure an empty ResourceConfig
     */
    public void addREST(String contextPath, String servletPath, String servletName, Consumer<ResourceConfig> resourceConfigConsumer) {
        ResourceConfig rc = new ResourceConfig();
        resourceConfigConsumer.accept(rc);
        ServletContainer sc = new ServletContainer(rc);
        getTomcatServer().addServlet(contextPath, servletPath, servletName, sc);
    }
    
    /**
     * Adds the REST.
     *
     * @param contextPath the context path
     * @param servletPath the servlet path
     * @param servletName the servlet name
     * @param resourceConfig the resource config
     */
    public void addREST(String contextPath, String servletPath, String servletName, ResourceConfig resourceConfig) {
        ServletContainer sc = new ServletContainer(resourceConfig);
        getTomcatServer().addServlet(contextPath, servletPath, servletName, sc);
    }
    
    /**
     * contextPath is <code>"/rest"</code>, servletPath is <code>"/*"</code>, servletName is <code>"jersey"</code>.
     * @param resourceConfig e.g. <code>public class MyApp extends ResourceConfig { ...</code>
     */
    public void addREST(ResourceConfig resourceConfig) {
        addREST("/rest", "/*", "jersey", resourceConfig);
    }
    
    //run
    
    /**
     * Start and await server.
     *
     * @throws LifecycleException the lifecycle exception
     */
    public void startAndAwaitServer() throws LifecycleException {
        // start tomcat
        tomcatServer.start();
        tomcatServer.await();
    }
    
    /**
     * Browse localhost.
     *
     * @param listeningPort the listening port
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void browseLocalhost(int listeningPort) throws URISyntaxException, IOException {
        // browse to hosted webapp
        Desktop.getDesktop().browse(new URI("http://localhost:" + listeningPort));
    }
    
    //getter
    
    /**
     * Gets the tomcat server.
     *
     * @return the tomcat server
     */
    public TomcatServer getTomcatServer() {
        return tomcatServer;
    }

    /**
     * Gets the resource servlet.
     *
     * @return the resource servlet
     */
    public ResourceServlet getResourceServlet() {
        return resourceServlet;
    }


    /**
     * Gets the port.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Sets the logger level to error.
     */
    public static void setLoggerLevelToError() {
        //turn off anoying logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.ERROR);
        //not avail in tomcat 8
        //org.apache.catalina.startup.TldConfig.setNoTldJars("*.jar");
    }

    /**
     * Gets the tray icon.
     *
     * @return the tray icon
     */
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }
    
}
