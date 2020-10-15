package de.sdsd.projekt.api.isoxml;

import java.util.Arrays;
import java.util.Optional;

import de.sdsd.projekt.api.isoxml.IsoxmlCreator.Elem;
import de.sdsd.projekt.api.isoxml.IsoxmlCreator.ISO11783_TaskData;

/**
 * The Class Partfield.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
public class Partfield extends Elem {

	/**
	 * Instantiates a new partfield.
	 *
	 * @param parent     the parent
	 * @param designator the designator
	 * @param area       the area
	 */
	public Partfield(ISO11783_TaskData parent, String designator, long area) {
		super(parent, "PFD");
		if (area < 0 || area > 4294967294L)
			throw new IllegalArgumentException("invalid area");
		e.setAttribute("A", id);
		e.setAttribute("C", designator);
		e.setAttribute("D", Long.toString(area));
	}

	/**
	 * Sets the code.
	 *
	 * @param code the code
	 * @return the partfield
	 */
	public Partfield setCode(String code) {
		e.setAttribute("B", code);
		return this;
	}

	/**
	 * Sets the customer.
	 *
	 * @param ctr the ctr
	 * @return the partfield
	 */
	public Partfield setCustomer(Task.Customer ctr) {
		e.setAttribute("E", ctr.id);
		return this;
	}

	/**
	 * Sets the farm.
	 *
	 * @param frm the frm
	 * @return the partfield
	 */
	public Partfield setFarm(Task.Farm frm) {
		e.setAttribute("F", frm.id);
		return this;
	}

	/**
	 * Sets the crop type.
	 *
	 * @param ctp the ctp
	 * @return the partfield
	 */
	public Partfield setCropType(CropType ctp) {
		e.setAttribute("G", ctp.id);
		return this;
	}

	/**
	 * Sets the crop variety.
	 *
	 * @param cvt the cvt
	 * @return the partfield
	 */
	public Partfield setCropVariety(CropVariety cvt) {
		e.setAttribute("H", cvt.id);
		return this;
	}

	/**
	 * Sets the field.
	 *
	 * @param pfd the pfd
	 * @return the partfield
	 */
	public Partfield setField(Partfield pfd) {
		e.setAttribute("I", pfd.id);
		return this;
	}

	/**
	 * Adds the polygon.
	 *
	 * @param type the type
	 * @return the polygon
	 */
	public Polygon addPolygon(PolygonType type) {
		return new Polygon(this, type);
	}

	/**
	 * Adds the line string.
	 *
	 * @param type the type
	 * @return the line string
	 */
	public LineString addLineString(LineStringType type) {
		return new LineString(this, type);
	}

	/**
	 * Adds the point.
	 *
	 * @param type  the type
	 * @param north the north
	 * @param east  the east
	 * @return the point. xml point
	 */
	public Point.XmlPoint addPoint(Point.PointType type, double north, double east) {
		return new Point.XmlPoint(this, type, north, east);
	}

	/**
	 * Adds the point.
	 *
	 * @return the point. bin point
	 */
	public Point.BinPoint addPoint() {
		return new Point.BinPoint(this);
	}

	/**
	 * Adds the guidance group.
	 *
	 * @return the guidance group
	 */
	public GuidanceGroup addGuidanceGroup() {
		return new GuidanceGroup(this);
	}

	/**
	 * The Enum ProductType.
	 */
	public static enum ProductType {

		/** The single. */
		SINGLE(1),
		/** The mixture. */
		MIXTURE(2),
		/** The temporary mixture. */
		TEMPORARY_MIXTURE(3);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new product type.
		 *
		 * @param number the number
		 */
		private ProductType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<ProductType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<ProductType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class Product.
	 */
	public static class Product extends Elem {

		/**
		 * Instantiates a new product.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public Product(ISO11783_TaskData parent, String designator) {
			super(parent, "PDT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Sets the group.
		 *
		 * @param group the group
		 * @return the product
		 */
		public Product setGroup(ProductGroup group) {
			e.setAttribute("C", group.id);
			return this;
		}

		/**
		 * Sets the value presentation.
		 *
		 * @param vpn the vpn
		 * @return the product
		 */
		public Product setValuePresentation(Device.ValuePresentation vpn) {
			e.setAttribute("D", vpn.id);
			return this;
		}

		/**
		 * Sets the quantity DDI.
		 *
		 * @param ddi the ddi
		 * @return the product
		 */
		public Product setQuantityDDI(int ddi) {
			e.setAttribute("E", ddi(ddi));
			return this;
		}

		/**
		 * Sets the type.
		 *
		 * @param type the type
		 * @return the product
		 */
		public Product setType(ProductType type) {
			e.setAttribute("F", Integer.toString(type.number));
			return this;
		}

		/**
		 * Sets the mixture recipe quantity.
		 *
		 * @param mixtureRecipeQuantity the mixture recipe quantity
		 * @return the product
		 */
		public Product setMixtureRecipeQuantity(int mixtureRecipeQuantity) {
			if (mixtureRecipeQuantity < 0)
				throw new IllegalArgumentException("invalid mixture recipe quantity");
			e.setAttribute("G", Integer.toString(mixtureRecipeQuantity));
			return this;
		}

		/**
		 * Sets the density mass per volume.
		 *
		 * @param densityMassPerVolume the density mass per volume
		 * @return the product
		 */
		public Product setDensityMassPerVolume(int densityMassPerVolume) {
			if (densityMassPerVolume < 0)
				throw new IllegalArgumentException("invalid density mass per volume");
			e.setAttribute("H", Integer.toString(densityMassPerVolume));
			return this;
		}

		/**
		 * Sets the density mass per count.
		 *
		 * @param densityMassPerCount the density mass per count
		 * @return the product
		 */
		public Product setDensityMassPerCount(int densityMassPerCount) {
			if (densityMassPerCount < 0)
				throw new IllegalArgumentException("invalid density mass per count");
			e.setAttribute("I", Integer.toString(densityMassPerCount));
			return this;
		}

		/**
		 * Sets the density volume per count.
		 *
		 * @param densityVolumePerCount the density volume per count
		 * @return the product
		 */
		public Product setDensityVolumePerCount(int densityVolumePerCount) {
			if (densityVolumePerCount < 0)
				throw new IllegalArgumentException("invalid density volume per count");
			e.setAttribute("J", Integer.toString(densityVolumePerCount));
			return this;
		}

		/**
		 * Adds the product relation.
		 *
		 * @param pdt           the pdt
		 * @param quantityValue the quantity value
		 * @return the product relation
		 */
		public ProductRelation addProductRelation(Product pdt, int quantityValue) {
			return new ProductRelation(this, pdt, quantityValue);
		}
	}

	/**
	 * The Class ProductRelation.
	 */
	public static class ProductRelation extends Elem {

		/**
		 * Instantiates a new product relation.
		 *
		 * @param parent        the parent
		 * @param pdt           the pdt
		 * @param quantityValue the quantity value
		 */
		public ProductRelation(Product parent, Product pdt, int quantityValue) {
			super(parent, "PRN");
			e.setAttribute("A", pdt.id);
			if (quantityValue < 0)
				throw new IllegalArgumentException("invalid quantity value");
			e.setAttribute("B", Integer.toString(quantityValue));
		}
	}

	/**
	 * The Enum ProductGroupType.
	 */
	public static enum ProductGroupType {

		/** The product group. */
		PRODUCT_GROUP(1),
		/** The crop type. */
		CROP_TYPE(2);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new product group type.
		 *
		 * @param number the number
		 */
		private ProductGroupType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<ProductGroupType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<ProductGroupType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class ProductGroup.
	 */
	public static class ProductGroup extends Elem {

		/**
		 * Instantiates a new product group.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public ProductGroup(ISO11783_TaskData parent, String designator) {
			super(parent, "PGP");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Sets the type.
		 *
		 * @param type the type
		 * @return the product group
		 */
		public ProductGroup setType(ProductGroupType type) {
			e.setAttribute("C", Integer.toString(type.number));
			return this;
		}
	}

	/**
	 * The Class CropType.
	 */
	public static class CropType extends Elem {

		/**
		 * Instantiates a new crop type.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public CropType(ISO11783_TaskData parent, String designator) {
			super(parent, "CTP");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Sets the product group.
		 *
		 * @param pgp the pgp
		 * @return the crop type
		 */
		public CropType setProductGroup(ProductGroup pgp) {
			e.setAttribute("C", pgp.id);
			return this;
		}

		/**
		 * Adds the crop variety.
		 *
		 * @param designator the designator
		 * @return the crop variety
		 */
		public CropVariety addCropVariety(String designator) {
			return new CropVariety(this, designator);
		}
	}

	/**
	 * The Class CropVariety.
	 */
	public static class CropVariety extends Elem {

		/**
		 * Instantiates a new crop variety.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 */
		public CropVariety(CropType parent, String designator) {
			super(parent, "CVT");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
		}

		/**
		 * Sets the product.
		 *
		 * @param pdt the pdt
		 * @return the crop variety
		 */
		public CropVariety setProduct(Product pdt) {
			e.setAttribute("C", pdt.id);
			return this;
		}
	}

	/**
	 * The Enum PolygonType.
	 */
	public static enum PolygonType {

		/** The partfield boundary. */
		PARTFIELD_BOUNDARY(1),
		/** The treatment zone. */
		TREATMENT_ZONE(2),
		/** The water surface. */
		WATER_SURFACE(3),
		/** The building. */
		BUILDING(4),
		/** The road. */
		ROAD(5),

		/** The obstacle. */
		OBSTACLE(6),
		/** The flag. */
		FLAG(7),
		/** The other. */
		OTHER(8),
		/** The mainfield. */
		MAINFIELD(9),
		/** The headland. */
		HEADLAND(10),
		/** The buffer zone. */
		BUFFER_ZONE(11),
		/** The windbreak. */
		WINDBREAK(12);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new polygon type.
		 *
		 * @param number the number
		 */
		private PolygonType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<PolygonType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<PolygonType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class Polygon.
	 */
	public static class Polygon extends Elem {

		/**
		 * Instantiates a new polygon.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		private Polygon(Elem parent, PolygonType type) {
			super(parent, "PLN");
			e.setAttribute("A", Integer.toString(type.number));
		}

		/**
		 * Instantiates a new polygon.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public Polygon(Partfield parent, PolygonType type) {
			this((Elem) parent, type);
		}

		/**
		 * Instantiates a new polygon.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public Polygon(GuidanceGroup parent, PolygonType type) {
			this((Elem) parent, type);
		}

		/**
		 * Instantiates a new polygon.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public Polygon(GuidancePattern parent, PolygonType type) {
			this((Elem) parent, type);
		}

		/**
		 * Instantiates a new polygon.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public Polygon(Grid.TreatmentZone parent, PolygonType type) {
			this((Elem) parent, type);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the polygon
		 */
		public Polygon setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the area.
		 *
		 * @param area the area
		 * @return the polygon
		 */
		public Polygon setArea(long area) {
			if (area < 0)
				throw new IllegalArgumentException("invalid area");
			e.setAttribute("C", Long.toString(area));
			return this;
		}

		/**
		 * Sets the colour.
		 *
		 * @param color the color
		 * @return the polygon
		 */
		public Polygon setColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("D", Integer.toString(color));
			return this;
		}

		/**
		 * Sets the ID.
		 *
		 * @return the polygon
		 */
		public Polygon setID() {
			e.setAttribute("E", id);
			return this;
		}

		/**
		 * Adds the ring.
		 *
		 * @param type the type
		 * @return the line string
		 */
		public LineString addRing(LineStringType type) {
			return new LineString(this, type);
		}
	}

	/**
	 * The Enum LineStringType.
	 */
	public static enum LineStringType {

		/** The polygon exterior. */
		POLYGON_EXTERIOR(1),
		/** The polygon interior. */
		POLYGON_INTERIOR(2),
		/** The tram line. */
		TRAM_LINE(3),
		/** The sampling route. */
		SAMPLING_ROUTE(4),

		/** The guidance pattern. */
		GUIDANCE_PATTERN(5),
		/** The drainage. */
		DRAINAGE(6),
		/** The fence. */
		FENCE(7),
		/** The flag. */
		FLAG(8),
		/** The obstacle. */
		OBSTACLE(9);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new line string type.
		 *
		 * @param number the number
		 */
		private LineStringType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<LineStringType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<LineStringType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class LineString.
	 */
	public static class LineString extends Elem {

		/**
		 * Instantiates a new line string.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		private LineString(Elem parent, LineStringType type) {
			super(parent, "LSG");
			e.setAttribute("A", Integer.toString(type.number));
		}

		/**
		 * Instantiates a new line string.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public LineString(Partfield parent, LineStringType type) {
			this((Elem) parent, type);
		}

		/**
		 * Instantiates a new line string.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public LineString(Polygon parent, LineStringType type) {
			this((Elem) parent, type);
		}

		/**
		 * Instantiates a new line string.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public LineString(GuidancePattern parent, LineStringType type) {
			this((Elem) parent, type);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the line string
		 */
		public LineString setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the width.
		 *
		 * @param width the width
		 * @return the line string
		 */
		public LineString setWidth(long width) {
			if (width < 0 || width > 4294967294L)
				throw new IllegalArgumentException("invalid width");
			e.setAttribute("C", Long.toString(width));
			return this;
		}

		/**
		 * Sets the length.
		 *
		 * @param length the length
		 * @return the line string
		 */
		public LineString setLength(long length) {
			if (length < 0 || length > 4294967294L)
				throw new IllegalArgumentException("invalid length");
			e.setAttribute("D", Long.toString(length));
			return this;
		}

		/**
		 * Sets the colour.
		 *
		 * @param color the color
		 * @return the line string
		 */
		public LineString setColour(int color) {
			if (color < 0 || color > 255)
				throw new IllegalArgumentException("invalid color");
			e.setAttribute("E", Integer.toString(color));
			return this;
		}

		/**
		 * Sets the ID.
		 *
		 * @return the line string
		 */
		public LineString setID() {
			e.setAttribute("F", id);
			return this;
		}

		/**
		 * Adds the point.
		 *
		 * @param type  the type
		 * @param north the north
		 * @param east  the east
		 * @return the point. xml point
		 */
		public Point.XmlPoint addPoint(Point.PointType type, double north, double east) {
			return new Point.XmlPoint(this, type, north, east);
		}

		/**
		 * Adds the point.
		 *
		 * @return the point. bin point
		 */
		public Point.BinPoint addPoint() {
			return new Point.BinPoint(this);
		}
	}

	/**
	 * The Class GuidanceGroup.
	 */
	public static class GuidanceGroup extends Elem {

		/**
		 * Instantiates a new guidance group.
		 *
		 * @param parent the parent
		 */
		public GuidanceGroup(Partfield parent) {
			super(parent, "GGP");
			e.setAttribute("A", id);
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the guidance group
		 */
		public GuidanceGroup setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Adds the guidance pattern.
		 *
		 * @param type the type
		 * @return the guidance pattern
		 */
		public GuidancePattern addGuidancePattern(GuidancePatternType type) {
			return new GuidancePattern(this, type);
		}

		/**
		 * Adds the polygon.
		 *
		 * @param type the type
		 * @return the polygon
		 */
		public Polygon addPolygon(PolygonType type) {
			return new Polygon(this, type);
		}
	}

	/**
	 * The Enum GuidancePatternType.
	 */
	public static enum GuidancePatternType {

		/** The ab. */
		AB(1),
		/** The a plus. */
		A_PLUS(2),
		/** The curve. */
		CURVE(3),
		/** The pivot. */
		PIVOT(4),
		/** The spiral. */
		SPIRAL(5);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new guidance pattern type.
		 *
		 * @param number the number
		 */
		private GuidancePatternType(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<GuidancePatternType> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<GuidancePatternType> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum GuidancePatternOptions.
	 */
	public static enum GuidancePatternOptions {

		/** The clockwise. */
		CLOCKWISE(1),
		/** The counter clockwise. */
		COUNTER_CLOCKWISE(2),
		/** The full circle. */
		FULL_CIRCLE(3);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new guidance pattern options.
		 *
		 * @param number the number
		 */
		private GuidancePatternOptions(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<GuidancePatternOptions> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<GuidancePatternOptions> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum GuidancePatternPropagationDirection.
	 */
	public static enum GuidancePatternPropagationDirection {

		/** The both. */
		BOTH(1),
		/** The left. */
		LEFT(2),
		/** The right. */
		RIGHT(3),
		/** The no prop. */
		NO_PROP(4);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new guidance pattern propagation direction.
		 *
		 * @param number the number
		 */
		private GuidancePatternPropagationDirection(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<GuidancePatternPropagationDirection> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<GuidancePatternPropagationDirection> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum GuidancePatternExtension.
	 */
	public static enum GuidancePatternExtension {

		/** The both. */
		BOTH(1),
		/** The first only. */
		FIRST_ONLY(2),
		/** The last only. */
		LAST_ONLY(3),
		/** The no ext. */
		NO_EXT(4);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new guidance pattern extension.
		 *
		 * @param number the number
		 */
		private GuidancePatternExtension(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<GuidancePatternExtension> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<GuidancePatternExtension> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Enum GuidancePatternGnssMethod.
	 */
	public static enum GuidancePatternGnssMethod {

		/** The gnss fix. */
		GNSS_FIX(1),
		/** The dgnss fix. */
		DGNSS_FIX(2),
		/** The precise gnss. */
		PRECISE_GNSS(3),
		/** The rtk fixed integer. */
		RTK_FIXED_INTEGER(4),
		/** The rtk float. */
		RTK_FLOAT(5),

		/** The estimated dr mode. */
		ESTIMATED_DR_MODE(6),
		/** The manual input. */
		MANUAL_INPUT(7),
		/** The simulate mode. */
		SIMULATE_MODE(8),
		/** The desktop generated data. */
		DESKTOP_GENERATED_DATA(16),
		/** The other. */
		OTHER(17);

		/** The number. */
		public final int number;

		/**
		 * Instantiates a new guidance pattern gnss method.
		 *
		 * @param number the number
		 */
		private GuidancePatternGnssMethod(int number) {
			this.number = number;
		}

		/**
		 * From.
		 *
		 * @param number the number
		 * @return the optional
		 */
		public static Optional<GuidancePatternGnssMethod> from(int number) {
			return Arrays.stream(values()).filter(e -> e.number == number).findAny();
		}

		/**
		 * From.
		 *
		 * @param wikiUri the wiki uri
		 * @return the optional
		 */
		public static Optional<GuidancePatternGnssMethod> from(String wikiUri) {
			try {
				return from(Integer.parseInt(wikiUri.substring(wikiUri.lastIndexOf('_') + 1)));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * The Class GuidancePattern.
	 */
	public static class GuidancePattern extends Elem {

		/**
		 * Instantiates a new guidance pattern.
		 *
		 * @param parent the parent
		 * @param type   the type
		 */
		public GuidancePattern(GuidanceGroup parent, GuidancePatternType type) {
			super(parent, "GPN");
			e.setAttribute("A", id);
			e.setAttribute("C", Integer.toString(type.number));
		}

		/**
		 * Sets the designator.
		 *
		 * @param designator the designator
		 * @return the guidance pattern
		 */
		public GuidancePattern setDesignator(String designator) {
			e.setAttribute("B", designator);
			return this;
		}

		/**
		 * Sets the options.
		 *
		 * @param options the options
		 * @return the guidance pattern
		 */
		public GuidancePattern setOptions(GuidancePatternOptions options) {
			e.setAttribute("D", Integer.toString(options.number));
			return this;
		}

		/**
		 * Sets the propagation direction.
		 *
		 * @param propagationDirection the propagation direction
		 * @return the guidance pattern
		 */
		public GuidancePattern setPropagationDirection(GuidancePatternPropagationDirection propagationDirection) {
			e.setAttribute("E", Integer.toString(propagationDirection.number));
			return this;
		}

		/**
		 * Sets the extension.
		 *
		 * @param extension the extension
		 * @return the guidance pattern
		 */
		public GuidancePattern setExtension(GuidancePatternExtension extension) {
			e.setAttribute("F", Integer.toString(extension.number));
			return this;
		}

		/**
		 * Sets the heading.
		 *
		 * @param heading the heading
		 * @return the guidance pattern
		 */
		public GuidancePattern setHeading(double heading) {
			if (heading < 0 || heading > 360)
				throw new IllegalArgumentException("invalid heading");
			e.setAttribute("G", floating(heading));
			return this;
		}

		/**
		 * Sets the radius.
		 *
		 * @param radius the radius
		 * @return the guidance pattern
		 */
		public GuidancePattern setRadius(long radius) {
			if (radius < 0 || radius > 4294967294L)
				throw new IllegalArgumentException("invalid radius");
			e.setAttribute("H", Long.toString(radius));
			return this;
		}

		/**
		 * Sets the gnss method.
		 *
		 * @param gnssMethod the gnss method
		 * @return the guidance pattern
		 */
		public GuidancePattern setGnssMethod(GuidancePatternGnssMethod gnssMethod) {
			e.setAttribute("I", Integer.toString(gnssMethod.number));
			return this;
		}

		/**
		 * Sets the horizontal accuracy.
		 *
		 * @param horizontalAccuracy the horizontal accuracy
		 * @return the guidance pattern
		 */
		public GuidancePattern setHorizontalAccuracy(double horizontalAccuracy) {
			if (horizontalAccuracy < 0 || horizontalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("J", floating(horizontalAccuracy));
			return this;
		}

		/**
		 * Sets the vertical accuracy.
		 *
		 * @param verticalAccuracy the vertical accuracy
		 * @return the guidance pattern
		 */
		public GuidancePattern setVerticalAccuracy(double verticalAccuracy) {
			if (verticalAccuracy < 0 || verticalAccuracy > 65)
				throw new IllegalArgumentException("invalid accuracy");
			e.setAttribute("K", floating(verticalAccuracy));
			return this;
		}

		/**
		 * Sets the base station.
		 *
		 * @param bsn the bsn
		 * @return the guidance pattern
		 */
		public GuidancePattern setBaseStation(BaseStation bsn) {
			e.setAttribute("L", bsn.id);
			return this;
		}

		/**
		 * Sets the original SRID.
		 *
		 * @param srid the srid
		 * @return the guidance pattern
		 */
		public GuidancePattern setOriginalSRID(String srid) {
			e.setAttribute("M", srid);
			return this;
		}

		/**
		 * Sets the number of swaths left.
		 *
		 * @param swathsLeft the swaths left
		 * @return the guidance pattern
		 */
		public GuidancePattern setNumberOfSwathsLeft(long swathsLeft) {
			if (swathsLeft < 0 || swathsLeft > 4294967294L)
				throw new IllegalArgumentException("invalid number");
			e.setAttribute("N", Long.toString(swathsLeft));
			return this;
		}

		/**
		 * Sets the number of swaths right.
		 *
		 * @param swathsRight the swaths right
		 * @return the guidance pattern
		 */
		public GuidancePattern setNumberOfSwathsRight(long swathsRight) {
			if (swathsRight < 0 || swathsRight > 4294967294L)
				throw new IllegalArgumentException("invalid number");
			e.setAttribute("O", Long.toString(swathsRight));
			return this;
		}

		/**
		 * Adds the line string.
		 *
		 * @param type the type
		 * @return the line string
		 */
		public LineString addLineString(LineStringType type) {
			return new LineString(this, type);
		}

		/**
		 * Adds the polygon.
		 *
		 * @param type the type
		 * @return the polygon
		 */
		public Polygon addPolygon(PolygonType type) {
			return new Polygon(this, type);
		}
	}

	/**
	 * The Class BaseStation.
	 */
	public static class BaseStation extends Elem {

		/**
		 * Instantiates a new base station.
		 *
		 * @param parent     the parent
		 * @param designator the designator
		 * @param north      the north
		 * @param east       the east
		 * @param up         the up
		 */
		public BaseStation(ISO11783_TaskData parent, String designator, double north, double east, int up) {
			super(parent, "BSN");
			e.setAttribute("A", id);
			e.setAttribute("B", designator);
			e.setAttribute("C", north(north));
			e.setAttribute("D", east(east));
			e.setAttribute("E", Integer.toString(up));
		}

	}
}