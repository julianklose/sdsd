package de.sdsd.projekt.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;

import agrirouter.technicalmessagetype.Gps.GPSList;
import agrirouter.technicalmessagetype.Gps.GPSList.GPSEntry;
import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.TimeLogWriter;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.parser.EFDItoISOXML.TLG;
import de.sdsd.projekt.parser.EfdiParser.EfdiTimeLog;
import de.sdsd.projekt.parser.EfdiParser.EfdiTimeLog.Entry;
import efdi.GrpcEfdi.Position;

/**
 * The Class MainParser.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class MainParser {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			InputStream in = args.length > 1 ? FileUtils.openInputStream(new File(args[1])) : System.in;
			OutputStream out = args.length > 2 ? FileUtils.openOutputStream(new File(args[2])) : System.out;
			switch (args[0].toLowerCase()) {
			case "parse":
				parse(in, out);
				break;
			case "validate":
				validate(in, out);
				break;
			case "test":
				System.exit(test(in, out) ? 0 : 1);
				break;
			case "isoxml":
				isoxml(in, out);
				break;
			case "gps":
				gps(in, out);
				break;
			default:
				System.err.println("No parser specified ('parse', 'validate', 'test', 'isoxml', 'gps')");
				break;
			}
		} else
			System.err.println("USAGE: java -jar parser.jar parse|validate|test|isoxml|gps filepath");
	}

	/**
	 * Parses the.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void parse(InputStream input, OutputStream output) {
		try (ParserAPI api = new ParserAPI(output)) {
			Validation errors = new Validation();
			long t1 = System.nanoTime();

			try {
				EfdiParser efdi = new EfdiParser(input);
				errors.addAll(efdi.validate());

				Model model = efdi.toRDF();
				if (model != null && !model.isEmpty())
					api.writeTriples(model);

				for (EfdiTimeLog tlg : efdi.getTimelogs()) {
					try (TimeLogWriter tlw = api.addTimeLog(tlg.createLog(), tlg.getValueInfos())) {
						for (EfdiTimeLog.Entry tim : tlg) {
							tlw.write(tim.getTime(), tim.getLatitude(), tim.getLongitude(), tim.getAltitude(),
									tim.getValueRow());
						}
					} catch (Throwable e) {
						e.printStackTrace();
						errors.fatal(e.getMessage());
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
	 * Validate.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void validate(InputStream input, OutputStream output) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
			try {
				EfdiParser efdi = new EfdiParser(input);
				efdi.validate().forEach(out::println);
			} catch (Throwable e) {
				out.println(e.getMessage());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test.
	 *
	 * @param input  the input
	 * @param output the output
	 * @return true, if successful
	 */
	public static boolean test(InputStream input, OutputStream output) {
		try (ZipInputStream in = new ZipInputStream(input, Charset.forName("Cp437"))) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;
				String name = entry.getName().toLowerCase();
				if (name.endsWith("devicedescription.bin"))
					return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

	/**
	 * Isoxml.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void isoxml(InputStream input, OutputStream output) {
		try (ZipOutputStream zip = new ZipOutputStream(output, Charset.forName("Cp437"))) {
			EfdiParser efdi = new EfdiParser(input);
			EFDItoISOXML converter = new EFDItoISOXML(efdi.getDeviceDescription());
			for (EfdiTimeLog tlg : efdi.getTimelogs()) {
				try {
					TLG log = converter.addTimeLog(tlg.tlg);
					zip.putNextEntry(new ZipEntry(log.getFilename() + ".xml"));
					log.writeXML(zip);
					zip.closeEntry();
					zip.putNextEntry(new ZipEntry(log.getFilename() + ".bin"));
					log.writeBIN(zip);
					zip.closeEntry();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			zip.putNextEntry(new ZipEntry("TASKDATA.xml"));
			converter.writeTaskData(zip);
			zip.closeEntry();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gps.
	 *
	 * @param input  the input
	 * @param output the output
	 */
	public static void gps(InputStream input, OutputStream output) {
		try (ZipOutputStream zip = new ZipOutputStream(output, Charset.forName("Cp437"))) {
			EfdiParser efdi = new EfdiParser(input);
			for (EfdiTimeLog tlg : efdi.getTimelogs()) {
				try {
					GPSList.Builder list = GPSList.newBuilder();

					for (Entry time : tlg) {
						Position pos = time.tim.getPositionStart();
						GPSEntry.Builder entry = GPSEntry.newBuilder();
						entry.setPositionNorth(pos.getPositionNorth());
						entry.setPositionEast(pos.getPositionEast());
						entry.setPositionUp(pos.getPositionUp());
						entry.setPositionStatusValue(pos.getPositionStatusValue());
						entry.setPdop(pos.getPdop());
						entry.setHdop(pos.getHdop());
						entry.setNumberOfSatellites(pos.getNumberOfSatellites());
						entry.setGpsUtcTimestamp(
								pos.hasGpsUtcTimestamp() ? pos.getGpsUtcTimestamp() : time.tim.getStart());
						list.addGpsEntries(entry.build());
					}

					zip.putNextEntry(new ZipEntry(tlg.tlg.getFilename() + ".bin"));
					list.build().writeTo(zip);
					zip.closeEntry();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
