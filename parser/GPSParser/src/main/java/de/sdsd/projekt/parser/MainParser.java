package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import com.google.protobuf.Timestamp;

import agrirouter.technicalmessagetype.Gps.GPSList;
import agrirouter.technicalmessagetype.Gps.GPSList.GPSEntry;
import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.TimeLog;
import de.sdsd.projekt.api.ParserAPI.TimeLogWriter;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.ParserAPI.ValueInfo;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;

/**
 * Main class of the GPS parser containing the standardized interface methods
 * {@link #parse(InputStream, OutputStream) parse} and
 * {@link #test(InputStream, OutputStream) test}. These functions are called
 * using corresponding command line arguments.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * @see #parse(InputStream, OutputStream)
 * @see #test(InputStream, OutputStream)
 */
public class MainParser {

	/**
	 * The main method processes the command line arguments passed to the parser.
	 * There are two code paths leading either to the execution of the
	 * {@link #parse(InputStream, OutputStream) parse} or
	 * {@link #test(InputStream, OutputStream) test} method.
	 *
	 * @param args Command line arguments passed to the parser.
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @see #parse(InputStream, OutputStream)
	 * @see #test(InputStream, OutputStream)
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
				System.err.println("No parser specified ('parse', 'test')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|test filepath");
	}

	/**
	 * Utility method converting a protobuf {@code Timestamp} to a high resolution
	 * {@code Instant} object (including nanoseconds).
	 *
	 * @param time Protobuf {@code Timestamp} to be converted into an
	 *             {@code Instant} object.
	 * @return the instant
	 */
	public static Instant toInstant(Timestamp time) {
		return Instant.ofEpochSecond(time.getSeconds(), time.getNanos());
	}

	/** The Constant FORMAT. */
	private static final WikiFormat FORMAT = new WikiFormat("gps");

	/** The Constant INFO. */
	private static final WikiType LOG = FORMAT.res("log"), INFO = FORMAT.res("info");

	/**
	 * The parse method reads a raw input stream pointing to a file's contents and
	 * builds (parses) a Jena RDF {@code Model} object. The aforementioned RDF model
	 * has to honor the structure dictated by the Wikinormia.
	 * 
	 * GPS entries are extracted from the input stream using the protobuf function
	 * {@code parseFrom} provided by the {@code GPSList} class. After that the
	 * {@code GPSEntries} are converted into {@code TimeLogs} by extracting relevant
	 * information like time and GPS positions. Metadata contained by the
	 * {@code GPSEntries} are parsed into objects of type {@code ValueInfo} and
	 * written to the triple store.
	 * 
	 * @param input  The input stream to be parsed.
	 * @param output The output stream containing the parsed RDF model information.
	 */
	public static void parse(InputStream input, OutputStream output) {
		try (ParserAPI api = new ParserAPI(output)) {
			Validation errors = new Validation();
			long t1 = System.nanoTime();

			try {
				List<GPSEntry> list = GPSList.parseFrom(input).getGpsEntriesList();

				Model model = ModelFactory.createDefaultModel();
				TimeLog tlg = new TimeLog("sdsd:gps", "GPS", toInstant(list.get(0).getGpsUtcTimestamp()),
						toInstant(list.get(list.size() - 1).getGpsUtcTimestamp()), list.size());
				tlg.writeTo(model, LOG);

				List<ValueInfo> infos = new ArrayList<>(5);
				infos.add(new ValueInfo("sdsd:posStatus").setDesignator("Position status").addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:pdop").setDesignator("Position DOP").setScale(0.1).setNumberOfDecimals(1)
						.addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:hdop").setDesignator("Horizontal DOP").setScale(0.1)
						.setNumberOfDecimals(1).addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:satellites").setDesignator("Number of satellites").addTimeLog(tlg));
				infos.add(new ValueInfo("sdsd:fieldStatus").setDesignator("Field status").addTimeLog(tlg));
				for (ValueInfo info : infos) {
					info.writeTo(model, INFO);
				}
				api.writeTriples(model);

				Long[] values = new Long[5];
				try (TimeLogWriter tlw = api.addTimeLog(tlg, infos)) {
					for (GPSEntry gps : list) {
						values[0] = (long) gps.getPositionStatusValue();
						values[1] = Math.round(gps.getPdop() * 10.);
						values[2] = Math.round(gps.getHdop() * 10.);
						values[3] = (long) gps.getNumberOfSatellites();
						values[4] = (long) gps.getFieldStatusValue();

						tlw.write(toInstant(gps.getGpsUtcTimestamp()), gps.getPositionNorth(), gps.getPositionEast(),
								gps.getPositionUp(), values);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				errors.fatal(e.getMessage());
			}

			api.setParseTime((System.nanoTime() - t1) / 1000000);
			api.setErrors(errors);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * The {@code test} method is used to decide whether a given input stream can be
	 * processed by this parser.
	 * 
	 * @param input  Input stream to be tested.
	 * @param output Unused
	 * @return {True} if this parser is likely capable to parse the provided input
	 *         stream, {@code false} otherwise.
	 */
	public static boolean test(InputStream input, OutputStream output) {
		try {
			GPSList list = GPSList.parseFrom(input);
			return list.getGpsEntriesCount() > 0;
		} catch (IOException e) {
		}
		return false;
	}

}