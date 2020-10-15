package de.sdsd.projekt.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.opencsv.bean.CsvToBeanBuilder;

/**
 * Wrapper class for using HackeData class. The class provides functionalities
 * for working with HackeData. It holds a list of HackeData objects.
 * 
 * @author ngs
 *
 */
public class HackeDataParser {
	
	/** The data. */
	List<HackeData> data;

	/**
	 * Constructor, which parse data from specified input stream to data list of
	 * HackeData. It uses CsvToBeanBuilder,which is imported by Maven.
	 * 
	 * @param input specifies InputStream to use for parsing to assigned list.
	 */
	public HackeDataParser(InputStream input) {
		this.data = new CsvToBeanBuilder(new InputStreamReader(input)).withThrowExceptions(true).withSeparator(';')
				.withType(HackeData.class).build().parse();
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public List<HackeData> getData() {
		return data;
	}

	/**
	 * Sets the data.
	 *
	 * @param data the new data
	 */
	public void setData(List<HackeData> data) {
		this.data = data;
	}

}
