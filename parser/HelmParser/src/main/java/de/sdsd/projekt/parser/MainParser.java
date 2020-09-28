package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;

/**
 * Main class of the HelmParser.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class MainParser {
	/**
	 * Reads the contents of the file pointed to by {@code args[1]} into an
	 * {@code InputStream}. If no input file is specified, {@code stdin} is used.
	 * After that, an {@code OutputStream} pointing to {@code args[2]} is opened.
	 * 
	 * If the option "parse" {@link #parse(InputStream, OutputStream)} is passed as
	 * first command line argument, the input is parsed and redirected into a (ZIP)
	 * file specified by {@code args[2]} or {@code stdout} otherwise.
	 * 
	 * If the first command line argument is "test"
	 * {@link #test(InputStream, OutputStream)}, the parser tests whether the
	 * provided input can most likely be parsed or not. This check should always be
	 * implemented in a very efficient way.
	 * 
	 * @param args Array of provided command line arguments.
	 * 
	 * @see #parse(InputStream, OutputStream)
	 * @see #test(InputStream, OutputStream)
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;

			switch (args[0].toLowerCase()) {
			case "parse":
				parse(in, out);
				break;
			case "test":
				System.exit(test(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse', 'validate', 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|validate|test output");
	}

	private static void parse(InputStream input, OutputStream output) {
		HelmParser.parse(input, output);
	}

	private static boolean test(InputStream input, OutputStream output) {
		return HelmParser.test(input, output);
	}
}