package de.sdsd.projekt.parser;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import com.opencsv.bean.CsvToBeanBuilder;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.api.ParserAPI.GeoWriter;
import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.api.ServiceAPI.ElementType;
import de.sdsd.projekt.api.Util;
import de.sdsd.projekt.api.Util.WikiFormat;
import de.sdsd.projekt.api.Util.WikiType;
import de.sdsd.projekt.parser.annotations.HelmIdReference;
import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.helmdata.Artikel;
import de.sdsd.projekt.parser.helmdata.Artikeltyp;
import de.sdsd.projekt.parser.helmdata.Betrieb;
import de.sdsd.projekt.parser.helmdata.Schlag;
import de.sdsd.projekt.parser.helmdata.Schnittstelle;
import de.sdsd.projekt.parser.helmdata.Tagebuch;
import de.sdsd.projekt.parser.helmdata.Teilbetrieb;
import de.sdsd.projekt.parser.interfaces.Geographic;
import de.sdsd.projekt.parser.interfaces.Identifiable;
import de.sdsd.projekt.parser.wrapper.HelmCoordinate;
import de.sdsd.projekt.parser.wrapper.Id;
import de.sdsd.projekt.parser.wrapper.IdRef;
import de.sdsd.projekt.parser.wrapper.RdfModel;

/**
 * Parser for .rax-files containing multiple CSV files separated by []-tags.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public final class HelmParser {
	
	/** The Constant CSV_SEPARATOR. */
	private static final String CSV_SEPARATOR = ";";
	
	/** The Constant EOF_TOKEN. */
	private static final String EOF_TOKEN = "[END]";

	/** The Constant FORMAT. */
	private static final String FORMAT = "HelmData";
	
	/** The Constant WIKI_FORMAT. */
	private static final WikiFormat WIKI_FORMAT = Util.format(FORMAT);

	/** The Constant ENTITIES. */
	// Supported CSV delimiter tags.
	private static final HashMap<String, Class<?>> ENTITIES = new HashMap<String, Class<?>>() {
		private static final long serialVersionUID = 1L;
		{
			put("[ARTIKEL]", Artikel.class);
			put("[ARTIKELTYP]", Artikeltyp.class);
			put("[BETRIEB]", Betrieb.class);
			put("[SCHLAG]", Schlag.class);
			put("[SCHNITTSTELLE]", Schnittstelle.class);
			put("[TAGEBUCH]", Tagebuch.class);
			put("[TEILBETRIEB]", Teilbetrieb.class);
		}
	};

	/** The Constant WIKI_RESOURCES. */
	// Wiki resources representing the CSV entities of a .rax file.
	private static final Map<Class<?>, WikiType> WIKI_RESOURCES = createWikiResources();

	/** The helm ids. */
	// IDs of identifiable CSV rows mapped to their generated Jena resource URIs.
	private static HashMap<Id, Resource> HELM_IDS = new HashMap<>();

	/** The errors. */
	// List of caught exception messages.
	private static Validation ERRORS = new Validation();

	/**
	 * Parses the CSV contents of a .rax file. Initially, the CSV entities are split
	 * and extracted out of the input .rax file. After that, we iterate through the
	 * CSV entities and map their contents to POJOs using the OpenCSV library. The
	 * mapped objects are then stored inside the map {@code parsedCsvEntities} which
	 * is passed to {@link #addReferencesToRdfModel(RdfModel)} and
	 * {@link #createRdfModel(Map)}. The contents of the returned {@code rdfModel}
	 * are zipped and written into the {@code OutputStream} using
	 * {@link #writeRdfModel(RdfModel, OutputStream)}.
	 * 
	 * @param input  Contents of the .rax file.
	 * @param output ZIP compressed Jena triples and Geo-JSON representing the
	 *               parser's output.
	 * @see #csvSplitEntities(InputStream)
	 * @see #csvMapToObjects(List, Class)
	 * @see #createRdfModel(Map)
	 * @see #writeRdfModel(RdfModel, OutputStream)
	 */
	static void parse(InputStream input, OutputStream output) {
		Map<Class<?>, List<String>> csvEntities = csvSplitEntities(input);
		Map<Class<?>, List<?>> parsedCsvEntities = new HashMap<Class<?>, List<?>>();

		for (Entry<Class<?>, List<String>> csvEntity : csvEntities.entrySet()) {
			Class<?> csvClass = csvEntity.getKey();
			List<String> csvRows = csvEntity.getValue();

			List<?> csvObjects = csvMapToObjects(csvRows, csvClass);
			parsedCsvEntities.put(csvClass, csvObjects);
		}

		RdfModel rdfModel = createRdfModel(parsedCsvEntities);
		addReferencesToRdfModel(rdfModel);

		try (ParserAPI api = new ParserAPI(output)) {
			writeRdfModel(api, rdfModel, output);
		} catch (Throwable e) {
			error(e);
		}
	}

	/**
	 * Checks whether the provided {@code InputStream} is likely to be parsed by
	 * this parser. An input is considered parsable if its first line contains a
	 * known RAX-delimiter tag [...] and its last line is the [END] tag.
	 * 
	 * @param input  Input to be tested.
	 * @param output Error message which occurred while testing.
	 * @return {@code true} if the input can most likely be parsed, {@code false}
	 *         otherwise.
	 * @see #csvFileToRows(InputStream, Charset)
	 */
	static boolean test(InputStream input, OutputStream output) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
			try {
				List<String> csvRows = csvFileToRows(input, StandardCharsets.ISO_8859_1);
				if (csvRows.size() > 1) {
					String firstRow = csvRows.get(0);
					String lastRow = csvRows.get(csvRows.size() - 1);
					boolean success = ENTITIES.containsKey(firstRow) && lastRow.equals(EOF_TOKEN);
					if (!success)
						out.println("RaxParser::test(): Test failed. Unknown entity \"" + firstRow
								+ "\" in first row OR last row \"" + lastRow + "\" was not the expected EOF token \""
								+ EOF_TOKEN + "\".");
					return success;
				}
			} catch (Throwable e) {
				out.println(e.getMessage());
			}
		}

		return false;
	}

	/**
	 * Creates a wrapper object of type {@code RdfModel} containing a list of
	 * references to parent CSV objects as well as GPS coordinates, which are not
	 * part of the Jena model and therefore not written into the triplestore.
	 * 
	 * This function loops through all the CSV files, which were inside of the .rax
	 * file, delimited by []-tags, and adds the mapped CSV objects (rows) of that
	 * file to the Jena model, located inside of {@code rdfModel}. At first, each
	 * CSV object is inserted into the model without any {@code partOf} reference to
	 * its parent(s). These connections are made inside of
	 * {@link HelmParser#addReferencesToRdfModel(RdfModel)} after the model has been
	 * initially built.
	 *
	 * @param parsedCsvEntities The parsed CSV entities of a .rax input file, mapped
	 *                          to their POJO class type.
	 * @return Wrapper object of type {@code RdfModel} containing the loosely built
	 *         Jena RDF model, a list of GPS coordinates and the list of references
	 *         to parent objects.
	 * @see #addEntityToRdfModel(RdfModel, Entry)
	 * @see #addReferencesToRdfModel(RdfModel)
	 */
	private static RdfModel createRdfModel(Map<Class<?>, List<?>> parsedCsvEntities) {
		RdfModel rdfModel = new RdfModel();

		for (Entry<Class<?>, List<?>> entity : parsedCsvEntities.entrySet())
			addEntityToRdfModel(rdfModel, entity);

		return rdfModel;
	}

	/**
	 * Each extracted CSV file is called an entity. This method loops through all
	 * the CSV rows {@code List<?>} of an entity and transforms its attributes into
	 * Jena RDF model entries. Every such entry is identified by a globally unique
	 * identifier, created by {@code Util.createRandomUriResource()}.
	 * 
	 * If a CSV row, represented by its mapped object, implements the
	 * {@code Identifiable}, its ID value and class type are stored inside the map
	 * {@code HELM_IDS}. This is necessary to be able to assign the corresponding
	 * parent object to a child instance later.
	 * 
	 * If an object implements the {@code Geographic} interface, this means that GPS
	 * coordinates are available which are not to be entered in the triplestore.
	 * Instead, a {@code HelmCoordinate} object is created which stores a reference
	 * to the current RDF model entry. In addition to the GPS coordinates, the ID
	 * attributes are also provided with the annotation {@code HelmTransient}, since
	 * these must not be inserted into the triplestore either.
	 * 
	 * If the annotation {@code HelmReference} indicates that an attribute is a
	 * reference to a parent element, it is inserted into the list of {@code RefIds}
	 * called {@code refs}, located within the {@code RdfModel} wrapper object.
	 *
	 * @param rdfModel A reference to an initially empty RDF model wrapper instance.
	 * @param entity   Mapping from an RAX entity to its class type.
	 * @see #addResourceLabel(Resource, Object)
	 * @see #getFieldsWithValues(Object)
	 */
	private static void addEntityToRdfModel(RdfModel rdfModel, Entry<Class<?>, List<?>> entity) {
		Class<?> entityClass = entity.getKey();
		List<?> entityRows = entity.getValue();

		WikiType entityResource = WIKI_RESOURCES.get(entityClass);

		for (Object row : entityRows) {
			Resource entityUriResource = Util.createRandomUriResource(rdfModel.getModel(), entityResource, null);
			addResourceLabel(entityUriResource, row);

			if (row instanceof Identifiable) {
				Long idValue = ((Identifiable) row).getId();
				Id id = new Id(entityClass, idValue);
				HELM_IDS.put(id, entityUriResource);
			}

			if (row instanceof Geographic) {
				HelmCoordinate coord = new HelmCoordinate();
				Geographic geoObj = (Geographic) row;

				if (geoObj.getLongitude() != null && geoObj.getLatitude() != null) {
					coord.setLongitude(geoObj.getLongitude());
					coord.setLatitude(geoObj.getLatitude());
					coord.setLabel(geoObj.getGeoLabel());
					coord.setReference(entityUriResource);

					rdfModel.addCoordinate(coord);
				} else {
					error("addEntityToRdfModel", "A HelmCoordinate of a Geographic object of type '"
							+ entityClass.getSimpleName() + "' with id '" + entityUriResource + "' is null.");
				}
			}

			for (Entry<Field, Object> attrVal : getFieldsWithValues(row).entrySet()) {
				Object attrValue = attrVal.getValue();
				if (attrValue == null)
					continue;

				Field attrField = attrVal.getKey();
				String attrName = attrField.getName();

				if (!attrField.isAnnotationPresent(HelmTransient.class)) {
					Literal typedLiteral;

					if (attrValue instanceof Date)
						typedLiteral = Util.lit(((Date) attrValue).toInstant());
					else
						typedLiteral = ResourceFactory.createTypedLiteral(attrValue);

					Property wikiProperty = entityResource.prop(attrName);
					entityUriResource.addProperty(wikiProperty, typedLiteral);
				}

				HelmIdReference helmRef = attrField.getAnnotation(HelmIdReference.class);
				if (helmRef != null) {
					IdRef ref = new IdRef(new Id(helmRef.value(), (Long) attrValue), entityUriResource);
					rdfModel.addPartOfReference(ref);
				}
			}
		}
	}

	/**
	 * Finds and sets the Jena parent resource ({@code partOf}, referenced by the
	 * {@code IdRef} objects inside the provided {@code RdfModel}.
	 * 
	 * @param rdfModel The Jena RDF model containing the list of referenced IDs.
	 * @see #error(String, String)
	 */
	private static void addReferencesToRdfModel(RdfModel rdfModel) {
		for (IdRef ref : rdfModel.getRefs()) {
			Id parentId = ref.getId();
			Resource parent = HELM_IDS.get(parentId);

			if (parent != null)
				ref.getReferencingResource().addProperty(DCTerms.isPartOf, parent);
			else {
				error("addReferencesToRdfModel", "Cannot find parent resource for " + ref + ".");
			}
		}
	}

	/**
	 * Uses the Parser API to write the completely cross-linked Jena RDF model into
	 * the {@code OutputStream} as a ZIP encoded data stream. By providing a third
	 * command line argument, this data can also be written into a standalone ZIP
	 * file.
	 * 
	 * @param api      SDSD Parser API instance.
	 * @param rdfModel The Jena RDF model to be converted into a ZIP encoded
	 *                 {@code OutputStream}.
	 * @param output   ZIP encoded {@code OutputStream}.
	 */
	private static void writeRdfModel(ParserAPI api, RdfModel rdfModel, OutputStream output) {
		long t1 = System.nanoTime();

		try {
			api.writeTriples(rdfModel.getModel());
			writeGeoPositions(api, rdfModel.getCoords());
		} catch (Throwable e) {
			error(e);
		}

		api.setParseTime((System.nanoTime() - t1) / 1000000);
		api.setErrors(ERRORS);
	}

	/**
	 * Uses the Parser API to write GPS coordinates into a file called
	 * {@code geo.json} and associate that position with the corresponding Jena RDF
	 * model entry. GPS coordinates are not written to the triple but to the
	 * geostore.
	 * 
	 * @param api    SDSD Parser API instance.
	 * @param coords List of GPS coordinates to be written.
	 */
	private static void writeGeoPositions(ParserAPI api, List<HelmCoordinate> coords) {
		try (GeoWriter geo = api.writeGeo()) {
			for (HelmCoordinate coord : coords)
				geo.writeFeature(coord.toGeoJson(), ElementType.Other, coord.getReference().getURI(), coord.getLabel());
		} catch (Throwable e) {
			error(e);
		}
	}

	/**
	 * Creates Wikinormia resources using the declared RAX entities
	 * {@link #ENTITIES} and maps them to their corresponding class types.
	 * 
	 * @return Mapping of the RAX entity class types to their Wikinormia resources.
	 */
	private static Map<Class<?>, WikiType> createWikiResources() {
		HashMap<Class<?>, WikiType> wikiResources = new HashMap<>();
		for (Class<?> entityClass : ENTITIES.values()) {
			String entityName = entityClass.getSimpleName();
			wikiResources.put(entityClass, WIKI_FORMAT.res(entityName));
		}

		return wikiResources;
	}

	/**
	 * Uses introspection to map the declared fields of an instantiated {@code obj}
	 * to its values.
	 * 
	 * @param obj Object of some arbitrary type whose fields are mapped to their
	 *            values.
	 * @return Mapping of the fields to their values.
	 */
	private static Map<Field, Object> getFieldsWithValues(Object obj) {
		Map<Field, Object> fieldsWithValues = new HashMap<>();

		for (Field attribute : obj.getClass().getDeclaredFields()) {
			attribute.setAccessible(true);
			try {
				Object attributeValue = attribute.get(obj);
				fieldsWithValues.put(attribute, attributeValue);
			} catch (Throwable e) {
				error(e);
			}
		}

		return fieldsWithValues;
	}

	/**
	 * This function splits a RAX formatted input into its individual CSV
	 * components. To find the split points within the RAX file, the keys of the map
	 * {@code ENTITIES} are used. It is assumed that the first line after the split
	 * contains the attribute names of the entity. Each additional line, which is
	 * not a file separator, is therefore assumed to be a line of a CSV file.
	 * 
	 * @param input Input RAX file as an {@code InputStream}.
	 * @return Mapping of the RAX entity's class to its list of CSV rows.
	 */
	private static Map<Class<?>, List<String>> csvSplitEntities(InputStream input) {
		Map<Class<?>, List<String>> csvEntities = new HashMap<Class<?>, List<String>>();

		List<String> csvRows = csvFileToRows(input, StandardCharsets.ISO_8859_1);
		Class<?> curRowClass = null;

		for (int i = 0; i < csvRows.size(); ++i) {
			String row = csvRows.get(i);
			if (ENTITIES.containsKey(row)) {
				curRowClass = ENTITIES.get(row);
				if (!csvEntities.containsKey(curRowClass))
					csvEntities.put(curRowClass, new ArrayList<String>());
				if (i + 1 < csvRows.size()) {
					Optional<String> header = removeTypesFromCsvHeader(csvRows.get(i + 1));
					if (header.isPresent())
						csvRows.set(i + 1, header.get());
				}
			} else if (!isRaxSeparator(row))
				csvEntities.get(curRowClass).add(row);
			else if (!EOF_TOKEN.equals(row))
				error("csvSplitEntities", "Unrecognized CSV row: " + row);
		}

		return csvEntities;
	}

	/**
	 * Uses the OpenCSV library to map the rows of a typed CSV file to a list of
	 * objects of that type.
	 *
	 * @param <T> the generic type
	 * @param csvRows   List of CSV rows to be mapped to their class type.
	 * @param valueType Class type of the objects inside the returned list.
	 * @return List of mapped POJOs. Each object inside this lists represents a row
	 *         of a CSV file.
	 */
	private static <T> List<T> csvMapToObjects(List<String> csvRows, Class<T> valueType) {
		String csvFileStr = StringUtils.join(csvRows, "\n");
		List<T> csvRowObjects = new ArrayList<>();

		try {
			csvRowObjects = new CsvToBeanBuilder<T>(new StringReader(csvFileStr)).withSeparator(CSV_SEPARATOR.charAt(0))
					.withType(valueType).build().parse();
		} catch (Throwable e) {
			error(e);
		}

		return csvRowObjects;
	}

	/**
	 * Removes the data types, which are separated by semicolons, following each
	 * attribute name. If data types are missing or appear too often, an empty
	 * {@code Optional} is returned.
	 * 
	 * @param csvHeader CSV header row to be sanitized.
	 * @return An {@code Optional} containing the sanitized CSV header, separated by
	 *         semicolons.
	 */
	private static Optional<String> removeTypesFromCsvHeader(String csvHeader) {
		List<String> headers = new LinkedList<String>(Arrays.asList(csvHeader.split(CSV_SEPARATOR)));
		if (headers.size() % 2 != 0) {
			error("removeTypesFromCsvHeader", "Column and datatype count do not match.");
			return Optional.empty();
		}

		List<String> types = new ArrayList<>();
		for (int i = 0; i < headers.size(); ++i)
			if (i % 2 == 1)
				types.add(headers.get(i));

		headers.removeAll(types);
		return Optional.of(StringUtils.join(headers, CSV_SEPARATOR));
	}

	/**
	 * Converts the input from the provided {@code InputStream} into a string and
	 * separates the individual lines of that input at the respective line breaks.
	 * 
	 * @param input   Stream of input data to be converted and split into lines.
	 * @param charset The charset encoding to be used for the string conversion.
	 * @return List of CSV lines.
	 */
	private static List<String> csvFileToRows(InputStream input, Charset charset) {
		List<String> rows = new ArrayList<>();
		try {
			rows = Arrays.asList(IOUtils.toString(input, charset).split("[\\r\\n]+")).stream().map(String::trim)
					.filter(row -> !row.isEmpty()).collect(Collectors.toList());
		} catch (Throwable e) {
			error(e);
		}
		return rows;
	}

	/**
	 * Checks whether a CSV row is an RAX separator tag "[...]".
	 * 
	 * @param row The CSV line to be checked for a separator tag.
	 * @return {@code True} if {@code row} is a separator tag, {@code false}
	 *         otherwise.
	 */
	private static boolean isRaxSeparator(String row) {
		return row.startsWith("[") && row.endsWith("]") && !row.contains(CSV_SEPARATOR)
				&& StringUtils.isAllUpperCase(row.substring(1, row.length() - 1));
	}

	/**
	 * Finds a field by its annotation inside an object and returns its value.
	 * 
	 * @param row            Mapped CSV row containing the annotated field.
	 * @param annotationType Class type of the annotation.
	 * @return Value of the annotated field.
	 */
	private static Optional<Object> getAnnotatedFieldValue(Object row, Class<? extends Annotation> annotationType) {
		Object value = null;

		for (Field field : row.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			if (field.isAnnotationPresent(annotationType)) {
				try {
					value = field.get(row);
				} catch (Throwable e) {
					error(e);
				}
				break;
			}
		}

		return Optional.ofNullable(value);
	}

	/**
	 * Finds the field annotated with {@code HelmLabel} inside a mapped CSV row and
	 * extracts its value as a row label. This label is then added to the randomly
	 * generated {@code UriResource} identifying a Jena RDF entry.
	 * 
	 * @param entityUriResource Randomly generated identifier of a Jena RDF entry.
	 * @param row               Mapped CSV row containing a label field annotated
	 *                          with {@code HelmLabel}.
	 * @see #getAnnotatedFieldValue(Object, Class)
	 */
	private static void addResourceLabel(Resource entityUriResource, Object row) {
		Optional<Object> resourceLabel = getAnnotatedFieldValue(row, HelmLabel.class);
		if (resourceLabel.isPresent())
			entityUriResource.addLiteral(RDFS.label, ResourceFactory.createTypedLiteral(resourceLabel.get()));
	}

	/**
	 * Registers and prints a user defined error message to {@code System.err}.
	 * 
	 * @param location The name of the function/method the error originated from.
	 * @param msg      Error message to be registered and printed.
	 */
	private static void error(String location, String msg) {
		msg = "HelmParser::" + location + "(): " + msg;
		ERRORS.error(msg);
		System.err.println(msg);
	}

	/**
	 * Registers and prints the error message from an exception.
	 * 
	 * @param e The raised and caught exception.
	 */
	private static void error(Throwable e) {
		ERRORS.fatal(e.getMessage());
		e.printStackTrace();
	}
}