package de.sdsd.projekt.prototype.util;

import java.io.File;
import javax.servlet.Servlet;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.filters.RemoteAddrFilter;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.slf4j.LoggerFactory;

public class TomcatServer implements Runnable {

    //private static final Logger logger = LoggerFactory.getLogger(TomcatServer.class);

    public static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir");
    
    private Tomcat tomcat;

    private File tmpDir;

    private boolean disableClientCaching;
    private ServletCacheControlFilter cacheControlFilter;
    private FilterDef filterDefNoClientCaching;
    private FilterMap filterMapNoClientCaching;

    private boolean restrictToLocalHost;
    private RemoteAddrFilter filter;
    private FilterDef filterDefLocalhostOnly;
    private FilterMap filterMapLocalhostOnly;

    private boolean addAdminBasicLogin;
    
    public TomcatServer(int port, String tempFolder, boolean restrictToLocalHost, boolean disableClientCaching) throws Exception {
        this.tomcat = new Tomcat() {
            // disable session persistence
            @Override
            public Host getHost() {
                Host host = super.getHost();
                if (host == null) {
                    host = new StandardHost() {
                        @Override
                        public void addChild(final Container child) {
                            if (child instanceof StandardContext) {
                                setupContextWithNonpersistentSessionManager(child);
                            }
                            super.addChild(child);
                        }

                        private void setupContextWithNonpersistentSessionManager(final Container child) {
                            StandardManager mgr = new StandardManager();
                            mgr.setPathname(null);
                            //child.setManager(mgr); //TODO since tomcat 8 not avail
                        }
                    };
                    host.setName(hostname);
                    getEngine().addChild(host);
                }
                return host;
            }
        };
        this.tmpDir = new File(tempFolder);
        // set port
        tomcat.setPort(port);
        tomcat.setSilent(true);
        // set localhost restriction (optional)
        this.restrictToLocalHost = restrictToLocalHost;
        if (restrictToLocalHost) {
            //logger.info("restricting tomcat to localhost only");
            filter = new RemoteAddrFilter();
            filter.setAllow("127\\.\\d+\\.\\d+\\.\\d+|::1|0:0:0:0:0:0:0:1");
            filterDefLocalhostOnly = new FilterDef();
            filterDefLocalhostOnly.setFilter(filter);
            filterDefLocalhostOnly.setFilterName("ipcheck");
            filterDefLocalhostOnly.setFilterClass(RemoteAddrFilter.class.getName());
            filterMapLocalhostOnly = new FilterMap();
            filterMapLocalhostOnly.setFilterName("ipcheck");
            filterMapLocalhostOnly.addURLPattern("/*");
        }
        // disable client caching (optional)
        this.disableClientCaching = disableClientCaching;
        if (disableClientCaching) {
            //logger.info("setting tomcat to prevent client caching");
            cacheControlFilter = new ServletCacheControlFilter();
            filterDefNoClientCaching = new FilterDef();
            filterDefNoClientCaching.setFilter(cacheControlFilter);
            filterDefNoClientCaching.setFilterName("noClientCaching");
            filterDefNoClientCaching.setFilterClass(ServletCacheControlFilter.class.getName());
            filterMapNoClientCaching = new FilterMap();
            filterMapNoClientCaching.setFilterName("noClientCaching");
            filterMapNoClientCaching.addURLPattern("/*");
        }
        //https://stackoverflow.com/questions/48998387/code-works-with-embedded-apache-tomcat-8-but-not-with-9-whats-changed
        tomcat.getConnector();
    }

    public TomcatServer(int port, boolean restrictToLocalHost) throws Exception {
        this(port, TEMP_FOLDER, restrictToLocalHost, true);
    }
    
    private void addBasicAdminLoginTo(Context ctx) {
        //https://gist.github.com/n-shinya/5401893
        
        /*
          <security-constraint>
            <web-resource-collection>
               <web-resource-name>Admin</web-resource-name>
               <url-pattern>/admin/*</url-pattern>
             </web-resource-collection>
             <auth-constraint>
               <role-name>admin</role-name>
             </auth-constraint>
          </security-constraint>
        
          <login-config>
                <auth-method>BASIC</auth-method>
               <!-- Please note following line .. its commented -->
               <!-- <realm-name>Admin</realm-name> -->
          </login-config>
        
          <!-- Following section was missing -->
          <security-role>
              <role-name>admin</role-name>
         </security-role>
        */
        
        final String AUTH_ROLE = "admin";
        
        LoginConfig config = new LoginConfig();
        config.setAuthMethod("BASIC");
        
        ctx.setLoginConfig(config);
        
        ctx.addSecurityRole(AUTH_ROLE);
        
        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(AUTH_ROLE);
        
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
       
        ctx.addConstraint(constraint);

        String path = "tomcat-users.xml";
        MemoryRealm realm = new MemoryRealm();
        realm.setPathname(path);
        tomcat.getEngine().setRealm(realm);
        
        ((StandardContext)ctx).addValve(new BasicAuthenticator());
    }
    
    public ResourceServlet addResourceServlet() {
        ResourceServlet rs = new ResourceServlet();
        addServlet("", "/", "ResourceServlet", rs);
        return rs;
    }
    
    public ResourceServlet addResourceServlet(String servletPath, String welcomeFile) {
        ResourceServlet rs = new ResourceServlet(welcomeFile);
        addServlet("", servletPath, "ResourceServlet", rs);
        return rs;
    }
    
    public ResourceServlet addResourceServlet(String servletPath, String welcomeFile, boolean debug) {
        ResourceServlet rs = new ResourceServlet(welcomeFile, debug);
        addServlet("", servletPath, "ResourceServlet", rs);
        return rs;
    }
    
    public void addDefaultServlet(String name, String contextPath, String fileDir, String welcomeFile) {
        StandardContext ctx = (StandardContext) tomcat.addContext(contextPath, fileDir);
        
        if(addAdminBasicLogin)
            addBasicAdminLoginTo(ctx);
        
        //disable creating a temp folder
        //ctx.setCachingAllowed(false); //TODO not avail in tomcat 8
        if (disableClientCaching) {
            ctx.addFilterDef(filterDefNoClientCaching);
            ctx.addFilterMap(filterMapNoClientCaching);
        }
        if (restrictToLocalHost) {
            ctx.addFilterDef(filterDefLocalhostOnly);
            ctx.addFilterMap(filterMapLocalhostOnly);
        }
        Wrapper defServletWrapper = ctx.createWrapper();
        defServletWrapper.setName(name);
        defServletWrapper.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defServletWrapper.addInitParameter("debug", "0");
        defServletWrapper.addInitParameter("listings", "false");
        defServletWrapper.setLoadOnStartup(1);
        ctx.addChild(defServletWrapper);
        ctx.addWelcomeFile(welcomeFile);
        ctx.addServletMappingDecoded("/", name);
        
        // delete unnecessary tomcat folder(s)
        //deleteTomcatTempFolders();
    }

    public Context addServlet(String contextPath, String servletPath, String servletName, Servlet servlet) {
        StandardContext ctx = (StandardContext) tomcat.addContext(contextPath, tmpDir.getAbsolutePath());
        //disable creating a temp folder
        //ctx.setCachingAllowed(false);//TODO not avail in tomcat 8
        ctx.setName(servletName);
        
        if(addAdminBasicLogin)
            addBasicAdminLoginTo(ctx);
        
        if (disableClientCaching) {
            ctx.addFilterDef(filterDefNoClientCaching);
            ctx.addFilterMap(filterMapNoClientCaching);
        }
        if (restrictToLocalHost) {
            ctx.addFilterDef(filterDefLocalhostOnly);
            ctx.addFilterMap(filterMapLocalhostOnly);
        }
        Tomcat.addServlet(ctx, servletName, servlet);
        ctx.addServletMappingDecoded(servletPath, servletName);
        // delete unnecessary tomcat folder(s)
        //deleteTomcatTempFolders();
        return ctx;
    }
    
    public Context addServlet(Servlet servlet) {
        return addServlet("", "/", servlet.getClass().getSimpleName(), servlet);
    }
    
    public Context addContext(String contextPath, String contextName) {
        StandardContext ctx = (StandardContext) tomcat.addContext(contextPath, tmpDir.getAbsolutePath());
        //ctx.setCachingAllowed(false);//TODO not avail in tomcat 8
        ctx.setName(contextName);
        
        if(addAdminBasicLogin)
            addBasicAdminLoginTo(ctx);
        
        if (disableClientCaching) {
            ctx.addFilterDef(filterDefNoClientCaching);
            ctx.addFilterMap(filterMapNoClientCaching);
        }
        if (restrictToLocalHost) {
            ctx.addFilterDef(filterDefLocalhostOnly);
            ctx.addFilterMap(filterMapLocalhostOnly);
        }
        return ctx;
    }
    
    public Servlet addServletToContext(Context ctx, String servletPath, String servletName, Servlet servlet) {
        Tomcat.addServlet(ctx, servletName, servlet);
        ctx.addServletMappingDecoded(servletPath, servletName);
        // delete unnecessary tomcat folder(s)
        //deleteTomcatTempFolders();
        return servlet;
    }

    public void addServlet(String contextPath, String servletPath, String servletName, String docBase, Servlet servlet) {
        Context ctx = tomcat.addContext(contextPath, docBase);
        
        if(addAdminBasicLogin)
            addBasicAdminLoginTo(ctx);
        
        if (disableClientCaching) {
            ctx.addFilterDef(filterDefNoClientCaching);
            ctx.addFilterMap(filterMapNoClientCaching);
        }
        if (restrictToLocalHost) {
            ctx.addFilterDef(filterDefLocalhostOnly);
            ctx.addFilterMap(filterMapLocalhostOnly);
        }
        Tomcat.addServlet(ctx, servletName, servlet);
        ctx.addServletMappingDecoded(servletPath, servletName);
        
        // delete unnecessary tomcat folder(s)
        //deleteTomcatTempFolders();
    }
    
    public void removeContext(String name) {
        Container found = null;
        for(Container c : tomcat.getHost().findChildren()) {
            if(name.equals(c.getName())) {
                found = c;
                break;
            }
        }
        if(found != null) {
            tomcat.getHost().removeChild(found);
        }
        //Container c = tomcat.getHost().findChild(servletName);
    }
    
    public void start() throws LifecycleException {
        tomcat.start();
    }

    public void stop() throws LifecycleException {
        tomcat.stop();
    }

    public void await() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        tomcat.getServer().await();
    }
    
    public static void deleteTomcatTempFolders() {
        for (File f : new File(".").listFiles()) {
            if (f.getName().matches("tomcat\\.\\d+") && f.isDirectory()) {
                FileUtils.deleteQuietly(f);
            }
        }
    }
    
    public static void setLoggerLevelToError() {
        //turn off anoying logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.ERROR);
        //org.apache.catalina.startup.TldConfig.setNoTldJars("*.jar");
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public boolean isAddAdminBasicLogin() {
        return addAdminBasicLogin;
    }

    public void setAddAdminBasicLogin(boolean addAdminBasicLogin) {
        this.addAdminBasicLogin = addAdminBasicLogin;
    }
    
}
