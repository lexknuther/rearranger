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
package com.wrq.rearranger.settings.attributeGroups;

import com.wrq.rearranger.entry.RangeEntry;
import com.wrq.rearranger.ruleinstance.DefaultRuleInstance;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.settings.atomicAttributes.FinalAttribute;
import com.wrq.rearranger.settings.atomicAttributes.NameAttribute;
import com.wrq.rearranger.settings.atomicAttributes.ProtectionLevelAttributes;
import com.wrq.rearranger.settings.atomicAttributes.SortOptions;
import com.wrq.rearranger.settings.atomicAttributes.StaticAttribute;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

/**
 * Routines for Java modifiers common to fields, methods and classes, namely: protection levels (public, private,
 * protected, package), final modifier, and static modifier.  Ability to match the name to a regular expression is also
 * supported.
 */
public abstract class CommonAttributes implements AttributeGroup, IPrioritizableRule {

// ------------------------------ FIELDS ------------------------------

	private ProtectionLevelAttributes plAttr;

	private FinalAttribute fAttr;

	private StaticAttribute stAttr;

	private NameAttribute nameAttr;

	private SortOptions sortAttr;

	private int priority; // 1 = low priority, > 1 is higher priority

// -------------------------- STATIC METHODS --------------------------

	static void readExternal(CommonAttributes result, Element item) {
		result.plAttr = ProtectionLevelAttributes.readExternal(item);
		result.stAttr = StaticAttribute.readExternal(item);
		result.fAttr = FinalAttribute.readExternal(item);
		result.nameAttr = NameAttribute.readExternal(item);
		result.sortAttr = SortOptions.readExternal(item);
		result.priority = RearrangerSettingsImplementation.getIntAttribute(item, "priority", 1);
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public CommonAttributes() {
		plAttr = new ProtectionLevelAttributes();
		fAttr = new FinalAttribute();
		stAttr = new StaticAttribute();
		nameAttr = new NameAttribute();
		sortAttr = new SortOptions();
		priority = 1;
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	public NameAttribute getNameAttr() {
		return nameAttr;
	}

	public void setNameAttr(NameAttribute value) {
		nameAttr = value;
	}

	public ProtectionLevelAttributes getPlAttr() {
		return plAttr;
	}

	public void setPlAttr(ProtectionLevelAttributes value) {
		plAttr = value;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	public SortOptions getSortAttr() {
		return sortAttr;
	}

	public void setSortAttr(SortOptions value) {
		sortAttr = value;
	}

	public final StaticAttribute getStAttr() {
		return stAttr;
	}

	public void setStAttr(StaticAttribute value) {
		stAttr = value;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		CommonAttributes other;

		return value instanceof CommonAttributes &&
				plAttr.equals((other = (CommonAttributes) value).plAttr) &&
				stAttr.equals(other.stAttr) &&
				fAttr.equals(other.fAttr) &&
				nameAttr.equals(other.nameAttr) &&
				sortAttr.equals(other.sortAttr) &&
				priority == other.priority;
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface IRule ---------------------

	/**
	 * Calculates a list of regular expression patterns, one for each comment that the rule might create, which will
	 * match any comment that the rule might generate.  These can be combined to form a global comment pattern which
	 * will match all possible generated comments from all rules.   These patterns will be appended to the supplied
	 * list.
	 */
	@Override
	public void addCommentPatternsToList(List<String> list) {
		return;
	}

	/**
	 * Returns a boolean indicating if the comments associated with this rule match a global comment pattern. Most rules
	 * don't have comments, so by default it returns true.
	 *
	 * @param pattern global pattern to which all comments should conform
	 *
	 * @return true if comments conform to pattern
	 */
	@Override
	public boolean commentsMatchGlobalPattern(String pattern) {
		return true;
	}

	@Override
	public IRuleInstance createRuleInstance() {
		return new DefaultRuleInstance(this);
	}

	@Override
	public int getCommentCount() {
		return 0;
	}

	@Override
	public List<String> getOffendingPatterns(String pattern) {
		return new ArrayList<String>(1);
	}

	@Override
	public boolean isMatch(RangeEntry rangeEntry) {
		return plAttr.isMatch(rangeEntry.getModifiers()) &&
				stAttr.isMatch(rangeEntry.getModifiers()) &&
				fAttr.isMatch(rangeEntry.getModifiers()) &&
				nameAttr.isMatch(rangeEntry.getName());
	}

// -------------------------- OTHER METHODS --------------------------

	final void deepCopyCommonItems(final CommonAttributes result) {
		result.plAttr = (ProtectionLevelAttributes) plAttr.deepCopy();
		result.fAttr = (FinalAttribute) fAttr.deepCopy();
		result.stAttr = (StaticAttribute) stAttr.deepCopy();
		result.nameAttr = (NameAttribute) nameAttr.deepCopy();
		result.sortAttr = (SortOptions) sortAttr.deepCopy();
		result.priority = priority;
	}

	public final FinalAttribute getfAttr() {
		return fAttr;
	}

	public void setfAttr(FinalAttribute value) {
		fAttr = value;
	}

	final void writeExternalCommonAttributes(final Element child) {
		plAttr.appendAttributes(child);
		stAttr.appendAttributes(child);
		fAttr.appendAttributes(child);
		nameAttr.appendAttributes(child);
		sortAttr.appendAttributes(child);
		child.setAttribute("priority", "" + priority);
	}

}

