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
 *
 * @author Markus Schr&ouml;der
 */
public class ServerManager {
    
    private TomcatServer tomcatServer;
    private ResourceServlet resourceServlet;
    
    private TrayIcon trayIcon;
    
    private int port;

    //init
    
    public void initServer(int listeningPort) throws Exception {
        this.port = listeningPort;
        // initialize tomcat server
        tomcatServer = new TomcatServer(listeningPort, false);
    }

    public void initResourceServlet(String indexFile) throws Exception {
        //serve html/js/css from resources
        resourceServlet = tomcatServer.addResourceServlet("/", indexFile, false);
    }
    
    public void initResourceServletStatic() throws Exception {
        //serve html/js/css from resources
        Context staticContext = tomcatServer.addContext("/static", "static");
        resourceServlet = new ResourceServlet();
        resourceServlet.setDebug(false);
        tomcatServer.addServletToContext(staticContext, "/", "resourceServlet", resourceServlet);
    }

    //sys tray
    
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
    
    public static Map<String, Class[]> contextPath2endpoints = new HashMap<>();
    
    public void addWebsocketEndpoints(String contextPath, Class... endpoints) {
        contextPath2endpoints.put(contextPath, endpoints);
        
        StandardContext sc = (StandardContext) getTomcatServer().getTomcat().addContext(contextPath, null);
        sc.addApplicationListener(WebsocketEndpointInstaller.class.getName());
        
        Tomcat.addServlet(sc, "default", new DefaultServlet());
        sc.addServletMappingDecoded("/", "default");
    }

    public static class WebsocketEndpointInstaller extends WsContextListener {

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
    
    public void startAndAwaitServer() throws LifecycleException {
        // start tomcat
        tomcatServer.start();
        tomcatServer.await();
    }
    
    public void browseLocalhost(int listeningPort) throws URISyntaxException, IOException {
        // browse to hosted webapp
        Desktop.getDesktop().browse(new URI("http://localhost:" + listeningPort));
    }
    
    //getter
    
    public TomcatServer getTomcatServer() {
        return tomcatServer;
    }

    public ResourceServlet getResourceServlet() {
        return resourceServlet;
    }


    public int getPort() {
        return port;
    }
    
    public static void setLoggerLevelToError() {
        //turn off anoying logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.ERROR);
        //not avail in tomcat 8
        //org.apache.catalina.startup.TldConfig.setNoTldJars("*.jar");
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }
    
}
