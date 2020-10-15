package de.sdsd.projekt.prototype.jsonrpc;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import static de.sdsd.projekt.prototype.jsonrpc.JsonRpcEndpoint.getSessionId;
import de.sdsd.projekt.prototype.smartdatabrowser.SmartDataBrowser;
import de.sdsd.projekt.prototype.smartdatabrowser.SmartDataBrowser.SDBResponse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 * Endpoint to access the Smart Data Browser functionality.
 *
 * @author Markus Schr&ouml;der
 * @see SmartDataBrowser
 */
public class SmartDataBrowserEndpoint extends JsonRpcEndpoint {

    /** The smart data browser. */
    //the actual smart data browser implementation
    private SmartDataBrowser smartDataBrowser;

    /** The server address. */
    //address of the server for generating the links correctly
    private String serverAddress;

    /**
     * Instantiates a new smart data browser endpoint.
     *
     * @param application the application
     */
    public SmartDataBrowserEndpoint(ApplicationLogic application) {
        super(application);

        serverAddress = "https://app.sdsd-projekt.de";

        JSONObject stardog = application.getSettings().getJSONObject("stardog");
        String sparqlQueryEndpoint = stardog.getString("query");
        String user = stardog.getString("user");
        String password = stardog.getString("password");

        smartDataBrowser = SmartDataBrowser.serverLess(sparqlQueryEndpoint, user, password, serverAddress, 100);
    }

    /**
     * Browses a given url with the smart data browser.
     * This calles {@link #browse(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String) } with "application/json" as accept header key.
     *
     * @param req the request filled by Jabsorb.
     * @param urlPart the actual url part that is browsed
     * @return a JSON representation of the rendered result.
     * @throws JsonRpcException the json rpc exception
     */
    public JSONObject browse(HttpServletRequest req, String urlPart) throws JsonRpcException {
        //return browse(req, urlPart, "text/html");
        return browse(req, urlPart, "application/json");
    }

    /**
     * Browses a given url with the smart data browser.
     *
     * @param req the request filled by Jabsorb.
     * @param urlPart the actual url part that is browsed
     * @param accept a mime type what view should be used to render the result (e.g. text/json).
     * @return a JSON representation of the rendered result.
     * @throws JsonRpcException the json rpc exception
     */
    public JSONObject browse(HttpServletRequest req, String urlPart, String accept) throws JsonRpcException {
        User user = null;
        try {
            user = application.getUser(getSessionId(req));
            if (user == null) {
                throw new NoLoginException();
            }

            Set<String> graphs = application.triple.getGraphs(user);

            JSONObject jsonResp = new JSONObject();

            String urlStr = serverAddress + urlPart;

            URL url;
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                jsonResp.put("error", ex.getMessage());
                return jsonResp;
            }

            Map<String, Object> ctx = smartDataBrowser.contextFromURL(url);
            //Context restCtx = new Context(url);

            //check if allowed
            if (ctx.containsKey(SmartDataBrowser.GRAPH)) {
                String graphUri = (String) ctx.get(SmartDataBrowser.GRAPH);
                if (!graphs.contains(graphUri)) {
                    throw new SDSDException("no access to graph " + graphUri);
                }
            }

            //permission related infos
            List<File> files = application.list.files.getList(user);
            files.sort(File.CMP_RECENT);

            ctx.put("username", user.getName());
            ctx.put("wikinormia", TripleFunctions.TBOX);
            ctx.put("userGraphUri", user.getGraphUri());
            ctx.put("userGraphs", graphs);
            ctx.put("userFiles", files);
            ctx.put("Accept", accept);

            SDBResponse resp = new SmartDataBrowser.SDBResponse();

            String body;
            try {
                body = (String) smartDataBrowser.processView(ctx, resp);

                if (accept.equals("text/html")) {
                    jsonResp.put("html", body);
                } else if (accept.equals("application/json")) {
                    jsonResp.put("json", new JSONObject(body));
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new SDSDException(e.getMessage());
            }

            return jsonResp;

        } catch (Throwable e) {
            throw createError(user, e);
        }
    }

    /**
     * Instead of the browse method, this method receives the view name and parameters 
     * via arguments (not via URL query parameters).
     * This way the dashboard can easier call smart data browser views.
     *
     * @param req the request filled by Jabsorb.
     * @param view name of the view
     * @param params parameters as JSON object.
     * @param accept a mime type what view should be used to render the result (e.g. text/json).
     * @return a JSON representation of the rendered result.
     * @throws JsonRpcException the json rpc exception
     */
    public JSONObject get(HttpServletRequest req, String view, JSONObject params, String accept) throws JsonRpcException {
        User user = null;
        try {
            user = application.getUser(getSessionId(req));
            if (user == null) {
                throw new NoLoginException();
            }

            Set<String> graphs = application.triple.getGraphs(user);

            JSONObject jsonResp = new JSONObject();

            Map<String, Object> ctx = new HashMap<>();
            ctx.put(SmartDataBrowser.VIEW, view);

            ctx.put("params", params.toMap());

            //check if allowed
            if (ctx.containsKey(SmartDataBrowser.GRAPH)) {
                Object graph = ctx.get(SmartDataBrowser.GRAPH);
                if (graph instanceof String) {
                    String graphUri = (String) ctx.get(SmartDataBrowser.GRAPH);
                    if (!graphs.contains(graphUri)) {
                        throw new SDSDException("no access to graph " + graphUri);
                    }
                } else if (graph instanceof List<?>) {
                    for (Object g : (List<?>) graph) {
                        if (!graphs.contains(g)) {
                            throw new SDSDException("no access to graph " + g);
                        }
                    }
                }
            }

            //permission related infos
            List<File> files = application.list.files.getList(user);
            files.sort((o1, o2) -> {
                return o2.getCreated().compareTo(o1.getCreated());
            });

            ctx.put("username", user.getName());
            ctx.put("wikinormia", TripleFunctions.TBOX);
            ctx.put("userGraphUri", user.getGraphUri());
            ctx.put("userGraphs", graphs);
            ctx.put("userFiles", files);
            ctx.put("Accept", accept);

            SDBResponse resp = new SmartDataBrowser.SDBResponse();

            String body;
            try {
                body = (String) smartDataBrowser.processView(ctx, resp);

                if (accept.equals("text/html")) {
                    jsonResp.put("html", body);
                } else if (accept.equals("application/json")) {
                    jsonResp.put("json", new JSONObject(body));
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new SDSDException(e.getMessage());
            }

            return jsonResp;

        } catch (Throwable e) {
            throw createError(user, e);
        }
    }

}
