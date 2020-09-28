package de.sdsd.projekt.prototype;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.jsonrpc.AdminEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.AgrirouterEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.ApiEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.DashboardEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.FeldgrenzenEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.FileEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.FormatEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.JsonRpcEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.MapEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.ServiceEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.SimulatorEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.SmartDataBrowserEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.TelemetryEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.UserEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.ValidatorEndpoint;
import de.sdsd.projekt.prototype.jsonrpc.WikinormiaEndpoint;
import de.sdsd.projekt.prototype.util.ServerManager;
import de.sdsd.projekt.prototype.websocket.WebsocketEndpoint;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.jabsorb.JSONRPCBridge;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * Startup of the SDSD webserver.
 *
 * @author Markus Schr&ouml;der
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class Main {

	/**
	 * If detailed information about sent requests and received results is shown.
	 */
	public static final boolean DEBUG_MODE = "true".equalsIgnoreCase(System.getProperty("detailedDebugMode"));

	/**
	 * Start the SDSD webserver.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// CLI Options

		Settings settings = new Settings();
		settings.process(args);

		// Server

		// org.apache.catalina.startup.TldConfig.setNoTldJars("*.jar");
		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(Level.WARN);

		ServerManager serverManager = new ServerManager();
		serverManager.initServer(settings.getPort());

		JSONObject appSettings = new JSONObject(
				FileUtils.readFileToString(new File(settings.getConfigFile()), StandardCharsets.UTF_8));
		// serverManager.withBasicAdminLogin();
		ApplicationLogic restApplication = new ApplicationLogic(appSettings);

		serverManager.addREST(restApplication);

		restApplication.registerEndpoint("user", new UserEndpoint(restApplication));
		restApplication.registerEndpoint("file", new FileEndpoint(restApplication));
		restApplication.registerEndpoint("agrirouter", new AgrirouterEndpoint(restApplication));
		restApplication.registerEndpoint("admin", new AdminEndpoint(restApplication));
		restApplication.registerEndpoint("service", new ServiceEndpoint(restApplication));
		restApplication.registerEndpoint("telemetry", new TelemetryEndpoint(restApplication));
		restApplication.registerEndpoint("validator", new ValidatorEndpoint(restApplication));
		restApplication.registerEndpoint("simulator", new SimulatorEndpoint(restApplication));
		restApplication.registerEndpoint("api", new ApiEndpoint(restApplication));
		restApplication.registerEndpoint("wikinormia", new WikinormiaEndpoint(restApplication));
		restApplication.registerEndpoint("format", new FormatEndpoint(restApplication));
		restApplication.registerEndpoint("map", new MapEndpoint(restApplication));
		restApplication.registerEndpoint("sdb", new SmartDataBrowserEndpoint(restApplication));
		restApplication.registerEndpoint("dashboard", new DashboardEndpoint(restApplication));
		restApplication.registerEndpoint("feldgrenzen", new FeldgrenzenEndpoint(restApplication));

		for (Entry<String, JsonRpcEndpoint> endpoint : restApplication.getEndpoints()) {
			serverManager.addJsonRPC(endpoint.getKey(), endpoint.getValue());
		}

		JSONRPCBridge.getGlobalBridge().setExceptionTransformer(JsonRpcEndpoint::exceptionTransformer);

		serverManager.addWebsocketEndpoints("/websocket", WebsocketEndpoint.class);
		// connect to ws://localhost:8081/websocket/sdsd

		serverManager.initResourceServlet("/home.html");

		// we will use stardog
		// serverManager.initServerSparql();
		// serverManager.addSparqlEndpoint("sdsd", restApplication.getDataset());

		// start
		serverManager.startAndAwaitServer();

		System.out.println("server is running at " + settings.getPort() + " on host " + settings.getHost() + "...");

	}

}
