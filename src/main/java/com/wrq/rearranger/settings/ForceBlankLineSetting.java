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

import org.jdom.Element;

/**
 * Contains a single "force N blank lines before/after" some brace.
 */
public class ForceBlankLineSetting {

// ------------------------------ FIELDS ------------------------------

	public static final int CLASS_OBJECT = 0;

	public static final int METHOD_OBJECT = 1;

	public static final int EOF_OBJECT = 2;

	private boolean force;

	private int blankLineCount;

	private boolean before;

	private boolean openBrace;

	private int object;

	private String name;

// -------------------------- STATIC METHODS --------------------------

	/**
	 * Read the contents of this object from the JDOM element.
	 *
	 * @param entry JDOM element which contains setting values as attributes.
	 */
	public static ForceBlankLineSetting readExternal(
			Element entry, boolean before, boolean openBrace, int object, String name) {
		Element fblsElement = entry.getChild(name);
		boolean force = RearrangerSettingsImplementation.getBooleanAttribute(fblsElement, "Force", false);
		int nBlankLines = RearrangerSettingsImplementation.getIntAttribute(fblsElement, "nBlankLines", 1);
		ForceBlankLineSetting result = new ForceBlankLineSetting(before, openBrace, object, name);

		result.setForce(force);
		result.setBlankLineCount(nBlankLines);
		return result;
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public ForceBlankLineSetting() {
	}

	ForceBlankLineSetting(boolean before, boolean openBrace, int object, String name) {
		this.before = before;
		this.openBrace = openBrace;
		this.object = object;
		this.name = name;
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	public int getBlankLineCount() {
		return blankLineCount;
	}

	public void setBlankLineCount(int value) {
		blankLineCount = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	public int getObject() {
		return object;
	}

	public void setObject(int value) {
		object = value;
	}

	public boolean isBefore() {
		return before;
	}

	public void setBefore(boolean value) {
		before = value;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isOpenBrace() {
		return openBrace;
	}

	public void setOpenBrace(boolean value) {
		openBrace = value;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		ForceBlankLineSetting other;

		return value instanceof ForceBlankLineSetting &&
				force == (other = (ForceBlankLineSetting) value).force &&
				blankLineCount == other.blankLineCount &&
				openBrace == other.openBrace &&
				object == other.object;
	}

	public String toString() {
		return name + ": " + (before ? "before " : "after ") +
				getObjectName() +
				(openBrace ? " open brace" : " close brace") +
				(force ? ", force " + blankLineCount + " lines" : ", leave intact");
	}

	public String getObjectName() {
		return object == CLASS_OBJECT ? "class" : "method";
	}

// -------------------------- OTHER METHODS --------------------------

	public ForceBlankLineSetting deepCopy() {
		ForceBlankLineSetting result = new ForceBlankLineSetting(before, openBrace, object, name);

		result.force = force;
		result.blankLineCount = blankLineCount;
		return result;
	}

	public void writeExternal(Element entry) {
		Element element = new Element(name);

		entry.getChildren().add(element);
		element.setAttribute("Force", Boolean.toString(force));
		element.setAttribute("nBlankLines", String.valueOf(blankLineCount));
	}

}
