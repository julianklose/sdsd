package de.sdsd.projekt.prototype;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Start options for the SDSD server.
 * 
 * @author Markus Schr&ouml;der
 */
public class Settings {

	/** The options. */
	private Options options;

	/** The host. */
	private String host;

	/** The port. */
	private int port;

	/** The config file. */
	private String configFile;

	/**
	 * Instantiates a new settings.
	 */
	public Settings() {
		port = 8081;
		host = "http://localhost:" + port;
		configFile = "settings.json";

		options = new Options();
		options.addOption("h", "help", false, "prints this help");
		options.addOption("l", "listening-port", true, "listening port, default: " + port);
		options.addOption("H", "host", true, "serving host (with http(s), without ending /), default: " + host);
		options.addOption("c", "config", true, "path to the configuration file, default: " + configFile);
	}

	/**
	 * Process.
	 *
	 * @param args the args
	 * @throws ParseException the parse exception
	 */
	public void process(String[] args) throws ParseException {
		// parse it
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		// help
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar sdsd.jar", options);
			System.exit(0);
		}

		// set options
		if (cmd.hasOption("l"))
			port = Integer.parseInt(cmd.getOptionValue("l"));
		if (cmd.hasOption("H"))
			host = cmd.getOptionValue("H");
		if (cmd.hasOption("c"))
			configFile = cmd.getOptionValue("c");
	}

	/**
	 * Gets the host.
	 *
	 * @return the host
	 */
	public String getHost() {
		return host;
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
	 * Gets the config file.
	 *
	 * @return the config file
	 */
	public String getConfigFile() {
		return configFile;
	}

}
