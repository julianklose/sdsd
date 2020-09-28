package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class MainParser {

	public static void main(String[] args) throws IOException {
		if(args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch(args[0].toLowerCase()) {
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
	
	public static void copy(final InputStream input, final OutputStream output) throws IOException {
		try {
			IOUtils.copy(input, output);
		} finally {
			input.close();
			output.close();
		}
	}
	
	public static boolean testServiceResult(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input)) {
			ZipEntry entry;
			while((entry = in.getNextEntry()) != null) {
				if(entry.isDirectory()) continue;
				String name = entry.getName().toLowerCase();
				if(name.equals("meta.json"))
					return true;
			}
		} catch (IOException e) {}
		return false;
	}

}
