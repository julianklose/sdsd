package de.sdsd.projekt.prototype.smartdatabrowser;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import spark.HaltException;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;
import spark.utils.MimeParse;

/**
 * Implementation of the smart data browser which renders RDF using SPARQL and 
 * template files.
 * @author Markus Schr&ouml;der
 */
public class SmartDataBrowser {

    //constants
    private static final String APPNAME = "smart-data-browser";
    private static final String SITENAME = "Smart Data Browser";
    private static final int DEFAULT_PORT = (int) 'S' * 100 + (int) 'D';
    
    //cmd parsing
    private CommandLine cmd;
    private static final CommandLineParser parser = new DefaultParser();
    private static final Options options = new Options();
    static {
        options.addOption("h", "help", false, "prints this help");
        options.addOption("s", "sparql", true, "the sparql endpoint, e.g. http://example.com/sparql");
        options.addOption("f", "file", true, "a turtle file that is used to query (instead of sparql endpoint with -s option)"); //use f instead of s (u,p)
        options.addOption("u", "user", true, "in case of -s a username for authentication");
        options.addOption("p", "password", true, "in case of -s a password for authentication");
        options.addOption("a", "serveraddr", true, "the address of the smart data browser server to contruct valid links");
        options.addOption("P", "port", true, "the port of the smart data browser server (default: "+DEFAULT_PORT+")");
        options.addOption("N", "no-server", false, "does not start a server: useful for using smart data browser as a library");
        options.addOption("I", "no-intro", false, "does not show intro screen on startup: useful for using smart data browser as a library");
        options.addOption("l", "limit", true, "default limit value if query parameter is not given");
        options.addOption("vf", "view-folder", true, "set the folder where the views are loaded");
        options.addOption("wf", "web-folder", true, "set the folder where the views are loaded");
        options.addOption("ds", "debug-sparql", false, "prints all SPARQL queries");
        options.addOption("dc", "debug-context", false, "prints context");
    }

    private FreeMarkerEngine freeMarkerEngine;

    private static final boolean DEBUG_MODE = "true".equalsIgnoreCase(System.getProperty("detailedDebugMode"));
    
    private int port;
    private static boolean debugContext = DEBUG_MODE;
    private static boolean debugSparql = DEBUG_MODE;
    
    //use arg '-l'
    private int defaultPageLimit;
    
    private File viewsFolder;
    private File webFolder;
    private File prefixFile;

    private String sparqlService;
    private String serverAddress;
    private CloseableHttpClient httpClient;

    private Dataset dataset;

    //all logged in users
    //private Map<String, String> authKey2nickname;
    public SmartDataBrowser(String[] args) {
        initCmd(args);
        initFreemarker();
        
        if (cmd.hasOption("sparql")) {
            initSparqlHttp();
        }
        if(cmd.hasOption("file")) {
            initFiles();
        }
        if (!cmd.hasOption("no-server")) {
            initRoutes();
        }
    }

    public static SmartDataBrowser serverLess(String sparqlEndpoint, String username, String password, String serverAddress, int limit) {
        return new SmartDataBrowser(new String[]{
            "-s", sparqlEndpoint,
            "-u", username,
            "-p", password,
            "-a", serverAddress,
            "-l", String.valueOf(limit),
            //"-ds", //no debug
            //"-dc",
            "-N", //no server
            "-I" //no intro
        });
    }

    //==========================================================================
    
    //inits
    /**
     * Inits command line interface and exits if help is shown.
     *
     * @param args
     */
    private void initCmd(String[] args) {
        try {
            //parse it
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

        //help
        if (cmd.hasOption("h")) {
            new HelpFormatter().printHelp(APPNAME, options);
            System.exit(0);
        }

        if (!(cmd.hasOption("sparql") || cmd.hasOption("file"))) {
            throw new RuntimeException("option sparql (-s) or file (-f) required");
        }

        sparqlService = cmd.getOptionValue("sparql");
        port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(DEFAULT_PORT)));
        serverAddress = cmd.getOptionValue("serveraddr", "http://localhost:" + port);
        defaultPageLimit = Integer.parseInt(cmd.getOptionValue("limit", "-1"));
        
        debugSparql = cmd.hasOption("debug-sparql");
        debugContext = cmd.hasOption("debug-context");
        
        //use debug mode from outside propery: detailedDebugMode
        debugContext = DEBUG_MODE;
        debugSparql = DEBUG_MODE;

        //folders
        viewsFolder = new File(cmd.getOptionValue("view-folder", "./view"));
        viewsFolder.mkdir();
        //prefix file
        prefixFile = new File(viewsFolder, "prefix.sparql");
        if (!prefixFile.exists()) {
            try {
                prefixFile.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        //web folder
        if(!cmd.hasOption("no-server")) {
            webFolder = new File(cmd.getOptionValue("web-folder", "./web"));
            webFolder.mkdir();
        }
        
        if (!cmd.hasOption("no-intro")) {
            System.out.println(logo);
            System.out.println();
            //state
            System.out.println("View Folder: " + viewsFolder.getAbsolutePath());
            if(!cmd.hasOption("no-server")) {
                System.out.println("Server Address: " + serverAddress);
                System.out.println("Web Folder: " + webFolder.getAbsolutePath());
            }
            //target
            if (cmd.hasOption("sparql")) {
                System.out.println("SPARQL-Endpoint: " + sparqlService);
            }
            if (cmd.hasOption("user")) {
                System.out.println("User: " + cmd.getOptionValue("user"));
            }
            if (cmd.hasOption("file")) {
                for (String path : cmd.getOptionValues("file")) {
                    System.out.println("File: " + path);
                }
            }
            //general
            System.out.println("Debug-Context: " + debugContext);
            System.out.println("Debug-SPARQL: " + debugSparql);
            System.out.println("Default-Limit: " + defaultPageLimit);
            System.out.println();
        }
    }

    /**
     * http settings for sparql.
     */
    private void initSparqlHttp() {
        HttpClientBuilder builder = HttpClientBuilder.create();

        //if credentials provided
        if (cmd.hasOption("user")) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    cmd.getOptionValue("user", "admin"),
                    cmd.getOptionValue("password", "admin"))
            );

            builder.setDefaultCredentialsProvider(credsProvider);
        }

        //build
        httpClient = builder.build();
    }

    /**
     * Inits freemarker template engine to render SPARQL results.
     */
    private void initFreemarker() {
        //template (freemarker)
        Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_28);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        freemarkerConfig.setLogTemplateExceptions(false);
        try {
            //freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(SmartDataBrowser.class, ROOT_PATH + "/tmpl"));
            freemarkerConfig.setTemplateLoader(new FileTemplateLoader(viewsFolder));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        freeMarkerEngine = new FreeMarkerEngine(freemarkerConfig);
    }

    /**
     * Inits alls routes that can be used via HTTP REST.
     */
    private void initRoutes() {
        Spark.port(port);

        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            response.body(exception.getMessage());
        });

        //folder with css js html etc
        spark.Spark.staticFiles.externalLocation(webFolder.getAbsolutePath());

        //in user realm only authenticated can access
        //spark.Spark.before("/user/:username", this::beforeCheckAuthenticated);
        //spark.Spark.before("/user/:username/*", this::beforeCheckAuthenticated);
        //if ends with '/' redirect to path without '/'
        spark.Spark.before((req, res) -> {
            String path = req.pathInfo();
            if (!path.equals("/") && path.endsWith("/")) {
                res.redirect(path.substring(0, path.length() - 1));
            }
        });

        Spark.get("/", this::getRoot);
        Spark.get("/:view", this::getRoot);
    }

    /**
     * Inits all files by creating a jena model.
     */
    private void initFiles() {
        long beginUsed = usedMemory();
        long begin = System.currentTimeMillis();

        dataset = DatasetFactory.create();

        for (String path : cmd.getOptionValues("file")) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }

            try (FileReader r = new FileReader(file)) {
                dataset.getDefaultModel().read(r, null, "TTL");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        long end = System.currentTimeMillis();
        long elapsed = end - begin;

        long endUsed = usedMemory();

        System.out.println("dataset default graph has " + dataset.getDefaultModel().size() + " triples, "
                + FileUtils.byteCountToDisplaySize(endUsed - beginUsed) + " used, " + elapsed + " ms");
    }

    private long usedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    //==========================================================================
    public static final String VIEW = "view"; //decides HOW the uri is viewed

    public static final String URI = "uri"; //decides WHAT should be presented
    public static final String GRAPH = "graph"; //decides FROM WHERE it should be presented

    public static final String LIMIT = "limit"; //decides HOW MANY should be presented 
    public static final String OFFSET = "offset";

    public static final String LANG = "lang"; //decides IN WHICH LANGUAGE it should be presented

    public static final String QUERY_PARAMS = "queryParams"; //Set<String>
    public static final String QUERY_PARAMS_VALUES = "queryParamsValues"; //Map<String, String[]>

    private static final String RESPONSE_EXT = ".response";
    private static final String SPARQL_EXT = ".sparql";

    //via sparkjava route
    private Object getRoot(Request req, Response resp) {

        //fill context
        Map<String, Object> ctx = getDefaultModel(req);
        for (String header : req.headers()) {
            ctx.put(header, req.headers(header));
        }

        Map<String, String[]> queryParamsValues = new HashMap<>();
        for (String param : req.queryParams()) {
            ctx.put(param, req.queryParams(param));
            ctx.put(param + "s", Arrays.asList(req.queryParamsValues(param)));

            queryParamsValues.put(param, req.queryParamsValues(param));
        }

        ctx.put(QUERY_PARAMS, req.queryParams());
        ctx.put(QUERY_PARAMS_VALUES, queryParamsValues);

        String viewPathParam = req.params("view");
        if (viewPathParam != null) {
            ctx.put(VIEW, viewPathParam);
        }
        
        return processView(ctx, resp);
    }

    public Map<String, Object> contextFromURL(URL url) {
        Map<String, Object> ctx = new HashMap<>();

        Map<String, List<String>> query = splitQuery(url);

        Map<String, String[]> queryParamsValues = new HashMap<>();

        ctx.put(QUERY_PARAMS, query.keySet());

        for (String param : query.keySet()) {
            List<String> l = query.get(param);
            l.removeIf(s -> s == null);
            queryParamsValues.put(param, l.toArray(new String[0]));

            if (!l.isEmpty()) {
                ctx.put(param, l.get(0));
                ctx.put(param + "s", l);
            }
        }

        ctx.put(QUERY_PARAMS_VALUES, queryParamsValues);

        String[] split = url.getPath().split("\\/");
        if(split.length >= 2) {
            ctx.put(VIEW, split[1]);
        }
        
        return ctx;
    }

    //via lib call
    public Object processView(Map<String, Object> ctx, Response resp) throws HaltException {

        if (defaultPageLimit > 0) {
            ctx.computeIfAbsent(LIMIT, s -> String.valueOf(defaultPageLimit));
        }
        ctx.computeIfAbsent(VIEW, s -> "noview");

        //per process view a new util class
        ctx.put("util", new SmartDataBrowserUtil(ctx));

        if (debugContext) {
            print(ctx);
        }

        //select view folder
        File viewFolder = new File(this.viewsFolder, (String) ctx.get(VIEW));
        if (!viewFolder.exists()) {
            if (cmd.hasOption("no-server")) {
                throw new RuntimeException(ctx.get(VIEW) + " view does not exist");
            } else {
                throw Spark.halt(HttpStatus.NOT_FOUND_404, ctx.get(VIEW) + " view does not exist");
            }
        }

        //supported mime types
        List<String> supportedMimeTypes = new ArrayList<>();
        File[] responseArray = viewFolder.listFiles((file) -> {
            return file.getName().endsWith(RESPONSE_EXT);
        });
        if (responseArray != null) {
            for (File file : responseArray) {
                String name = file.getName().substring(0, file.getName().length() - RESPONSE_EXT.length());
                name = filename2mimetype(name);
                supportedMimeTypes.add(name);
            }
        }
        //no response
        if (supportedMimeTypes.isEmpty()) {
            if (cmd.hasOption("no-server")) {
                throw new RuntimeException("no supported mime type in view " + ctx.get(VIEW));
            } else {
                throw Spark.halt(HttpStatus.NOT_ACCEPTABLE_406, "no supported mime type in view " + ctx.get(VIEW));
            }
        }

        //decide on the mime type and select the response template
        String mimeType = MimeParse.bestMatch(supportedMimeTypes, (String) ctx.getOrDefault(HttpHeader.ACCEPT.asString(), "text/html"));
        String mimeTypeFilename = mimetype2filename(mimeType);
        String responseTemplate = (String) ctx.get(VIEW) + "/" + mimeTypeFilename + ".response";

        //flat SPARQL
        executeTreeSPARQL(viewFolder, ctx);

        //render resonse
        //resp.header(HttpHeader.CONTENT_TYPE.asString(), mimeType);
        resp.type(mimeType);
        String response = render(ctx, responseTemplate);
        return response;
    }

    private void executeTreeSPARQL(File viewFolder, Map<String, Object> ctx) {
        final String splitSymbol = "_";
        
        TreeNode root = new TreeNode();
        
        //search for sparql queries
        File[] sparqlFileArray = viewFolder.listFiles((file) -> {
            return file.getName().endsWith(SPARQL_EXT);
        });
        if (sparqlFileArray != null) {
            //flat sparql execution
            for (File file : sparqlFileArray) {
                String name = file.getName().substring(0, file.getName().length() - SPARQL_EXT.length());
                
                String[] parts = name.split("\\" + splitSymbol);
                
                TreeNode cur = root;
                String curName = "";
                for(String part : parts) {
                    
                    //build name again
                    if(!curName.isEmpty())
                        curName += splitSymbol;
                    curName += part;
                    
                    //find node or create it
                    if(cur.childMap.containsKey(curName)) {
                        cur = cur.childMap.get(curName);
                    } else {
                        TreeNode child = new TreeNode(viewFolder, curName, curName + SPARQL_EXT);
                        cur.childMap.put(curName, child);
                        cur = child;
                    }
                }
            }
        }
        
        if(debugSparql)
            System.out.println(root.toStringTree());
        
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while(!q.isEmpty()) {
            //first one is root
            TreeNode parent = q.poll();
            //the children will be executed
            //because then we have parent and child in one call
            for(TreeNode child : parent.getChildren()) {
                
                //execute with parent
                child.exec(parent, ctx);
                
                //call child's children later
                q.add(child);
            }
        }
        
        
        int a = 0;
    }

    private class TreeNode {

        private Map<String, TreeNode> childMap;
        private File viewFolder;
        private String filename;
        private String name;
        
        private List<CustomQuerySolution> result;

        //root
        public TreeNode() {
            childMap = new HashMap<>();
        }
        
        public TreeNode(File viewFolder, String name, String filename) {
            this();
            this.viewFolder = viewFolder;
            this.name = name;
            this.filename = filename;
        }
        
        public void exec(TreeNode parent, Map<String, Object> ctx) {
            //there was a query before
            if(parent.result != null) {
                if(debugSparql)
                    System.out.println(parent + " -> " + this + " executed " + parent.result.size() + " times (SPARQL DEBUG is skipped)");
               
                boolean oldDebugSparql = debugSparql;
                debugSparql = false;
                
                for(CustomQuerySolution qs : parent.result) {
                    exec(parent, qs, ctx);
                }
                
                debugSparql = oldDebugSparql;
                
            } else {
                if(debugSparql)
                    System.out.println(parent + " -> " + this);
                exec(parent, (CustomQuerySolution) null, ctx);
            }
        }
        
        private void exec(TreeNode parent, CustomQuerySolution parentQs, Map<String, Object> ctx) {
            
            SmartDataBrowserUtil util = (SmartDataBrowserUtil) ctx.get("util");
            
            //make a copy
            //because the only the child has to know the query solution of the parent for query construction
            Map<String, Object> ctxCopy = new HashMap<>(ctx);
            
            //a child has access to the parents query solution
            //the query can now use ${parent.get('var')} to get the value in this case
            if(parentQs != null) {
                ctxCopy.put(parent.name, parentQs);
            }
            
            //flag if query is empty
            boolean empty = false;
            
            //get the query
            ParameterizedSparqlString pss = null;
            try {
                pss = getQuery(viewFolder, filename, ctxCopy);
            } catch (EmptyQueryException e) {
                //if empty: continue
                //but give empty result
                empty = true;
                result = new ArrayList<>();
            }
            
            //we store the result because there could be child
            //we transform them to CustomQuerySolutions
            if(!empty) {
                if(debugSparql) {
                    System.out.println("reasoning: " + util.isReasoning());
                }
                
                result = CustomQuerySolution.transform(query(pss, util.isReasoning()));
            }
            
            //the child result is again a table
            //if there is a parent we have to put the result-table in the parent's query solution
            //thus a renderer can call <#list parent as qs> <#list qs.list('parent_child') as childqs> 
            if(parentQs != null) {
                //only parent has to know the child result
                parentQs.varName2qslist.put(name, result);
            } else {
                //root context has to know the query result
                ctx.put(name, result);
            }
        }

        @Override
        public String toString() {
            if(name == null)
                return "root";
            
            return name;
        }
        
        public String toStringTree() {
            StringBuilder sb = new StringBuilder();
            toStringTree("", true, sb);
            return sb.toString();
        }

        private void toStringTree(String prefix, boolean isTail, StringBuilder sb) {
            List<TreeNode> children = getChildren();
            
            sb.append(prefix).append(isTail ? "└── " : "├── ").append(toString()).append("\n");
            
            for (int i = 0; i < children.size() - 1; i++) {
                children.get(i).toStringTree(prefix + (isTail ? "    " : "│   "), false, sb);
            }
            
            if (children.size() > 0) {
                children.get(children.size() - 1).toStringTree(prefix + (isTail ? "    " : "│   "), true, sb);
            }
        }

        private List<TreeNode> getChildren() {
            return childMap.entrySet().stream().sorted(Entry.comparingByKey()).map(e -> e.getValue()).collect(toList());//new ArrayList<>(childMap.values());
        }
        
    }

    //==========================================================================
    //helper
    private void print(Map<String, Object> ctx) {
        for (String key : ctx.keySet().stream().sorted().toArray(i -> new String[i])) {
            System.out.println(key + " = " + ctx.get(key));
        }
        //System.out.println("reasoning = " + ((SmartDataBrowserUtil)ctx.get("util")).isReasoning());
        System.out.println();
    }

    private String filename2mimetype(String mimetype) {
        return mimetype.replaceAll("\\_", "/");
    }

    private String mimetype2filename(String filename) {
        return filename.replaceAll("\\/", "_");
    }

    private String render(Map<String, Object> ctx, String templateName) {
        return freeMarkerEngine.render(new ModelAndView(ctx, templateName));
    }

    private QueryExecution queryExec(ParameterizedSparqlString pss, boolean reasoning) {
        return queryExec(pss.asQuery(), reasoning);
    }

    private QueryExecution queryExec(Query query, boolean reasoning) {
        if (sparqlService != null) {
            return QueryExecutionFactory.sparqlService(sparqlService + (reasoning ? "/reasoning" : ""), query, httpClient);
        } else {
            return QueryExecutionFactory.create(query, dataset);
        }
    }

    private UpdateProcessor updateExec(ParameterizedSparqlString pss) {
        if (debugSparql) {
            System.out.println(pss.toString().trim());
            System.out.println();
        }

        return UpdateExecutionFactory.createRemote(pss.asUpdate(), sparqlService, httpClient);
    }

    private List<QuerySolution> query(ParameterizedSparqlString pss, boolean reasoning) {
        return query(pss.asQuery(), reasoning);
    }

    private List<QuerySolution> query(Query query, boolean reasoning) {
        
        long begin = System.currentTimeMillis();
        if (debugSparql) {
            System.out.println(query.toString().trim());
        }
        
        QueryExecution qe = queryExec(query, reasoning);
        List<QuerySolution> qss = ResultSetFormatter.toList(qe.execSelect());

        long end = System.currentTimeMillis();
        if (debugSparql) {
            System.out.println(qss.size() + " results in " + (end - begin) + " ms");
            System.out.println();
        }

        qe.close();
        return qss;
    }

    private boolean ask(ParameterizedSparqlString pss, boolean reasoning) {
        QueryExecution qe = queryExec(pss, reasoning);

        boolean result = qe.execAsk();

        if (debugSparql) {
            System.out.println(pss.toString().trim());
            System.out.println("answer: " + result);
            System.out.println();
        }

        qe.close();

        return result;
    }

    private ParameterizedSparqlString getQuery(File viewFolder, String sparqlFilename, Map<String, Object> ctx) {
        ParameterizedSparqlString pss;
        try {
            String prefix = FileUtils.readFileToString(prefixFile, StandardCharsets.UTF_8).trim();
            //String query = FileUtils.readFileToString(new File(viewFolder, sparqlFilename), StandardCharsets.UTF_8).trim();
            String query = render(ctx, viewFolder.getName() + "/" + sparqlFilename);
            //now reasoning could be activated with ${util.reasoning()}
            if (query.trim().isEmpty()) {
                throw new EmptyQueryException();
            }
            //dynamic prefixes
            //prefix += "\nPREFIX srv: <" + serverAddress + ">";
            pss = new ParameterizedSparqlString(prefix + "\n" + query);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return pss;
    }

    private class EmptyQueryException extends RuntimeException {

    }
    
    private Map<String, Object> getDefaultModel(Request req) {
        Map<String, Object> m = new HashMap<>();
        m.put("sitename", SITENAME);
        m.put("selfhref", address(req.uri()));
        return m;
    }

    private String address(Request req) {
        return serverAddress + req.uri();
    }

    private String address(String path) {
        return serverAddress + path;
    }

    private String userGraph(Request req) {
        return serverAddress + "/user/" + req.params("username");
    }

    private String userGraph(String username) {
        return serverAddress + "/user/" + username;
    }

    /**
     * Removes the last path segment of the uri.
     *
     * @param path
     * @return
     */
    private String up(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    private String ontologyAddress(String localname) {
        return serverAddress + "/ontology/" + localname;
    }

    //==========================================================================
    
    public class SmartDataBrowserUtil {

        private Map<String, Object> ctx;
        
        private boolean reasoning;

        public SmartDataBrowserUtil(Map<String, Object> ctx) {
            this.ctx = ctx;
        }

        //use build() and LinkBuilder
        @Deprecated
        public String link(String... keyValuePairs) {

            //build map
            Map<String, List<String>> m = new HashMap<>();
            for (String param : (Set<String>) ctx.get(QUERY_PARAMS)) {
                List<String> values = m.computeIfAbsent(param, s -> new ArrayList<>());
                values.addAll(Arrays.asList(((Map<String, String[]>) ctx.get(QUERY_PARAMS_VALUES)).get(param)));
            }

            //replace
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                //if null: delete if exist
                if (keyValuePairs[i + 1] == null) {
                    m.remove(keyValuePairs[i]);
                } else {
                    m.put(keyValuePairs[i], Arrays.asList(keyValuePairs[i + 1]));
                }

                if (keyValuePairs[i].equals(URI)) {
                    m.remove(LIMIT);
                    m.remove(OFFSET);
                }
            }

            //build link
            StringBuilder sb = new StringBuilder();
            sb.append(serverAddress);
            boolean first = true;
            if (!m.isEmpty()) {
                sb.append("/?");

                for (String param : m.keySet()) {
                    if (!first) {
                        sb.append("&");
                    }
                    first = false;

                    for (String value : m.get(param)) {
                        sb.append(param);
                        sb.append("=");
                        sb.append(EncodingUtil.encodeURIComponent(value));
                    }
                }
            }
            return sb.toString();
        }

        //deprecated because link() method deprecated
        @Deprecated
        private String pageLink(BiFunction<Integer, Integer, Integer> offsetFunc) {
            String offset = (String) ctx.getOrDefault(OFFSET, "0");
            String limit = (String) ctx.getOrDefault(LIMIT, String.valueOf(defaultPageLimit));

            int o = Integer.parseInt(offset);
            int l = Integer.parseInt(limit);

            o = offsetFunc.apply(o, l);

            return link(
                    LIMIT, String.valueOf(l),
                    OFFSET, String.valueOf(o)
            );
        }

        public String nextPageLink() {
            return pageLink((o, l) -> {
                return o + l;
            });
        }

        public String prevPageLink() {
            return pageLink((o, l) -> {
                o = o - l;
                if (o < 0) {
                    o = 0;
                }
                return o;
            });
        }

        public String firstPageLink() {
            return pageLink((o, l) -> {
                return 0;
            });
        }

        
        public void reasoning() {
            reasoning = true;
        }

        public boolean isReasoning() {
            return reasoning;
        }
        
        public LinkBuilder build() {
            return new LinkBuilder(ctx);
        }
        
        public LinkBuilder build(String view) {
            return new LinkBuilder(ctx).view(view);
        }
        
        public List<QuerySolution> pivotSPO(List<QuerySolution> input) {
            //before
            //s  p  o
            //----------
            //s1 p1 v1
            //s1 p2 v2
            //s1 p3 v3
            //s2 p1 v4
            //s2 p2 v5

            //after:
            //pivot p1  p2  p3
            //s1    v1  ..  ..
            //s2    ..  ..  ..
            /*
            Set<Resource> properties = new HashSet<>();
            for(QuerySolution qs : input) {
                properties.add(qs.getResource("p"));
            }
            List<Resource> sortedProperties = properties.stream().sorted().collect(toList());
             */
            List<QuerySolution> result = new ArrayList<>();

            Map<Resource, QuerySolutionMap> subject2qs = new HashMap<>();
            for (QuerySolution qs : input) {
                QuerySolutionMap qsm = subject2qs.computeIfAbsent(qs.getResource("s"), s -> new QuerySolutionMap());

                qsm.add("s", qs.getResource("s"));
                qsm.add(qs.getResource("p").getURI(), qs.get("o"));
                qsm.add(qs.getResource("p").getLocalName(), qs.get("o"));
            }

            result.addAll(subject2qs.values());
            return result;
        }

        public List<List<RDFNode>> pivotTable(List<QuerySolution> input) {
            //before
            //s  p  o
            //----------
            //s1 p1 v1
            //s1 p2 v2
            //s1 p3 v3
            //s2 p1 v4
            //s2 p2 v5

            //after:
            //pivot p1  p2  p3
            //s1    v1  ..  ..
            //s2    ..  ..  ..
            List<List<RDFNode>> rowsOfCols = new ArrayList<>();

            Map<Resource, Literal> prop2label = new HashMap<>();

            Set<Resource> properties = new HashSet<>();
            for (QuerySolution qs : input) {
                if (qs.contains("p")) {
                    properties.add(qs.getResource("p"));

                    if (qs.contains("p_label")) {
                        prop2label.put(qs.getResource("p"), qs.getLiteral("p_label"));
                    }
                }
            }
            List<RDFNode> sortedProperties = properties.stream().sorted((o1, o2) -> {
                return o1.getURI().compareTo(o2.getURI());
            }).map(r -> (RDFNode) r).collect(toList());

            //replace with property name?
            //header line
            sortedProperties.add(0, null);
            rowsOfCols.add(sortedProperties);

            //each subject a row
            Map<Resource, List<RDFNode>> subject2row = new HashMap<>();
            for (QuerySolution qs : input) {
                List<RDFNode> row = subject2row.computeIfAbsent(qs.getResource("s"), s -> new ArrayList());
                //if new row put s in it and row in result
                if (row.isEmpty()) {
                    row.add(qs.getResource("s"));
                    rowsOfCols.add(row);
                }
            }

            //build a propery subject object map
            Map<Resource, Map<Resource, RDFNode>> psoMap = new HashMap<>();
            for (QuerySolution qs : input) {
                if (qs.contains("p")) {
                    Map<Resource, RDFNode> m = psoMap.computeIfAbsent(qs.getResource("p"), p -> new LinkedHashMap<>());
                    //because of LinkedHashMap ordered subjects
                    m.putIfAbsent(qs.get("s").asResource(), qs.get("o"));
                }
            }
            
            //each property
            for (RDFNode prop : sortedProperties) {
                if(prop == null)
                    continue;
                
                Map<Resource, RDFNode> m = psoMap.get(prop.asResource());
                
                //each qs
                if(m != null) {
                    for(Entry<Resource, RDFNode> e : m.entrySet()) {
                        List<RDFNode> row = subject2row.get(e.getKey());
                        //can be null if not avail
                        row.add(e.getValue());
                    }
                }
            }
            
            //put in the property literals if possible
            RDFNode[] firstRow = rowsOfCols.get(0).toArray(new RDFNode[0]);
            for(int i = 0; i < firstRow.length; i++) {
                Literal label = prop2label.get(firstRow[i]);
                if(label != null) {
                    rowsOfCols.get(0).set(i, label);
                }
            }

            return rowsOfCols;
        }
    }
    
    public class LinkBuilder {
        
        private Map<String, List<String>> param2Values;

        public LinkBuilder(Map<String, Object> ctx) {
            param2Values = new HashMap<>();
            for (String param : (Set<String>) ctx.get(QUERY_PARAMS)) {
                List<String> values = param2Values.computeIfAbsent(param, s -> new ArrayList<>());
                values.addAll(Arrays.asList(((Map<String, String[]>) ctx.get(QUERY_PARAMS_VALUES)).get(param)));
            }
        }
        
        public LinkBuilder clear() {
            param2Values.clear();
            return this;
        }
        
        public LinkBuilder add(String param, Object value) {
            if(value == null)
                return this;
            
            param2Values.computeIfAbsent(param, p -> new ArrayList<>()).add(String.valueOf(value));
            return this;
        }
        
        public LinkBuilder addUnique(String param, Object value) {
            if(value == null)
                return this;
            
            String v = String.valueOf(value);
            List<String> l = param2Values.computeIfAbsent(param, p -> new ArrayList<>());
            if(!l.contains(v))
                l.add(v);
            return this;
        }
        
        public LinkBuilder put(String param, Object value) {
            if(value == null)
                return this;
            
            param2Values.put(param, new ArrayList<>(Arrays.asList(String.valueOf(value))));
            return this;
        }
        
        public LinkBuilder remove(String param) {
            param2Values.remove(param);
            return this;
        }
        
        public LinkBuilder remove(String param, Object value) {
            if(value == null)
                return this;
            
            if(param2Values.containsKey(param)) {
                param2Values.get(param).remove(String.valueOf(value));
            }
            return this;
        }
        
        public LinkBuilder view(String view) {
            put(VIEW, view);
            return this;
        }
        
        public String link() {
            //build link
            StringBuilder sb = new StringBuilder();
            sb.append(serverAddress);
            boolean first = true;
            if (!param2Values.isEmpty()) {
                sb.append("/?");

                for (String param : param2Values.keySet()) {

                    for (String value : param2Values.get(param)) {
                        if (!first) {
                            sb.append("&");
                        }
                        first = false;
                        
                        sb.append(param);
                        sb.append("=");
                        sb.append(EncodingUtil.encodeURIComponent(value));
                    }
                }
            }
            return sb.toString();
        }
    }

    public static class SDBResponse extends Response {

        private String contentType;
        private Map<String, String> headers;

        public SDBResponse() {
            headers = new HashMap<>();
        }

        @Override
        public void type(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public String type() {
            return this.contentType;
        }

        @Override
        public void header(String header, String value) {
            headers.put(header, value);
        }

    }

    /**
     * Utility class for JavaScript compatible UTF-8 encoding and decoding.
     *
     * @see
     * http://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-output
     * @author John Topley
     */
    private static class EncodingUtil {

        /**
         * Decodes the passed UTF-8 String using an algorithm that's compatible
         * with JavaScript's <code>decodeURIComponent</code> function. Returns
         * <code>null</code> if the String is <code>null</code>.
         *
         * @param s The UTF-8 encoded String to be decoded
         * @return the decoded String
         */
        public static String decodeURIComponent(String s) {
            if (s == null) {
                return null;
            }

            String result = null;

            try {
                result = URLDecoder.decode(s, "UTF-8");
            } // This exception should never occur.
            catch (UnsupportedEncodingException e) {
                result = s;
            }

            return result;
        }

        /**
         * Encodes the passed String as UTF-8 using an algorithm that's
         * compatible with JavaScript's <code>encodeURIComponent</code>
         * function. Returns <code>null</code> if the String is
         * <code>null</code>.
         *
         * @param s The String to be encoded
         * @return the encoded String
         */
        public static String encodeURIComponent(String s) {
            String result = null;

            try {
                result = URLEncoder.encode(s, "UTF-8")
                        .replaceAll("\\+", "%20")
                        .replaceAll("\\%21", "!")
                        .replaceAll("\\%27", "'")
                        .replaceAll("\\%28", "(")
                        .replaceAll("\\%29", ")")
                        .replaceAll("\\%7E", "~");
            } // This exception should never occur.
            catch (UnsupportedEncodingException e) {
                result = s;
            }

            return result;
        }

        /**
         * Private constructor to prevent this class from being instantiated.
         */
        private EncodingUtil() {
            super();
        }
    }

    public Map<String, List<String>> splitQuery(URL url) {
        if (url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(this::splitQueryParameter)
                .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    public SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, EncodingUtil.decodeURIComponent(value));
    }

    //public static void main(String[] args) {
    //    new SmartDataBrowser(args);
    //}

    private static String logo = "   _____                      __     ____        __           \n"
            + "  / ___/____ ___  ____ ______/ /_   / __ \\____ _/ /_____ _    \n"
            + "  \\__ \\/ __ `__ \\/ __ `/ ___/ __/  / / / / __ `/ __/ __ `/    \n"
            + " ___/ / / / / / / /_/ / /  / /_   / /_/ / /_/ / /_/ /_/ /     \n"
            + "/____/_/ /_/ /_/\\__,_/_/   \\__/  /_____/\\__,_/\\__/\\__,_/      \n"
            + "    ____                                                      \n"
            + "   / __ )_________ _      __________  _____                   \n"
            + "  / __  / ___/ __ \\ | /| / / ___/ _ \\/ ___/                   \n"
            + " / /_/ / /  / /_/ / |/ |/ (__  )  __/ /                       \n"
            + "/_____/_/   \\____/|__/|__/____/\\___/_/";
}
