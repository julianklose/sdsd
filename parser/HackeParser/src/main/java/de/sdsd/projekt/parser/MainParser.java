package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDFS;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.TimeLog;
import de.sdsd.projekt.api.ParserAPI.TimeLogWriter;
import de.sdsd.projekt.api.ParserAPI.ValueInfo;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiFormat;

/**
 * Class for executing Parser for Hacke data.
 * 
 * @author ngs
 *
 */
public class MainParser {
	/**
	 * Main function executes test or parse function depending on given arguments.
	 * 
	 * @param args specifies names of files for input and output
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
				System.err.println("No parser specified ('parse', 'validate', 'test', 'isoxml')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|validate|test|isoxml filepath");
	}
	
	private static final WikiFormat FORMAT = Util.format("hacke");

	public static final String TLGNAME = "Hacke", MOCOT = "Mocot", DICOT = "Dicot";
	private static final double SCALE = 0.0001;
	
	private static Long toValue(Double val) {
		if(val == null || val.isNaN() || val.isInfinite()) 
			return null;
		return Math.round(val / SCALE);
	}
	
	/**
	 * This function parses HackeData from input Stream and writes included TimeLogs
	 * to TimeLog store of SDSD. The included mocot and dicot values are written as
	 * values with TimeLogWriter. To represent not setted values, Double.NaN is used
	 * by TimeLogWriter write function. TimeLogWriter also writes data to output
	 * stream. The used WikiNormia types are Mocot and Dicot and their format is
	 * hackedata. The data type is csv.
	 * 
	 * 
	 * @param input  used input stream for parse function
	 * @param output used output type, written by TimeLogWriter
	 */
	public static void parse(InputStream input, OutputStream output) {

		try (ParserAPI api = new ParserAPI(output)) {
			List<String> errors = new ArrayList<>();
			long t1 = System.nanoTime();

			try {
				List<HackeData> hdp = new HackeDataParser(input).getData();
				
				Model model = ModelFactory.createDefaultModel();
				TimeLog timelog = new TimeLog(Util.createRandomUri(), TLGNAME,
						Instant.ofEpochMilli(hdp.get(0).getTime()),
						Instant.ofEpochMilli(hdp.get(hdp.size()-1).getTime()),
						hdp.size());
				
				timelog.writeTo(model, FORMAT.res("log"))
						.addLiteral(RDFS.label, Util.lit(TLGNAME));
				
				ValueInfo mocot = new ValueInfo(Util.createRandomUri())
						.addTimeLog(timelog)
						.setDesignator(MOCOT)
						.setScale(SCALE)
						.setNumberOfDecimals(4)
						.setUnit("%");
				mocot.writeTo(model, FORMAT.res("mocot"))
						.addLiteral(RDFS.label, Util.lit(MOCOT));
				
				ValueInfo dicot = new ValueInfo(Util.createRandomUri())
						.addTimeLog(timelog)
						.setDesignator(DICOT)
						.setScale(SCALE)
						.setNumberOfDecimals(4)
						.setUnit("%");
				mocot.writeTo(model, FORMAT.res("dicot"))
						.addLiteral(RDFS.label, Util.lit(DICOT));
				
				api.writeTriples(model);
				

				try (TimeLogWriter tlw = api.addTimeLog(timelog, Arrays.asList(mocot, dicot))) {
					Long[] values = new Long[2];
					for (HackeData hd : hdp) {
						if (hd.getTime() != null && hd.getLat() != null && hd.getLon() != null && hd.getAlt() != null) {
							values[0] = toValue(hd.getMocot());
							values[1] = toValue(hd.getDicot());
							tlw.write(Instant.ofEpochMilli(hd.getTime()), hd.getLat(), hd.getLon(), hd.getAlt(),
									values);
						}

					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				errors.add(e.getMessage());
			}

			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}

	}

	/**
	 * This function uses specified InputStream for testing if data is parsable or
	 * not. It checks, if the string that specifies data is included.
	 * 
	 * @param input  specifies input stream to use with test function.
	 * @param output specifies stream to write errors.
	 * @return Boolean, whether test was successful or not.
	 */
	public static boolean test(InputStream input, OutputStream output) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
			try {
				String content = IOUtils.toString(input, StandardCharsets.UTF_8);
				if (content.contains(
						"TIME;LON;LAT;ALT;SECTION;LON_HEAD;LAT_HEAD;STATUS;RESULTID;SYSTIME;BEAVP;BRSNN;DICOT;GALAP;MATIN;MOCOT;TRZAW;ZEAMX;")) {
					return true;
				} else {
					out.println("String for specification of fields not found.");
				}

			} catch (Throwable e) {
				out.println(e.getMessage());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

}
