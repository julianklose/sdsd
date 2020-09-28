package de.sdsd.projekt.api;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipOutputStream;

/**
 * The Class IsoZipOutputStream.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class IsoZipOutputStream extends ZipOutputStream {
	
	/**
	 * This class implements an output stream filter for writing files in the ISOXML ZIP file format. 
	 * Includes support for both compressed and uncompressed entries.
	 *
	 * @param out the out
	 */
	public IsoZipOutputStream(OutputStream out) {
		super(out, Charset.forName("Cp437"));
	}
}
