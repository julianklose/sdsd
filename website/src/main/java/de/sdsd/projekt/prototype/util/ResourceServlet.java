package de.sdsd.projekt.prototype.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class ResourceServlet extends HttpServlet {

    private boolean debug = true;
    private String welcomeFile = null;

    private Map<String, String> symlinks;
    private Map<String, Supplier<byte[]>> virtuals;

    private Function<String, String> pathRewriter = Function.identity();
    
    public ResourceServlet() {
        symlinks = new HashMap<>();
        virtuals = new HashMap<>();
    }

    public ResourceServlet(String welcomeFile) {
        this();
        this.welcomeFile = welcomeFile;
    }

    public ResourceServlet(String welcomeFile, boolean debug) {
        this(welcomeFile);
        this.debug = debug;
    }

    public void addSymlink(String src, String dst) {
        symlinks.put(src, dst);
    }

    public void removeSymlink(String src) {
        symlinks.remove(src);
    }

    public void addVirtual(String src, Supplier<byte[]> supplier) {
        virtuals.put(src, supplier);
    }

    public void removeVirtual(String src, Supplier<byte[]> supplier) {
        virtuals.remove(src);
    }

    public Function<String, String> getPathRewriter() {
        return pathRewriter;
    }

    public void setPathRewriter(Function<String, String> pathRewriter) {
        this.pathRewriter = pathRewriter;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = getRelativePath(req, true);

        path = pathRewriter.apply(path);
        
        if (path.trim().equals("/")) {
            if(welcomeFile == null)
                return;
            
            path = welcomeFile;
        }

        //follow symlink
        if (symlinks.containsKey(path)) {
            path = symlinks.get(path);
        }

        //content
        byte[] content;
        if (virtuals.containsKey(path)) {
            //from virtual
            content = virtuals.get(path).get();
        } else {
            //from resources
            
            //get from resources
            InputStream is = ResourceServlet.class.getResourceAsStream(path);
            if (is == null) {
                if (debug) {
                    System.out.println(Arrays.asList("404", path));
                }

                resp.sendError(404, path + " not found");
                return;
            }

            //data
            content = IOUtils.toByteArray(is);
        }
        String name = FilenameUtils.getName(path);
        String basename = FilenameUtils.getBaseName(path);
        String ext = FilenameUtils.getExtension(path);

        String contentType = getServletContext().getMimeType(name);
        if (contentType == null) {
            contentType = contentTypeByNameExt(basename, ext);
        }

        if (debug) {
            System.out.println(Arrays.asList("OK", path, basename, contentType, content.length));
        }

        resp.setContentType(contentType);
        resp.setStatus(200);
        resp.setContentLength(content.length);

        ServletOutputStream sos = resp.getOutputStream();
        sos.write(content);
        //sos.flush();
    }

    private String contentTypeByNameExt(String basename, String ext) {
        switch (ext) {
            case "js":
                return "text/javascript";
            case "css":
                return "text/css";
            case "html":
                return "text/html";
            case "woff2":
                return "font/woff2";//application/font-woff2
            case "woff":
                return "application/x-font-woff";
            case "ttf":
                return "font/opentype";
            case "ico":
                return "image/x-icon";
            case "png":
                return "image/png";
            case "jpg":
                return "image/jpg";
            case "ttl":
                return "text/turtle";
            case "nt":
                return "application/n-triples";
            case "rdf":
                return "application/rdf+xml";
            case "svg":
                return "image/svg+xml";
        }
        return "text/plain";
    }

    //shameless copied from DefaultServlet
    protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {
        // IMPORTANT: DefaultServlet can be mapped to '/' or '/path/*' but always
        // serves resources from the web app root with context rooted paths.
        // i.e. it can not be used to mount the web app root under a sub-path
        // This method must construct a complete context rooted path, although
        // subclasses can change this behaviour.

        String servletPath;
        String pathInfo;

        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            // For includes, get the info from the attributes
            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
            servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
        } else {
            pathInfo = request.getPathInfo();
            servletPath = request.getServletPath();
        }

        StringBuilder result = new StringBuilder();
        if (servletPath.length() > 0) {
            result.append(servletPath);
        }
        if (pathInfo != null) {
            result.append(pathInfo);
        }
        if (result.length() == 0 && !allowEmptyPath) {
            result.append('/');
        }

        return result.toString();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getWelcomeFile() {
        return welcomeFile;
    }

    public void setWelcomeFile(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }
    
}
