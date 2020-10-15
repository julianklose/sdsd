package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.Validation;

/**
 * Main class of the TTL parser containing the two interface methods
 * {@link #parse(InputStream, OutputStream) parse} and
 * {@link #testServiceResult(InputStream) testServiceResult}. These functions
 * are called using corresponding command line arguments {@code parse} and
 * {@code test}.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * @see #parse(InputStream, OutputStream)
 * @see #testServiceResult(InputStream)
 */
public class MainParser {

	/**
	 * The main method processes the command line arguments passed to the parser.
	 * There are two code paths leading either to the execution of the
	 * {@link #parse(InputStream, OutputStream) parse} or
	 * {@link #testServiceResult(InputStream) testServiceResult} method.
	 *
	 * @param args Command line arguments passed to the parser.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see #parse(InputStream, OutputStream)
	 * @see #testServiceResult(InputStream)
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
				System.exit(testServiceResult(in) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse' or 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}

	/**
	 * The parse method reads a raw input stream pointing to a file's contents and
	 * builds (parses) a Jena RDF {@code Model} object. In this case, the input
	 * stream consists of data in the TTL (Terse Triple Language) format which is
	 * parsed into an RDF model using the {@code Model}'s built in {@code read}
	 * method.
	 * 
	 * After that the parse time is set alongside any potential errors and the RDF
	 * model is written into the triple store using the {@code writeTriples} method
	 * of the SDSD {@code ParserAPI}.
	 *
	 * @param input  The input stream in TTL format to be converted into a Jena RDF
	 *               model.
	 * @param output The Jena RDF model representing the TTL {@code input}.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void parse(final InputStream input, final OutputStream output) throws IOException {
		long t1 = System.nanoTime();
		Model model = ModelFactory.createDefaultModel();
		Validation errors = new Validation();
		try {
			model.read(input, null, "TTL");
		} catch (Throwable e) {
			errors.fatal(e.getMessage());
		}
		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);
			api.writeTriples(model);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * The {@code testServiceResult} method is used to decide whether a given input
	 * stream can be processed by this parser.
	 * 
	 * @param input Input stream to be tested.
	 * @return {True} if this parser is likely capable to parse the provided input
	 *         stream, {@code false} otherwise.
	 */
	public static boolean testServiceResult(InputStream input) {
		try {
			ModelFactory.createDefaultModel().read(input, null, "TTL");
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
}