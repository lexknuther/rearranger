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
package com.wrq.rearranger.settings.atomicAttributes;

import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.util.Constraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Allows item selection by matching a string attribute to a regular expression.
 */
public abstract class StringAttribute extends AtomicAttribute {

// ------------------------------ FIELDS ------------------------------

	private String expression;

	private boolean match;

	private boolean invert;

	private final String attributeDisplayName;  // should be plural

	private final String storageName;

// --------------------------- CONSTRUCTORS ---------------------------

	StringAttribute(String attributeDisplayName, String storageName) {
		this.attributeDisplayName = attributeDisplayName;
		this.storageName = storageName;
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	public String getExpression() {
		return expression;
	}

	public void setExpression(final String expression) {
		this.expression = expression;
	}

	public JPanel getStringPanel() {
		JPanel stringPanel = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints();

		constraints.weightedLastRow();

		JCheckBox enableBox = new JCheckBox("whose " + attributeDisplayName);

		stringPanel.add(enableBox, constraints.firstCol());

		JComboBox invertBox = new JComboBox(new Object[]{"match", "do not match"});

		invertBox.setSelectedIndex(isInvert() ? 1 : 0);

		JTextField patternField = new JTextField(20);

		stringPanel.add(invertBox, constraints.nextCol());
		stringPanel.add(patternField, constraints.weightedLastCol());
		enableBox.setSelected(isMatch());
		invertBox.setEnabled(isMatch());
		patternField.setEnabled(isMatch());
		patternField.setText(getExpression());
		enableBox.addActionListener(e -> {
			setMatch(enableBox.isSelected());
			invertBox.setEnabled(enableBox.isSelected());
			patternField.setEnabled(enableBox.isSelected());
		});
		invertBox.addActionListener(e -> {
			setInvert(invertBox.getSelectedIndex() == 1);
		});
		patternField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(final DocumentEvent e) {
				setExpression(patternField.getText());
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				setExpression(patternField.getText());
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				setExpression(patternField.getText());
			}

		});
		return stringPanel;
	}

	public boolean isInvert() {
		return invert;
	}

	public void setInvert(boolean value) {
		invert = value;
	}

	public final boolean isMatch() {
		return match;
	}

	public final void setMatch(boolean value) {
		match = value;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(final Object object) {
		if (!(object instanceof StringAttribute)) {
			return false;
		}
		StringAttribute na = (StringAttribute) object;

		return match == na.match &&
				(expression == null || na.expression == null ?
						expression == null && na.expression == null : expression.equals(na.expression)) &&
				invert == na.invert;
	}

// -------------------------- OTHER METHODS --------------------------

	public void appendAttributes(final Element me) {
		Element stElement = new Element(storageName);

		if (expression == null) {
			expression = "";
		}
		me.getChildren().add(stElement);
		stElement.setAttribute("match", Boolean.valueOf(match).toString());
		stElement.setAttribute("invert", Boolean.valueOf(invert).toString());
		stElement.setAttribute("pattern", expression);
	}

	protected void deepCopy(StringAttribute result) {
		result.match = match;
		result.expression = expression;
		result.invert = invert;
	}

	public String getDescriptiveString() {
		return match ? "whose " +
				attributeDisplayName +
				(invert ? " do not match '" : " match '") +
				expression + "'"
				: "";
	}

	public boolean isMatch(final String string) {
		return !match || string.matches(expression) ^ invert;
	}

	public void loadAttributes(final Element item) {
		match = RearrangerSettingsImplementation.getBooleanAttribute(item, "match", false);
		invert = RearrangerSettingsImplementation.getBooleanAttribute(item, "invert", false);

		Attribute attr = RearrangerSettingsImplementation.getAttribute(item, "pattern");

		expression = attr == null ? "" : attr.getValue();
	}

}
