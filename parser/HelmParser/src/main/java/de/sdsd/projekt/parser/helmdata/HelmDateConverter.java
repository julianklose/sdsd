package de.sdsd.projekt.parser.helmdata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.opencsv.bean.AbstractBeanField;

/**
 * Custom converter for attributes of type Date inside CSV mapping classes.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class HelmDateConverter extends AbstractBeanField<Object, Object> {
	
	/** The Constant DATE_FORMAT. */
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	/**
	 * Convert.
	 *
	 * @param dateStr the date str
	 * @return the date
	 */
	@Override
	protected Date convert(String dateStr) {
		if (dateStr.isEmpty())
			return null;

		try {
			return (new SimpleDateFormat(DATE_FORMAT)).parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}
}