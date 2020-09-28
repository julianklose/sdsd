package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import de.sdsd.projekt.api.ParserAPI;

public class MainParser {

	public static void main(String[] args) throws IOException {
		if(args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch(args[0].toLowerCase()) {
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
	
	public static void parse(final InputStream input, final OutputStream output) throws IOException {
		long t1 = System.nanoTime();
		Model model = ModelFactory.createDefaultModel();
		List<String> errors = null;
		try {
			model.read(input, null, "TTL");
		} catch (Throwable e) {
			errors = Collections.singletonList(e.getMessage());
		}
		try (ParserAPI api = new ParserAPI(output)) {
			api.setParseTime((System.nanoTime()-t1)/1000000);
			api.setErrors(errors);
			api.writeTriples(model);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} 
	}
	
	public static boolean testServiceResult(InputStream input) {
		try {
			ModelFactory.createDefaultModel().read(input, null, "TTL");
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

}
