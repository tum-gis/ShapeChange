/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model;

import java.util.Vector;

import org.apache.xml.serializer.utils.XMLChar;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

public abstract class PropertyInfoImpl extends InfoImpl
		implements PropertyInfo {

	/** Optional qualifier on association role */
	protected Vector<Qualifier> qualifiers = null;

	/** Inquire restriction of property. */
	protected boolean restriction = false;
	protected Boolean implementedByNilReason = null;

	private static int globalSequenceNumberForAttributes = GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ATTRIBUTES;
	private static int globalSequenceNumberForAssociationRoles = GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ASSOCIATIONROLES;

	public int getNextNumberForAttributeWithoutExplicitSequenceNumber() {

		// return the current value and increment it afterwards
		return globalSequenceNumberForAttributes++;
	}

	public int getNextNumberForAssociationRoleWithoutExplicitSequenceNumber() {

		// return the current value and increment it afterwards
		return globalSequenceNumberForAssociationRoles++;
	}

	public boolean isRestriction() {
		return restriction;
	} // restriction()

	public String language() {
		String lang = this.taggedValue("language");

		if (lang==null || lang.isEmpty()) {
			ClassInfo ci = inClass();
			if (ci!=null)
				return ci.language();
		} else
			return lang;
		
		return null;
	}
	
	/**
	 * Return the encoding rule relevant on the operation, given the platform
	 */
	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty()
				|| options().ignoreEncodingRuleTaggedValues())
			s = inClass().encodingRule(platform);
		else
			s = s.toLowerCase();
		return s;
	} // encodingRule()

	/**
	 * This inquires whether the property represents metadata, i.e. if it
	 * carries an appropriate tagged value.
	 */
	public boolean isMetadata() {
		String s = taggedValue("isMetadata");
		if (s != null && s.toLowerCase().equals("true"))
			return true;
		return false;
	} // isMetadata()

	/**
	 * This returns the name of the property adorned with the namespace prefix
	 * of its class's package.
	 */
	public String qname() {
		String s = inClass().pkg().xmlns();
		return s + ":" + name();
	} // qname()

	/**
	 * Check the tagged value "gmlImplementedByNilReason" to find out whether
	 * the property shall allow for nil value treatment. Alternatively check the
	 * characteristics of the property
	 */
	public boolean implementedByNilReason() {

		if (implementedByNilReason == null) {

			String s = taggedValue("gmlImplementedByNilReason");

			if (s != null && s.toLowerCase().equals("true"))
				implementedByNilReason = true;

			else if (inClass().category() == Options.UNION
					&& inClass().properties().size() == 2
					&& name().equalsIgnoreCase("reason")
					&& inClass().name().endsWith("Reason"))
				implementedByNilReason = true;

			else if (this.options().isAIXM()
					&& (this.inClass().category() == Options.FEATURE
							|| this.inClass().category() == Options.OBJECT
							|| this.inClass()
									.category() == Options.AIXMEXTENSION)) {

				implementedByNilReason = true;

			} else
				implementedByNilReason = false;
		}
		return implementedByNilReason.booleanValue();
	} // implementedByNilReason()

	/**
	 * Find out whether nilReason is allowed for the property. The tagged value
	 * 'nilReasonAllowed' is evaluated and if found 'true' determines the
	 * result. Otherwise the result is determined by the value previously set
	 * for the property.
	 */
	// TODO This questionable design, because
	// 1. Target models should not play any role in an input interface.
	// 2. Model independent functionality like the set/inquire pair for
	// nilReasonAllowed should not be part of the model specific interface.

	protected boolean nilReasonAllowed = false;

	public boolean nilReasonAllowed() {
		String s = taggedValue("nilReasonAllowed");
		if (s != null && s.toLowerCase().equals("true")) {
			return true;
		}
		if (nilReasonAllowed) {
			return true;
		}
		return false;
	} // nilReasonAllowed()

	/** Mark the property as 'nilReasonAllowed'. */
	public void nilReasonAllowed(boolean b) {
		nilReasonAllowed = b;
	} // nilReasonAllowed()

	/*
	 * Full qualified UML name
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullName()
	 */
	public String fullName() {
		ClassInfo ci = inClass();
		if (ci == null)
			return "<unknown>::" + name();

		return ci.fullName() + "::" + name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullNameInSchema()
	 */
	public String fullNameInSchema() {

		ClassInfo ci = inClass();
		if (ci == null)
			return "<unknown>::" + name();

		return ci.fullNameInSchema() + "::" + name();
	}

	/*
	 * Try to determine the category of the property value
	 * 
	 * @see
	 * de.interactive_instruments.ShapeChange.Model.PropertyInfo#categoryOfValue
	 * ()
	 */
	public int categoryOfValue() {
		ClassInfo ci = model().classById(typeInfo().id);
		if (ci != null)
			return ci.category();

		return Options.UNKNOWN;
	}

	/*
	 * Validate the property against all applicable requirements and
	 * recommendations
	 */
	public void postprocessAfterLoadingAndValidate() {
		if (postprocessed)
			return;

		super.postprocessAfterLoadingAndValidate();

		String s, s2;
		if (matches("req-xsd-prop-ncname")) {
			s = name();
			if (!XMLChar.isValidNCName(s)) {
				MessageContext mc = result().addError(null, 149, "property", s);
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
		}

		if (matches("req-all-all-documentation")) {
			s = documentation();
			if (!s.contains(options().nameSeparator())) {
				MessageContext mc = result().addError(null, 153, name(),
						options().nameSeparator());
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
			if (!s.contains(options().definitionSeparator())) {
				MessageContext mc = result().addError(null, 153, name(),
						options().definitionSeparator());
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
		}

		ClassInfo ci = model().classById(typeInfo().id);
		String pn = name();
		int basecat = inClass().category();

		if (matches("req-all-prop-sequenceNumber")
				&& basecat != Options.ENUMERATION
				&& basecat != Options.CODELIST) {
			for (PackageInfo pkgi : model().selectedSchemas()) {
				if (inClass().inSchema(pkgi)) {
					String sn = taggedValue("sequenceNumber");
					if (sn == null || sn.length() == 0) {
						MessageContext mc = result().addError(null, 168, name(),
								inClass().name());
						if (mc != null)
							mc.addDetail(null, 400, "Property", fullName());
					}
					break;
				}
			}
		}

		if (matches("req-xsd-prop-value-type-exists") && ci == null
				&& basecat != Options.ENUMERATION
				&& basecat != Options.CODELIST) {
			MapEntry me = options().typeMapEntry(typeInfo().name,
					encodingRule("xsd"));
			if (me == null) {
				MessageContext mc = result().addWarning(null, 131,
						inClass().name() + "." + pn, typeInfo().name);
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
		}

		if (matches("req-xsd-prop-codelist-obligation") && ci != null
				&& ci.category() == Options.CODELIST) {
			s = ci.taggedValue("vocabulary");
			s2 = taggedValue("obligation");
			if (s2 != null && !s2.isEmpty()
					&& !s2.equalsIgnoreCase("implementingRule")
					&& !s2.equalsIgnoreCase("technicalGuidance")) {
				MessageContext mc = result().addError(null, 203, "obligation",
						inClass().name() + "." + pn, s2);
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			} else
				if (s2 != null && !s2.isEmpty() && (s == null || s.isEmpty())) {
				MessageContext mc = result().addError(null, 203, "obligation",
						inClass().name() + "." + pn, s2);
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
			s = taggedValue("extendableMyMS");
			if (s != null && !s.isEmpty()) {
				MessageContext mc = result().addError(null, 204,
						"extendableMyMS", inClass().name() + "." + pn);
				if (mc != null)
					mc.addDetail(null, 400, "Property", fullName());
			}
		}

		if (matches("req-xsd-prop-data-type") && ci != null
				&& (ci.category() == Options.DATATYPE
						|| ci.category() == Options.UNION)
				&& !isComposition() && !isAttribute() && isNavigable()) {
			MessageContext mc = result().addError(null, 148, inClass().name(),
					name(), ci.name());
			if (mc != null)
				mc.addDetail(null, 400, "Property", fullName());
		}

		postprocessed = true;
	}

	/**
	 * Default implementation investigates the initial value set for the
	 * property. If an initial value is set and it contains the String {frozen}
	 * or {readOnly} then this evaluates to true, else false.
	 * 
	 * Subclasses of PropertyInfoImpl can override this method if they have a
	 * more direct way to determine if the property is readOnly.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isReadOnly()
	 */
	public boolean isReadOnly() {

		if (initialValue() != null && !initialValue().isEmpty()
				&& (initialValue().indexOf("{frozen}") > 0
						|| initialValue().indexOf("{readOnly}") > 0)) {
			return true;
		} else {
			return false;
		}
	}

	public Qualifier qualifier(String name) {
		if (qualifiers == null)
			return null;

		for (Qualifier q : qualifiers) {
			if (q.name.equals(name))
				return q;
		}

		return null;
	}

	public Vector<Qualifier> qualifiers() {
		return qualifiers;
	}

	/**
	 * @return 'inline' or 'byreference' if one of these two options can be
	 *         identified for the property, otherwise the empty string ("")
	 */
	public String inlineOrByReferenceFromEncodingRule() {

		String s = "";

		// does AIXM encoding apply?
		if (this.options().isAIXM()) {
			/*
			 * in the AIXM encoding, features are always referenced while
			 * objects are always inline
			 */
			if (this.categoryOfValue() == Options.FEATURE) {
				s = "byreference";
			} else if (this.categoryOfValue() == Options.OBJECT) {
				s = "inline";
			}
		}

		return s;
	}

	/** Find out whether the property owns the stereotype voidable. */
	public boolean voidable() {
		// Validate cache
		validateStereotypesCache();
		// get info from the cache
		boolean res = stereotypesCache.contains("voidable");
		// if not set, check also nillable tagged value (used by GeoSciML)
		if (!res) {
			String s = taggedValue("nillable");
			if (s != null)
				res = s.equalsIgnoreCase("true");
		}
		return res;
	} // voidable()

}
