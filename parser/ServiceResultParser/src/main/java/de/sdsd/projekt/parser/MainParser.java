package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Main class of the ServiceResult parser containing the two interface methods
 * {@link #copy(InputStream, OutputStream) copy} and
 * {@link #testServiceResult(InputStream, OutputStream) testServiceResult}.
 * These functions are called using corresponding command line arguments
 * {@code parse} and {@code test}.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * @see #copy(InputStream, OutputStream)
 * @see #testServiceResult(InputStream, OutputStream)
 */
public class MainParser {

	/**
	 * The main method processes the command line arguments passed to the parser.
	 * There are two code paths leading either to the execution of the
	 * {@link #copy(InputStream, OutputStream) copy} or
	 * {@link #testServiceResult(InputStream) testServiceResult} method.
	 *
	 * @param args Command line arguments passed to the parser.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see #copy(InputStream, OutputStream)
	 * @see #testServiceResult(InputStream)
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch (args[0].toLowerCase()) {
			case "parse":
				copy(in, out);
				break;
			case "test":
				System.exit(testServiceResult(in, out) ? 0 : 1);
				break;
			default:
				System.err.println("No parser specified ('parse' or 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}

	/**
	 * The {@code copy} method represents the {@code parse} method seen in other
	 * parser implementations. In this case however, the input stream {@code input}
	 * containing the results of a service is not parsed but simply copied to the
	 * output stream {@code output} and later displayed on the SDSD web platform.
	 *
	 * @param input  Input stream to be copied to the output stream.
	 * @param output Output stream containing the contents of the input stream after
	 *               copying has finished.
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void copy(final InputStream input, final OutputStream output) throws IOException {
		try {
			IOUtils.copy(input, output);
		} finally {
			input.close();
			output.close();
		}
	}

	/**
	 * The {@code testServiceResult} method is used to decide whether a given input
	 * stream can be processed by this parser.
	 * 
	 * @param input  Input stream to be tested.
	 * @param output Unused
	 * @return {True} if this parser is likely capable to process the provided input
	 *         stream, {@code false} otherwise.
	 */
	public static boolean testServiceResult(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input)) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName().toLowerCase();
				if (name.equals("meta.json"))
					return true;
			}
		} catch (IOException e) {
		}
		return false;
	}
}