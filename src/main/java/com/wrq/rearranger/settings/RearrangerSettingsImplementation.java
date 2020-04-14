/*
 * Copyright (c) 2003, 2010, Dave Kriewall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wrq.rearranger.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.XCollection;
import com.wrq.rearranger.RearrangerImplementation;
import com.wrq.rearranger.settings.attributeGroups.AttributeGroup;
import com.wrq.rearranger.settings.attributeGroups.ClassAttributes;
import com.wrq.rearranger.settings.attributeGroups.FieldAttributes;
import com.wrq.rearranger.settings.attributeGroups.GetterSetterDefinition;
import com.wrq.rearranger.settings.attributeGroups.InnerClassAttributes;
import com.wrq.rearranger.settings.attributeGroups.InterfaceAttributes;
import com.wrq.rearranger.settings.attributeGroups.ItemAttributes;
import com.wrq.rearranger.settings.attributeGroups.MethodAttributes;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.input.sax.SAXEngine;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Holds two lists of rearrangement settings, one for the outer level classes, the other for class members.  Each list
 * contains objects of type CommonAttributes.
 */
public class RearrangerSettingsImplementation implements RearrangerSettings {

// ------------------------------ FIELDS ------------------------------

	public static final int OVERLOADED_ORDER_RETAIN_ORIGINAL = 0;

	public static final int OVERLOADED_ORDER_ASCENDING_PARAMETERS = 1;

	public static final int OVERLOADED_ORDER_DESCENDING_PARAMETERS = 2;

	private static final Logger logger = Logger.getInstance(RearrangerSettingsImplementation.class);

	private List<AttributeGroup> itemOrderAttributeList;

	private List<AttributeGroup> classOrderAttributeList;

	private RelatedMethodsSettings relatedMethodsSettings;

	private boolean keepGettersSettersTogether;

	private boolean keepGettersSettersWithProperty;

	private boolean keepOverloadedMethodsTogether;

	private String globalCommentPattern;

	private boolean askBeforeRearranging;

	private boolean rearrangeInnerClasses;

	private boolean showParameterTypes;

	private boolean showParameterNames;

	private boolean showFields;

	private boolean showTypeAfterMethod;

	private boolean showRules;

	private boolean showMatchedRules;

	private boolean showComments;

	private ForceBlankLineSetting afterClassLBrace;

	private ForceBlankLineSetting afterClassRBrace;

	private ForceBlankLineSetting beforeClassRBrace;

	private ForceBlankLineSetting beforeMethodLBrace;

	private ForceBlankLineSetting afterMethodLBrace;

	private ForceBlankLineSetting afterMethodRBrace;

	private ForceBlankLineSetting beforeMethodRBrace;

	private boolean removeBlanksInsideCodeBlocks;

	private ForceBlankLineSetting newlinesAtEOF;

	private int overloadedOrder;

	private GetterSetterDefinition defaultGSDefinition;

	private List<PrimaryMethodSetting> primaryMethodList;

// -------------------------- STATIC METHODS --------------------------

	public static RearrangerSettings getDefaultSettings() {
		URL settingsURL = RearrangerSettingsImplementation.class.getClassLoader().getResource(
				"com/wrq/rearranger/defaultConfiguration.xml"
		);

		assert settingsURL != null;

		try (InputStream inputStream = settingsURL.openStream()) {
			return getSettingsFromStream(inputStream);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static RearrangerSettings getSettingsFromStream(InputStream inputStream) {
		Document document;
		try {
			SAXEngine builder = new SAXBuilder();

			document = builder.build(inputStream);
		} catch (JDOMException jde) {
			logger.debug("JDOM exception building document:" + jde);
			return null;
		} catch (IOException e) {
			logger.debug("I/O exception building document:" + e);
			return null;
		}

		Element app = document.getRootElement();
		Element component = null;

		if (app.getName().equals("application")) {
			for (Element child : app.getChildren()) {
				if (child.getName().equals("component") &&
						child.getAttribute("name") != null &&
						child.getAttribute("name").getValue().equals(RearrangerImplementation.COMPONENT_NAME)) {
					component = child;
					break;
				}
			}
		} else {
			if (app.getName().equals("component") &&
					app.getAttribute("name") != null &&
					app.getAttribute("name").getValue().equals(RearrangerImplementation.COMPONENT_NAME)) {
				component = app;
			}
		}
		if (component != null) {
			Element rearranger = component.getChild(RearrangerImplementation.COMPONENT_NAME);

			if (rearranger != null) {
				RearrangerSettings result = new RearrangerSettingsImplementation();

				result.readExternal(rearranger);
				return result;
			}
		}
		return null;
	}

	/**
	 * @param entry JDOM element named "Rearranger" which contains setting values as attributes.
	 */
	@Override
	public void readExternal(Element entry) {
		Element items = entry.getChild("Items");
		Element classes = entry.getChild("Classes");
		List<Element> itemList = items.getChildren();
		List<Element> classList = classes.getChildren();

		for (Element element : itemList) {
			itemOrderAttributeList.add(ItemAttributes.readExternal(element));
		}
		for (Element element : classList) {
			classOrderAttributeList.add(ClassAttributes.readExternal(element));
		}

		Element gsd = entry.getChild("DefaultGetterSetterDefinition");

		defaultGSDefinition = GetterSetterDefinition.readExternal(gsd);

		Element relatedItems = entry.getChild("RelatedMethods");

		relatedMethodsSettings = RelatedMethodsSettings.readExternal(relatedItems);
		keepGettersSettersTogether = getBooleanAttribute(entry, "KeepGettersSettersTogether", true);
		keepGettersSettersWithProperty = getBooleanAttribute(entry, "KeepGettersSettersWithProperty", false);
		keepOverloadedMethodsTogether = getBooleanAttribute(entry, "KeepOverloadedMethodsTogether", true);

		Attribute attribute = getAttribute(entry, "globalCommentPattern");

		askBeforeRearranging = getBooleanAttribute(entry, "ConfirmBeforeRearranging", false);
		rearrangeInnerClasses = getBooleanAttribute(entry, "RearrangeInnerClasses", false);
		globalCommentPattern = attribute == null ? "" : attribute.getValue();
		overloadedOrder = getIntAttribute(entry, "overloadedOrder", OVERLOADED_ORDER_RETAIN_ORIGINAL);
		showParameterTypes = getBooleanAttribute(entry, "ShowParameterTypes", true);
		showParameterNames = getBooleanAttribute(entry, "ShowParameterNames", true);
		showFields = getBooleanAttribute(entry, "ShowFields", true);
		showRules = getBooleanAttribute(entry, "ShowRules", false);
		showMatchedRules = getBooleanAttribute(entry, "ShowMatchedRules", false);
		showComments = getBooleanAttribute(entry, "ShowComments", false);
		showTypeAfterMethod = getBooleanAttribute(entry, "ShowTypeAfterMethod", true);
		removeBlanksInsideCodeBlocks = getBooleanAttribute(entry, "RemoveBlanksInsideCodeBlocks", false);
		afterClassLBrace = ForceBlankLineSetting.readExternal(
				entry,
				false,
				true,
				ForceBlankLineSetting.CLASS_OBJECT,
				"AfterClassLBrace"
		);
		afterClassRBrace = ForceBlankLineSetting.readExternal(
				entry,
				false,
				false,
				ForceBlankLineSetting.CLASS_OBJECT,
				"AfterClassRBrace"
		);
		beforeClassRBrace = ForceBlankLineSetting.readExternal(
				entry,
				true,
				false,
				ForceBlankLineSetting.CLASS_OBJECT,
				"BeforeClassRBrace"
		);
		beforeMethodLBrace = ForceBlankLineSetting.readExternal(
				entry,
				true,
				true,
				ForceBlankLineSetting.METHOD_OBJECT,
				"BeforeMethodLBrace"
		);
		afterMethodLBrace = ForceBlankLineSetting.readExternal(
				entry,
				false,
				true,
				ForceBlankLineSetting.METHOD_OBJECT,
				"AfterMethodLBrace"
		);
		afterMethodRBrace = ForceBlankLineSetting.readExternal(
				entry,
				false,
				false,
				ForceBlankLineSetting.METHOD_OBJECT,
				"AfterMethodRBrace"
		);
		beforeMethodRBrace = ForceBlankLineSetting.readExternal(
				entry,
				true,
				false,
				ForceBlankLineSetting.METHOD_OBJECT,
				"BeforeMethodRBrace"
		);
		newlinesAtEOF = ForceBlankLineSetting.readExternal(
				entry,
				false,
				false,
				ForceBlankLineSetting.EOF_OBJECT,
				"NewlinesAtEOF"
		);
	}

	public static Attribute getAttribute(Element element, String attributeName) {
		if (element == null) {
			return null;
		}
		return element.getAttribute(attributeName);
	}

	public static int getIntAttribute(Element me, String attr) {
		return getIntAttribute(me, attr, 0);
	}

	public static int getIntAttribute(Element item, String attributeName, int defaultValue) {
		if (item == null) {
			return defaultValue;
		}

		Attribute a = item.getAttribute(attributeName);

		if (a == null) {
			return defaultValue;
		}

		String r = a.getValue();

		if (r == null) {
			return defaultValue;
		}
		if (r.isEmpty()) {
			return defaultValue;
		}
		return Integer.parseInt(r);
	}

	public static boolean getBooleanAttribute(Element me, String attr) {
		return getBooleanAttribute(me, attr, false);
	}

	public static boolean getBooleanAttribute(Element me, String attr, boolean defaultValue) {
		if (me == null) {
			return defaultValue;
		}
		Attribute a = me.getAttribute(attr);

		if (a == null) {
			return defaultValue;
		}

		String r = a.getValue();

		if (r == null) {
			return defaultValue;
		}
		return r.equalsIgnoreCase("true");
	}

	public static RearrangerSettings getSettingsFromFile(File file) {
		try {
			return getSettingsFromStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			logger.debug("getSettingsFromFile:" + e);
		}
		return null;
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public RearrangerSettingsImplementation() {
		itemOrderAttributeList = new ArrayList<>();
		classOrderAttributeList = new ArrayList<>();
		relatedMethodsSettings = new RelatedMethodsSettings();
		keepGettersSettersTogether = false;
		keepGettersSettersWithProperty = false;
		keepOverloadedMethodsTogether = false;
		globalCommentPattern = "";
		overloadedOrder = OVERLOADED_ORDER_RETAIN_ORIGINAL;
		askBeforeRearranging = false;
		rearrangeInnerClasses = false;
		showParameterTypes = true;
		showParameterNames = false;
		showFields = true;
		showTypeAfterMethod = true;
		showRules = false;
		showMatchedRules = false;
		showComments = false;
		removeBlanksInsideCodeBlocks = false;
		afterClassLBrace = new ForceBlankLineSetting(
				false,
				true,
				ForceBlankLineSetting.CLASS_OBJECT,
				"AfterClassLBrace"
		);
		afterClassRBrace = new ForceBlankLineSetting(
				false,
				false,
				ForceBlankLineSetting.CLASS_OBJECT,
				"AfterClassRBrace"
		);
		beforeClassRBrace = new ForceBlankLineSetting(
				true,
				false,
				ForceBlankLineSetting.CLASS_OBJECT,
				"BeforeClassRBrace"
		);
		beforeMethodLBrace = new ForceBlankLineSetting(
				true,
				true,
				ForceBlankLineSetting.METHOD_OBJECT,
				"BeforeMethodLBrace"
		);
		afterMethodLBrace = new ForceBlankLineSetting(
				false,
				true,
				ForceBlankLineSetting.METHOD_OBJECT,
				"AfterMethodLBrace"
		);
		beforeMethodRBrace = new ForceBlankLineSetting(
				true,
				false,
				ForceBlankLineSetting.METHOD_OBJECT,
				"BeforeMethodRBrace"
		);
		afterMethodRBrace = new ForceBlankLineSetting(
				false,
				false,
				ForceBlankLineSetting.METHOD_OBJECT,
				"AfterMethodRBrace"
		);
		newlinesAtEOF = new ForceBlankLineSetting(false, false, ForceBlankLineSetting.EOF_OBJECT, "NewlinesAtEOF");
		defaultGSDefinition = new GetterSetterDefinition();
		primaryMethodList = new LinkedList<PrimaryMethodSetting>();
	}

	private RearrangerSettingsImplementation(RearrangerSettings other) {
		this();
		for (AttributeGroup itemAttributes : other.getItemOrderAttributeList()) {
			AttributeGroup deepCopy = itemAttributes.deepCopy();

			itemOrderAttributeList.add(deepCopy);
		}
		for (AttributeGroup classAttributes : other.getClassOrderAttributeList()) {
			AttributeGroup deepCopy = classAttributes.deepCopy();

			classOrderAttributeList.add(deepCopy);
		}
		relatedMethodsSettings = other.getRelatedMethodsSettings().deepCopy();
		keepGettersSettersTogether = other.isKeepGettersSettersTogether();
		keepGettersSettersWithProperty = other.isKeepGettersSettersWithProperty();
		keepOverloadedMethodsTogether = other.isKeepOverloadedMethodsTogether();
		globalCommentPattern = other.getGlobalCommentPattern();
		overloadedOrder = other.getOverloadedOrder();
		askBeforeRearranging = other.isAskBeforeRearranging();
		rearrangeInnerClasses = other.isRearrangeInnerClasses();
		showParameterNames = other.isShowParameterNames();
		showParameterTypes = other.isShowParameterTypes();
		showFields = other.isShowFields();
		showTypeAfterMethod = other.isShowTypeAfterMethod();
		showRules = other.isShowRules();
		showMatchedRules = other.isShowMatchedRules();
		showComments = other.isShowComments();
		removeBlanksInsideCodeBlocks = other.isRemoveBlanksInsideCodeBlocks();
		afterClassLBrace = other.getAfterClassLBrace().deepCopy();
		afterClassRBrace = other.getAfterClassRBrace().deepCopy();
		beforeClassRBrace = other.getBeforeClassRBrace().deepCopy();
		beforeMethodLBrace = other.getBeforeMethodLBrace().deepCopy();
		afterMethodLBrace = other.getAfterMethodLBrace().deepCopy();
		afterMethodRBrace = other.getAfterMethodRBrace().deepCopy();
		beforeMethodRBrace = other.getBeforeMethodRBrace().deepCopy();
		newlinesAtEOF = other.getNewlinesAtEOF().deepCopy();
		defaultGSDefinition = other.getDefaultGSDefinition().deepCopy();
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	@Override
	public ForceBlankLineSetting getAfterClassLBrace() {
		return afterClassLBrace;
	}

	@Override
	public void setAfterClassLBrace(ForceBlankLineSetting value) {
		afterClassLBrace = value;
	}

	@Override
	public ForceBlankLineSetting getAfterClassRBrace() {
		return afterClassRBrace;
	}

	@Override
	public void setAfterClassRBrace(ForceBlankLineSetting value) {
		afterClassRBrace = value;
	}

	@Override
	public ForceBlankLineSetting getAfterMethodLBrace() {
		return afterMethodLBrace;
	}

	@Override
	public void setAfterMethodLBrace(ForceBlankLineSetting value) {
		afterMethodLBrace = value;
	}

	@Override
	public ForceBlankLineSetting getAfterMethodRBrace() {
		return afterMethodRBrace;
	}

	@Override
	public void setAfterMethodRBrace(ForceBlankLineSetting value) {
		afterMethodRBrace = value;
	}

	@Override
	public ForceBlankLineSetting getBeforeClassRBrace() {
		return beforeClassRBrace;
	}

	@Override
	public void setBeforeClassRBrace(ForceBlankLineSetting value) {
		beforeClassRBrace = value;
	}

	@Override
	public ForceBlankLineSetting getBeforeMethodLBrace() {
		return beforeMethodLBrace;
	}

	@Override
	public void setBeforeMethodLBrace(ForceBlankLineSetting value) {
		beforeMethodLBrace = value;
	}

	@Override
	public ForceBlankLineSetting getBeforeMethodRBrace() {
		return beforeMethodRBrace;
	}

	@Override
	public void setBeforeMethodRBrace(ForceBlankLineSetting value) {
		beforeMethodRBrace = value;
	}

	@XCollection(elementTypes = {ClassAttributes.class})
	@Override
	public List<AttributeGroup> getClassOrderAttributeList() {
		return classOrderAttributeList;
	}

	@Override
	public void setClassOrderAttributeList(List<AttributeGroup> value) {
		classOrderAttributeList = value;
	}

	@Override
	public GetterSetterDefinition getDefaultGSDefinition() {
		return defaultGSDefinition;
	}

	@Override
	public void setDefaultGSDefinition(GetterSetterDefinition value) {
		defaultGSDefinition = value;
	}

	@Override
	public String getGlobalCommentPattern() {
		return globalCommentPattern;
	}

	@Override
	public void setGlobalCommentPattern(String globalCommentPattern) {
		this.globalCommentPattern = globalCommentPattern;
	}

	@XCollection(elementTypes = {
			FieldAttributes.class,
			MethodAttributes.class,
			InnerClassAttributes.class,
			InterfaceAttributes.class,
			CommentRule.class
	})
	@Override
	public List<AttributeGroup> getItemOrderAttributeList() {
		return itemOrderAttributeList;
	}

	@Override
	public void setItemOrderAttributeList(List<AttributeGroup> value) {
		itemOrderAttributeList = value;
	}

	@Override
	public ForceBlankLineSetting getNewlinesAtEOF() {
		return newlinesAtEOF;
	}

	@Override
	public void setNewlinesAtEOF(ForceBlankLineSetting value) {
		newlinesAtEOF = value;
	}

	@Override
	public int getOverloadedOrder() {
		return overloadedOrder;
	}

	@Override
	public void setOverloadedOrder(int overloadedOrder) {
		this.overloadedOrder = overloadedOrder;
	}

	@Override
	public RelatedMethodsSettings getRelatedMethodsSettings() {
		return relatedMethodsSettings;
	}

	@Override
	public void setRelatedMethodsSettings(RelatedMethodsSettings value) {
		relatedMethodsSettings = value.deepCopy();
	}

	@Override
	public boolean isAskBeforeRearranging() {
		return askBeforeRearranging;
	}

	@Override
	public void setAskBeforeRearranging(boolean askBeforeRearranging) {
		this.askBeforeRearranging = askBeforeRearranging;
	}

	@Override
	public boolean isKeepGettersSettersTogether() {
		return keepGettersSettersTogether;
	}

	@Override
	public void setKeepGettersSettersTogether(boolean keepGettersSettersTogether) {
		this.keepGettersSettersTogether = keepGettersSettersTogether;
	}

	@Override
	public boolean isKeepGettersSettersWithProperty() {
		return keepGettersSettersWithProperty;
	}

	@Override
	public void setKeepGettersSettersWithProperty(boolean keepGettersSettersWithProperty) {
		this.keepGettersSettersWithProperty = keepGettersSettersWithProperty;
	}

	@Override
	public boolean isKeepOverloadedMethodsTogether() {
		return keepOverloadedMethodsTogether;
	}

	@Override
	public void setKeepOverloadedMethodsTogether(boolean keepOverloadedMethodsTogether) {
		this.keepOverloadedMethodsTogether = keepOverloadedMethodsTogether;
	}

	@Override
	public boolean isRearrangeInnerClasses() {
		return rearrangeInnerClasses;
	}

	@Override
	public void setRearrangeInnerClasses(boolean rearrangeInnerClasses) {
		this.rearrangeInnerClasses = rearrangeInnerClasses;
	}

	@Override
	public boolean isRemoveBlanksInsideCodeBlocks() {
		return removeBlanksInsideCodeBlocks;
	}

	@Override
	public void setRemoveBlanksInsideCodeBlocks(boolean removeBlanksInsideCodeBlocks) {
		this.removeBlanksInsideCodeBlocks = removeBlanksInsideCodeBlocks;
	}

	@Override
	public boolean isShowComments() {
		return showComments;
	}

	@Override
	public void setShowComments(boolean showComments) {
		this.showComments = showComments;
	}

	@Override
	public boolean isShowFields() {
		return showFields;
	}

	@Override
	public void setShowFields(boolean showFields) {
		this.showFields = showFields;
	}

	@Override
	public boolean isShowMatchedRules() {
		return showMatchedRules;
	}

	@Override
	public void setShowMatchedRules(boolean showMatchedRules) {
		this.showMatchedRules = showMatchedRules;
	}

	@Override
	public boolean isShowParameterNames() {
		return showParameterNames;
	}

	@Override
	public void setShowParameterNames(boolean showParameterNames) {
		this.showParameterNames = showParameterNames;
	}

	@Override
	public boolean isShowParameterTypes() {
		return showParameterTypes;
	}

	@Override
	public void setShowParameterTypes(boolean showParameterTypes) {
		this.showParameterTypes = showParameterTypes;
	}

	@Override
	public boolean isShowRules() {
		return showRules;
	}

	@Override
	public void setShowRules(boolean showRules) {
		this.showRules = showRules;
	}

	@Override
	public boolean isShowTypeAfterMethod() {
		return showTypeAfterMethod;
	}

	@Override
	public void setShowTypeAfterMethod(boolean showTypeAfterMethod) {
		this.showTypeAfterMethod = showTypeAfterMethod;
	}

	@Override
	public void setPrimaryMethodList(List<PrimaryMethodSetting> value) {
		primaryMethodList = value;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		RearrangerSettings other;

		return value instanceof RearrangerSettings &&
				getClassOrderAttributeList().equals((other = (RearrangerSettings) value).getClassOrderAttributeList()) &&
				getAfterClassLBrace().equals(other.getAfterClassLBrace()) && //false
				getAfterClassRBrace().equals(other.getAfterClassRBrace()) &&
				getBeforeClassRBrace().equals(other.getBeforeClassRBrace()) && //false
				getBeforeMethodLBrace().equals(other.getBeforeMethodLBrace()) && //false
				getAfterMethodLBrace().equals(other.getAfterMethodLBrace()) && //false
				getAfterMethodRBrace().equals(other.getAfterMethodRBrace()) && //false
				getBeforeMethodRBrace().equals(other.getBeforeMethodRBrace()) && //false
				getNewlinesAtEOF().equals(other.getNewlinesAtEOF()) && //false
				getItemOrderAttributeList().equals(other.getItemOrderAttributeList()) && //false
				getExtractedMethodsSettings().equals(other.getExtractedMethodsSettings()) &&
				isKeepGettersSettersTogether() == other.isKeepGettersSettersTogether() &&
				isKeepGettersSettersWithProperty() == other.isKeepGettersSettersWithProperty() &&
				isKeepOverloadedMethodsTogether() == other.isKeepOverloadedMethodsTogether() &&
				getOverloadedOrder() == other.getOverloadedOrder() &&
				getGlobalCommentPattern() == other.getGlobalCommentPattern() &&
				isAskBeforeRearranging() == other.isAskBeforeRearranging() &&
				isRearrangeInnerClasses() == other.isRearrangeInnerClasses() &&
				isShowParameterTypes() == other.isShowParameterTypes() &&
				isShowParameterNames() == other.isShowParameterNames() &&
				isShowFields() == other.isShowFields() &&
				isShowTypeAfterMethod() == other.isShowTypeAfterMethod() &&
				isShowRules() == other.isShowRules() &&
				isShowMatchedRules() == other.isShowMatchedRules() &&
				isShowComments() == other.isShowComments() &&
				isShowParameterNames() == other.isShowParameterNames() &&
				isRemoveBlanksInsideCodeBlocks() == other.isRemoveBlanksInsideCodeBlocks() &&
				getDefaultGSDefinition().equals(other.getDefaultGSDefinition());
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface RearrangerSettings ---------------------

	@Override
	public RelatedMethodsSettings getExtractedMethodsSettings() {
		return relatedMethodsSettings;
	}

	@Override
	public void writeExternal(Element entry) {
		Element items = new Element("Items");
		Element classes = new Element("Classes");

		entry.getChildren().add(items);
		entry.getChildren().add(classes);
		for (AttributeGroup item : itemOrderAttributeList) {
			item.writeExternal(items);
		}
		for (AttributeGroup attributes : classOrderAttributeList) {
			attributes.writeExternal(classes);
		}

		Element gsd = new Element("DefaultGetterSetterDefinition");

		entry.getChildren().add(gsd);
		defaultGSDefinition.writeExternal(gsd);

		Element relatedItems = new Element("RelatedMethods");

		entry.getChildren().add(relatedItems);
		entry.setAttribute("KeepGettersSettersTogether", Boolean.valueOf(keepGettersSettersTogether).toString());
		entry.setAttribute(
				"KeepGettersSettersWithProperty",
				Boolean.valueOf(keepGettersSettersWithProperty).toString()
		);
		entry.setAttribute("KeepOverloadedMethodsTogether", Boolean.valueOf(keepOverloadedMethodsTogether).toString());
		entry.setAttribute("ConfirmBeforeRearranging", Boolean.valueOf(askBeforeRearranging).toString());
		entry.setAttribute("RearrangeInnerClasses", Boolean.valueOf(rearrangeInnerClasses).toString());
		entry.setAttribute("globalCommentPattern", globalCommentPattern);
		entry.setAttribute("overloadedOrder", "" + overloadedOrder);
		entry.setAttribute("ShowParameterTypes", Boolean.valueOf(showParameterTypes).toString());
		entry.setAttribute("ShowParameterNames", Boolean.valueOf(showParameterNames).toString());
		entry.setAttribute("ShowFields", Boolean.valueOf(showFields).toString());
		entry.setAttribute("ShowTypeAfterMethod", Boolean.valueOf(showTypeAfterMethod).toString());
		entry.setAttribute("ShowRules", Boolean.valueOf(showRules).toString());
		entry.setAttribute("ShowMatchedRules", Boolean.valueOf(showMatchedRules).toString());
		entry.setAttribute("ShowComments", Boolean.valueOf(showComments).toString());
		entry.setAttribute("RemoveBlanksInsideCodeBlocks", Boolean.valueOf(removeBlanksInsideCodeBlocks).toString());
		relatedMethodsSettings.writeExternal(relatedItems);
		afterClassLBrace.writeExternal(entry);
		afterClassRBrace.writeExternal(entry);
		beforeClassRBrace.writeExternal(entry);
		beforeMethodLBrace.writeExternal(entry);
		afterMethodLBrace.writeExternal(entry);
		afterMethodRBrace.writeExternal(entry);
		beforeMethodRBrace.writeExternal(entry);
		newlinesAtEOF.writeExternal(entry);
	}

	@Override
	public void writeSettingsToFile(File file) {
		Element component = new Element("component");
		component.setAttribute("name", RearrangerImplementation.COMPONENT_NAME);
		Element r = new Element(RearrangerImplementation.COMPONENT_NAME);
		component.getChildren().add(r);
		writeExternal(r);
		Format format = Format.getPrettyFormat();
		XMLOutputter outputter = new XMLOutputter(format);
		try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			outputter.output(component, fileOutputStream);
		} catch (FileNotFoundException exception) {
			throw new IllegalStateException(exception);
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	@Override
	public RearrangerSettings deepCopy() {
		return new RearrangerSettingsImplementation(this);
	}

// -------------------------- OTHER METHODS --------------------------

	public void addClass(AttributeGroup ca, int index) {
		if (classOrderAttributeList.size() < index) {
			classOrderAttributeList.add(ca);
		} else {
			classOrderAttributeList.add(index, ca);
		}
	}

	public void addItem(AttributeGroup ia, int index) {
		if (itemOrderAttributeList.size() < index) {
			itemOrderAttributeList.add(ia);
		} else {
			itemOrderAttributeList.add(index, ia);
		}
	}

}
