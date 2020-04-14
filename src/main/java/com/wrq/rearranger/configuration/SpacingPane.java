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
package com.wrq.rearranger.configuration;

import com.wrq.rearranger.settings.ForceBlankLineSetting;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.util.Constraints;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * UI code for Spacing rules dialog.
 */
class SpacingPane {

// ------------------------------ FIELDS ------------------------------

	private RearrangerSettings settings;

// --------------------------- CONSTRUCTORS ---------------------------

	SpacingPane(RearrangerSettings settings) {
		this.settings = settings;
	}

// -------------------------- OTHER METHODS --------------------------

	public JPanel getPane() {
		JPanel result = new JPanel(new GridBagLayout());
		Border border = BorderFactory.createEtchedBorder();

		result.setBorder(border);

		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);

		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1.0d;
		constraints.insets = new Insets(3, 3, 0, 0);
		result.add(getForceBlankLinePanel(settings.getAfterClassLBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getBeforeClassRBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getAfterClassRBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getBeforeMethodLBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getAfterMethodLBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getBeforeMethodRBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getAfterMethodRBrace()), constraints.weightedLastCol());
		constraints.newRow();
		result.add(getForceBlankLinePanel(settings.getNewlinesAtEOF()), constraints.weightedLastCol());
		constraints.weightedLastRow();

		JCheckBox insideBlockBox = new JCheckBox("Remove initial and final blank lines inside code block");

		insideBlockBox.setSelected(settings.isRemoveBlanksInsideCodeBlocks());
		result.add(insideBlockBox, constraints.weightedLastCol());
		insideBlockBox.addActionListener(event -> settings.setRemoveBlanksInsideCodeBlocks(insideBlockBox.isSelected()));
		return result;
	}

	private static JPanel getForceBlankLinePanel(ForceBlankLineSetting forceBlankLineSetting) {
		JPanel result = new JPanel(new GridBagLayout());
		Constraints constraints = new Constraints();

		constraints.fill = GridBagConstraints.BOTH;
		constraints.lastRow();

		JCheckBox forceBox = new JCheckBox("Force");

		forceBox.setSelected(forceBlankLineSetting.isForce());
		result.add(forceBox, constraints.weightedFirstCol());

		NumberFormat integerInstance = NumberFormat.getIntegerInstance();

		integerInstance.setMaximumIntegerDigits(2);
		integerInstance.setMinimumIntegerDigits(1);

		JFormattedTextField blankLineCount = new JFormattedTextField(integerInstance);

		blankLineCount.setValue(Integer.valueOf("88"));

		Dimension preferredSize = blankLineCount.getPreferredSize();

		preferredSize.width += 3;

		blankLineCount.setPreferredSize(preferredSize);
		blankLineCount.setValue(forceBlankLineSetting.getBlankLineCount());
		blankLineCount.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
		constraints.insets = new Insets(0, 3, 0, 0);
		result.add(blankLineCount, constraints.weightedNextCol());
		blankLineCount.addPropertyChangeListener("value", event -> {
			int n = ((Number) blankLineCount.getValue()).intValue();

			if (n < 0) {
				n = 0;
				blankLineCount.setValue(n);
			}
			forceBlankLineSetting.setBlankLineCount(n);
		});

		String labelText = "blank lines " + (forceBlankLineSetting.isBefore() ? "before" : "after");

		switch (forceBlankLineSetting.getObject()) {
			case ForceBlankLineSetting.CLASS_OBJECT:
				labelText += " class ";
				break;
			case ForceBlankLineSetting.METHOD_OBJECT:
				labelText += " method ";
				break;
		}
		labelText += forceBlankLineSetting.isOpenBrace() ? "open brace \"{\""
				: "close brace \"}\"";
		if (forceBlankLineSetting.getObject() == ForceBlankLineSetting.EOF_OBJECT) {
			labelText = "newline characters at end of file";
		}

		JLabel blankLineLabel = new JLabel(labelText);

		result.add(blankLineLabel, constraints.lastCol());
		blankLineCount.setEnabled(forceBox.isSelected());
		blankLineLabel.setEnabled(forceBox.isSelected());
		forceBox.addActionListener(event -> {
			forceBlankLineSetting.setForce(forceBox.isSelected());
			blankLineCount.setEnabled(forceBox.isSelected());
			blankLineLabel.setEnabled(forceBox.isSelected());
		});
		return result;
	}

}
