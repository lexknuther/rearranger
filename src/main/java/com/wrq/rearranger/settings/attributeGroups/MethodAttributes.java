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

import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiMethod;
import com.wrq.rearranger.ModifierConstants;
import com.wrq.rearranger.entry.RangeEntry;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.settings.atomicAttributes.AbstractAttribute;
import com.wrq.rearranger.settings.atomicAttributes.ImplementedAttribute;
import com.wrq.rearranger.settings.atomicAttributes.ImplementingAttribute;
import com.wrq.rearranger.settings.atomicAttributes.InitializerAttribute;
import com.wrq.rearranger.settings.atomicAttributes.MaxParamsAttribute;
import com.wrq.rearranger.settings.atomicAttributes.MinParamsAttribute;
import com.wrq.rearranger.settings.atomicAttributes.NativeAttribute;
import com.wrq.rearranger.settings.atomicAttributes.OverriddenAttribute;
import com.wrq.rearranger.settings.atomicAttributes.OverridingAttribute;
import com.wrq.rearranger.settings.atomicAttributes.ReturnTypeAttribute;
import com.wrq.rearranger.settings.atomicAttributes.SynchronizedAttribute;
import com.wrq.rearranger.util.MethodUtil;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.jdom.Element;

/**
 * Routines to handle method modifiers other than those supported by CommonAttributes.  These are the 'abstract'
 * modifier, a type discriminator (constructor, getter/setter, other), and a boolean flag indicating whether the method
 * is overridden or not.
 */
public class MethodAttributes
		extends ItemAttributes
		implements IRestrictMethodExtraction,
		IHasGetterSetterDefinition {

// ------------------------------ FIELDS ------------------------------

	private AbstractAttribute abstractAttr;

	private OverriddenAttribute overriddenAttr;

	private OverridingAttribute overridingAttr;

	private InitializerAttribute staticInitAttr;

	private NativeAttribute nativeAttr;

	private SynchronizedAttribute syncAttr;

	private ReturnTypeAttribute returnTypeAttr;

	private ImplementedAttribute implementedAttr;

	private ImplementingAttribute implementingAttr;

	private MinParamsAttribute minParamsAttr;

	private MaxParamsAttribute maxParamsAttr;

	private boolean constructorMethodType;

	private boolean getterSetterMethodType;

	private boolean canonicalMethodType;

	private boolean otherMethodType;

	private boolean invertMethodType;

	private boolean noExtractedMethods;

	private GetterSetterDefinition getterSetterDefinition;

	private boolean predicateAdded;

	private String nextPredicate;

// -------------------------- STATIC METHODS --------------------------

	public static /*MethodAttributes*/AttributeGroup readExternal(final Element item) {
		MethodAttributes result = new MethodAttributes();

		CommonAttributes.readExternal(result, item);
		result.abstractAttr = AbstractAttribute.readExternal(item);
		result.syncAttr = SynchronizedAttribute.readExternal(item);
		result.overriddenAttr = OverriddenAttribute.readExternal(item);
		result.overridingAttr = OverridingAttribute.readExternal(item);
		result.staticInitAttr = InitializerAttribute.readExternal(item);
		result.nativeAttr = NativeAttribute.readExternal(item);
		result.returnTypeAttr = ReturnTypeAttribute.readExternal(item);
		result.implementedAttr = ImplementedAttribute.readExternal(item);
		result.implementingAttr = ImplementingAttribute.readExternal(item);
		result.minParamsAttr = MinParamsAttribute.readExternal(item);
		result.maxParamsAttr = MaxParamsAttribute.readExternal(item);

		Element me = item.getChild("Misc");

		result.constructorMethodType = RearrangerSettingsImplementation.getBooleanAttribute(me, "constructorMethod");
		result.getterSetterMethodType = RearrangerSettingsImplementation.getBooleanAttribute(me, "getterSetter");
		result.canonicalMethodType = RearrangerSettingsImplementation.getBooleanAttribute(me, "canonicalMethod");
		result.otherMethodType = RearrangerSettingsImplementation.getBooleanAttribute(me, "otherMethod");
		result.invertMethodType = RearrangerSettingsImplementation.getBooleanAttribute(me, "invertMethod");
		result.noExtractedMethods = RearrangerSettingsImplementation.getBooleanAttribute(me, "noExtractedMethods");
		result.getterSetterDefinition = GetterSetterDefinition.readExternal(item);
		return result;
	}

// --------------------------- CONSTRUCTORS ---------------------------

	public MethodAttributes() {
		init();
		getterSetterDefinition = new GetterSetterDefinition();
	}

	private void init() {
		abstractAttr = new AbstractAttribute();
		overriddenAttr = new OverriddenAttribute();
		overridingAttr = new OverridingAttribute();
		staticInitAttr = new InitializerAttribute();
		nativeAttr = new NativeAttribute();
		syncAttr = new SynchronizedAttribute();
		returnTypeAttr = new ReturnTypeAttribute();
		implementedAttr = new ImplementedAttribute();
		implementingAttr = new ImplementingAttribute();
		minParamsAttr = new MinParamsAttribute();
		maxParamsAttr = new MaxParamsAttribute();
	}

	public MethodAttributes(GetterSetterDefinition defaultGetterSetterDefinition) {
		init();
		getterSetterDefinition = defaultGetterSetterDefinition.deepCopy();
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	public AbstractAttribute getAbstractAttr() {
		return abstractAttr;
	}

	public void setAbstractAttr(AbstractAttribute value) {
		abstractAttr = value;
	}

	private JPanel getExcludePanel() {
		JPanel excludePanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.NONE;
		constraints.weightx = 1.0d;
		constraints.weighty = 0.0d;
		constraints.gridx = constraints.gridy = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = GridBagConstraints.REMAINDER;

		JCheckBox excludeBox = new JCheckBox("Exclude from extracted method processing");

		excludeBox.setSelected(noExtractedMethods);
		excludeBox.addActionListener(event -> {
			noExtractedMethods = excludeBox.isSelected();
		});
		excludePanel.add(excludeBox, constraints);
		return excludePanel;
	}

	@Override
	public GetterSetterDefinition getGetterSetterDefinition() {
		return getterSetterDefinition;
	}

	public void setGetterSetterDefinition(GetterSetterDefinition value) {
		getterSetterDefinition = value;
	}

	public ImplementedAttribute getImplementedAttr() {
		return implementedAttr;
	}

	public void setImplementedAttr(ImplementedAttribute value) {
		implementedAttr = value;
	}

	public ImplementingAttribute getImplementingAttr() {
		return implementingAttr;
	}

	public void setImplementingAttr(ImplementingAttribute value) {
		implementingAttr = value;
	}

	public MaxParamsAttribute getMaxParamsAttr() {
		return maxParamsAttr;
	}

	public void setMaxParamsAttr(MaxParamsAttribute value) {
		maxParamsAttr = value;
	}

	public MinParamsAttribute getMinParamsAttr() {
		return minParamsAttr;
	}

	public void setMinParamsAttr(MinParamsAttribute value) {
		minParamsAttr = value;
	}

	public NativeAttribute getNativeAttr() {
		return nativeAttr;
	}

	public void setNativeAttr(NativeAttribute value) {
		nativeAttr = value;
	}

	public OverriddenAttribute getOverriddenAttr() {
		return overriddenAttr;
	}

	public void setOverriddenAttr(OverriddenAttribute value) {
		overriddenAttr = value;
	}

	public OverridingAttribute getOverridingAttr() {
		return overridingAttr;
	}

	public void setOverridingAttr(OverridingAttribute value) {
		overridingAttr = value;
	}

	public ReturnTypeAttribute getReturnTypeAttr() {
		return returnTypeAttr;
	}

	public void setReturnTypeAttr(ReturnTypeAttribute value) {
		returnTypeAttr = value;
	}

	public InitializerAttribute getStaticInitAttr() {
		return staticInitAttr;
	}

	public void setStaticInitAttr(InitializerAttribute value) {
		staticInitAttr = value;
	}

	public SynchronizedAttribute getSyncAttr() {
		return syncAttr;
	}

	public void setSyncAttr(SynchronizedAttribute value) {
		syncAttr = value;
	}

	public boolean isCanonicalMethodType() {
		return canonicalMethodType;
	}

	public void setCanonicalMethodType(boolean canonicalMethodType) {
		this.canonicalMethodType = canonicalMethodType;
	}

	public boolean isConstructorMethodType() {
		return constructorMethodType;
	}

	public void setConstructorMethodType(boolean constructorMethodType) {
		this.constructorMethodType = constructorMethodType;
	}

	public boolean isGetterSetterMethodType() {
		return getterSetterMethodType;
	}

	public void setGetterSetterMethodType(final boolean getterSetterMethodType) {
		this.getterSetterMethodType = getterSetterMethodType;
	}

	public boolean isInvertMethodType() {
		return invertMethodType;
	}

	public void setInvertMethodType(boolean invertMethodType) {
		this.invertMethodType = invertMethodType;
	}

	@Override
	public boolean isNoExtractedMethods() {
		return noExtractedMethods;
	}

	@Override
	public void setNoExtractedMethods(boolean noExtractedMethods) {
		this.noExtractedMethods = noExtractedMethods;
	}

	public boolean isOtherMethodType() {
		return otherMethodType;
	}

	public void setOtherMethodType(final boolean otherMethodType) {
		this.otherMethodType = otherMethodType;
	}

// ------------------------ CANONICAL METHODS ------------------------

	public boolean equals(Object value) {
		MethodAttributes other;

		return value instanceof MethodAttributes &&
				super.equals(value) &&
				abstractAttr.equals((other = (MethodAttributes) value).abstractAttr) &&
				overriddenAttr.equals(other.overriddenAttr) &&
				overridingAttr.equals(other.overridingAttr) &&
				implementedAttr.equals(other.implementedAttr) &&
				implementingAttr.equals(other.implementingAttr) &&
				staticInitAttr.equals(other.staticInitAttr) &&
				nativeAttr.equals(other.nativeAttr) &&
				syncAttr.equals(other.syncAttr) &&
				returnTypeAttr.equals(other.returnTypeAttr) &&
				minParamsAttr.equals(other.minParamsAttr) &&
				maxParamsAttr.equals(other.maxParamsAttr) &&
				isConstructorMethodType() == other.isConstructorMethodType() &&
				isGetterSetterMethodType() == other.isGetterSetterMethodType() &&
				isCanonicalMethodType() == other.isCanonicalMethodType() &&
				isOtherMethodType() == other.isOtherMethodType() &&
				invertMethodType == other.invertMethodType &&
				noExtractedMethods == other.noExtractedMethods &&
				getterSetterDefinition.equals(other.getterSetterDefinition);
	}

	public final String toString() {
		// convert settings to readable English description of the method.
		//
		final StringBuffer sb = new StringBuffer(80);

		sb.append(getPlAttr().getProtectionLevelString());
		sb.append(getStAttr().getDescriptiveString());
		sb.append(getfAttr().getDescriptiveString());
		sb.append(staticInitAttr.getDescriptiveString());
		sb.append(nativeAttr.getDescriptiveString());
		sb.append(syncAttr.getDescriptiveString());

		final int nTypes = (isConstructorMethodType() ? 1 : 0) +
				(isGetterSetterMethodType() ? 1 : 0) +
				(isCanonicalMethodType() ? 1 : 0) +
				(isOtherMethodType() ? 1 : 0);

		int nTypesSeen = 0;
		if (isConstructorMethodType()) {
			sb.append(isInvertMethodType() ? "non-constructor" : "constructor");
			nTypesSeen++;
			if (nTypesSeen < nTypes) {
				sb.append("/");
			}
		}
		if (isGetterSetterMethodType()) {
			sb.append(isInvertMethodType() ? "non-getter/setter" : "getter/setter");
			nTypesSeen++;
			if (nTypesSeen < nTypes) {
				sb.append("/");
			}
		}
		if (isCanonicalMethodType()) {
			sb.append(isInvertMethodType() ? "non-canonical" : "canonical");
			nTypesSeen++;
			if (nTypesSeen < nTypes) {
				sb.append("/");
			}
		}
		if (isOtherMethodType()) {
			sb.append(isInvertMethodType() ? "non-other-type" : "other");
			nTypesSeen++;
			if (nTypesSeen < nTypes) {
				sb.append("/");
			}
		}
		if (nTypes > 0) {
			sb.append(' ');
		}
		sb.append(abstractAttr.getDescriptiveString());
		sb.append(overriddenAttr.getDescriptiveString());
		sb.append(overridingAttr.getDescriptiveString());
		sb.append(implementedAttr.getDescriptiveString());
		sb.append(implementingAttr.getDescriptiveString());

		if (sb.length() == 0) {
			sb.append("all methods");
		} else {
			sb.append("methods");
		}
		predicateAdded = false;
		nextPredicate = null;
		if (minParamsAttr.isMatch() || maxParamsAttr.isMatch()) {
			if (!maxParamsAttr.isMatch()) {
				nextPredicate = minParamsAttr.getDescriptiveString();
			} else if (!minParamsAttr.isMatch()) {
				nextPredicate = maxParamsAttr.getDescriptiveString();
			} else {
				if (minParamsAttr.getValue() == maxParamsAttr.getValue()) {
					nextPredicate = "with " + minParamsAttr.getValue() +
							(minParamsAttr.getValue() == 1
									? " parameter"
									: " parameters");
				} else {
					nextPredicate = "with " + minParamsAttr.getValue() + " to " +
							maxParamsAttr.getValue() + " parameters";
				}
			}
		}
		if (getNameAttr().isMatch()) {
			checkPredicate(sb, false);
			nextPredicate = getNativeAttr().getDescriptiveString();
		}
		if (returnTypeAttr.isMatch()) {
			checkPredicate(sb, false);
			nextPredicate = returnTypeAttr.getDescriptiveString();
		}
		checkPredicate(sb, true);
		sb.append(getSortAttr().getDescriptiveString());
		if (noExtractedMethods) {
			sb.append(" (no extracted methods)");
		}
		return sb.toString();
	}

	private void checkPredicate(StringBuffer sb, boolean finalPredicate) {
		if (predicateAdded && nextPredicate != null) {
			sb.append(',');
			if (finalPredicate) {
				sb.append(" and");
			}
		}
		if (nextPredicate != null) {
			sb.append(' ');
			sb.append(nextPredicate);
			predicateAdded = true;
			nextPredicate = null;
		}
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface AttributeGroup ---------------------

	// Start Methods of Interface AttributeGroup
	@Override
	public final /*ItemAttributes*/AttributeGroup deepCopy() {
		final MethodAttributes result = new MethodAttributes();
		deepCopyCommonItems(result);
		result.abstractAttr = (AbstractAttribute) abstractAttr.deepCopy();
		result.nativeAttr = (NativeAttribute) nativeAttr.deepCopy();
		result.syncAttr = (SynchronizedAttribute) syncAttr.deepCopy();
		result.constructorMethodType = constructorMethodType;
		result.getterSetterMethodType = getterSetterMethodType;
		result.canonicalMethodType = canonicalMethodType;
		result.otherMethodType = otherMethodType;
		result.invertMethodType = invertMethodType;
		result.overriddenAttr = (OverriddenAttribute) overriddenAttr.deepCopy();
		result.overridingAttr = (OverridingAttribute) overridingAttr.deepCopy();
		result.implementedAttr = (ImplementedAttribute) implementedAttr.deepCopy();
		result.implementingAttr = (ImplementingAttribute) implementingAttr.deepCopy();
		result.staticInitAttr = (InitializerAttribute) staticInitAttr.deepCopy();
		result.returnTypeAttr = (ReturnTypeAttribute) returnTypeAttr.deepCopy();
		result.minParamsAttr = (MinParamsAttribute) minParamsAttr.deepCopy();
		result.maxParamsAttr = (MaxParamsAttribute) maxParamsAttr.deepCopy();
		result.noExtractedMethods = noExtractedMethods;
		result.getterSetterDefinition = getterSetterDefinition.deepCopy();
		return result;
	}

	@Override
	public final void writeExternal(final Element parent) {
		Element me = new Element("Method");

		writeExternalCommonAttributes(me);
		abstractAttr.appendAttributes(me);
		nativeAttr.appendAttributes(me);
		syncAttr.appendAttributes(me);
		overriddenAttr.appendAttributes(me);
		overridingAttr.appendAttributes(me);
		implementedAttr.appendAttributes(me);
		implementingAttr.appendAttributes(me);
		staticInitAttr.appendAttributes(me);
		returnTypeAttr.appendAttributes(me);
		minParamsAttr.appendAttributes(me);
		maxParamsAttr.appendAttributes(me);

		Element miscElement = new Element("Misc");

		me.getChildren().add(miscElement);
		miscElement.setAttribute("constructorMethod", Boolean.valueOf(constructorMethodType).toString());
		miscElement.setAttribute("getterSetter", Boolean.valueOf(getterSetterMethodType).toString());
		miscElement.setAttribute("canonicalMethod", Boolean.valueOf(canonicalMethodType).toString());
		miscElement.setAttribute("otherMethod", Boolean.valueOf(otherMethodType).toString());
		miscElement.setAttribute("invertMethod", Boolean.valueOf(invertMethodType).toString());
		miscElement.setAttribute("noExtractedMethods", Boolean.valueOf(noExtractedMethods).toString());
		getterSetterDefinition.appendAttributes(me);
		parent.getChildren().add(me);
	}

// --------------------- Interface IRule ---------------------

// End Methods of Interface AttributeGroup
// Start Methods of Interface IRule

	@Override
	public final boolean isMatch(RangeEntry entry) {
		final boolean result = (entry.getEnd() instanceof PsiMethod ||
				entry.getEnd() instanceof PsiClassInitializer) &&
				super.isMatch(entry) &&
				abstractAttr.isMatch(entry.getModifiers()) &&
				overriddenAttr.isMatch(entry.getModifiers()) &&
				overridingAttr.isMatch(entry.getModifiers()) &&
				implementedAttr.isMatch(entry.getModifiers()) &&
				implementingAttr.isMatch(entry.getModifiers()) &&
				staticInitAttr.isMatch(entry.getModifiers()) &&
				nativeAttr.isMatch(entry.getModifiers()) &&
				syncAttr.isMatch(entry.getModifiers()) &&
				returnTypeAttr.isMatch(entry.getType()) &&
				minParamsAttr.isMatch(entry.getEnd()) &&
				maxParamsAttr.isMatch(entry.getEnd());
		if (result == false) {
			return false;
		}
		boolean typeResult = false;
		if (isConstructorMethodType()) {
			typeResult |= (entry.getModifiers() & ModifierConstants.CONSTRUCTOR) == ModifierConstants.CONSTRUCTOR;
		}
		if (isGetterSetterMethodType() &&
				entry.getEnd() instanceof PsiMethod) {
			/**
			 * determine if the method is a getter or setter according to this rule's specific
			 * definition.
			 */
			boolean isGetter = MethodUtil.isGetter(
					(PsiMethod) entry.getEnd(),
					getterSetterDefinition
			);
			boolean isSetter = MethodUtil.isSetter(
					(PsiMethod) entry.getEnd(),
					getterSetterDefinition
			);
			typeResult |= isGetter | isSetter;
		}
		if (isCanonicalMethodType()) {
			typeResult |= (entry.getModifiers() & ModifierConstants.CANONICAL) == ModifierConstants.CANONICAL;
		}
		if (isOtherMethodType()) {
			typeResult |= (entry.getModifiers() & ModifierConstants.OTHER_METHOD) == ModifierConstants.OTHER_METHOD;
		}
		typeResult ^= invertMethodType;
		if (!constructorMethodType &&
				!getterSetterMethodType &&
				!canonicalMethodType &&
				!otherMethodType) {
			typeResult = true; // true if no method type options are selected.
		}
		return result && typeResult;
	}

// -------------------------- OTHER METHODS --------------------------

	// End Methods of Interface IRule
	public JPanel getMethodAttributes() {
		JPanel methodPanel = new JPanel(new GridBagLayout());
		Border border = BorderFactory.createEtchedBorder();

		methodPanel.setBorder(border);

		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.weightx = 0.0d;
		constraints.weighty = 0.0d;
		constraints.gridx = constraints.gridy = 0;
		methodPanel.add(getPlAttr().getProtectionLevelPanel(), constraints);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = 1;
		constraints.gridx = 1;
		constraints.weightx = 1;
		methodPanel.add(getMethodTypePanel(), constraints);
		constraints.gridy = 1;
		constraints.insets = new Insets(5, 0, 0, 0);
		constraints.gridx = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		methodPanel.add(getStAttr().getAndNotPanel(), constraints);
		constraints.gridx++;
		methodPanel.add(getAbstractAttr().getAndNotPanel(), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		methodPanel.add(getfAttr().getAndNotPanel(), constraints);
		constraints.gridx++;
		methodPanel.add(getSyncAttr().getAndNotPanel(), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		methodPanel.add(getOverriddenAttr().getAndNotPanel(), constraints);
		constraints.gridx++;
		methodPanel.add(getOverridingAttr().getAndNotPanel(), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		methodPanel.add(getImplementedAttr().getAndNotPanel(), constraints);
		constraints.gridx++;
		methodPanel.add(getImplementingAttr().getAndNotPanel(), constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		methodPanel.add(getStaticInitAttr().getAndNotPanel(), constraints);
		constraints.gridx++;
		methodPanel.add(getNativeAttr().getAndNotPanel(), constraints);
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridx = 0;
		constraints.gridy++;
		methodPanel.add(getMinParamsAttr().getIntegerPanel(), constraints);
		constraints.gridy++;
		methodPanel.add(getMaxParamsAttr().getIntegerPanel(), constraints);
		constraints.gridy++;
		methodPanel.add(getNameAttr().getStringPanel(), constraints);
		constraints.gridy++;
		methodPanel.add(getReturnTypeAttr().getStringPanel(), constraints);
		constraints.gridy++;
		constraints.insets = new Insets(0, 0, 0, 0);
		methodPanel.add(getExcludePanel(), constraints);
		constraints.gridy++;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.weighty = 1.0d;
		methodPanel.add(getSortAttr().getSortOptionsPanel(), constraints);
		return methodPanel;
	}

	private JPanel getMethodTypePanel() {
		JPanel mtPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridwidth = 1;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0.0d;
		constraints.weighty = 1.0d;
		constraints.gridx = constraints.gridy = 0;

		JCheckBox notBox = new JCheckBox("not");

		notBox.setSelected(isInvertMethodType());
		notBox.setForeground(notBox.isSelected() ? Color.BLACK : Color.GRAY);
		mtPanel.add(notBox, constraints);
		constraints.gridx++;
		constraints.weightx = 1.0d;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		mtPanel.add(getMethodTypeInnerPanel(), constraints);
		notBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				setInvertMethodType(notBox.isSelected());
				notBox.setForeground(notBox.isSelected() ? Color.BLACK : Color.GRAY);
			}

		});
		return mtPanel;
	}

	private JPanel getMethodTypeInnerPanel() {
		JPanel mtPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.NONE;
		constraints.weightx = 1.0d;
		constraints.weighty = 0.0d;
		constraints.gridx = constraints.gridy = 0;

		JCheckBox constructorBox = new JCheckBox("constructor or");

		constructorBox.setSelected(isConstructorMethodType());

		JCheckBox getterSetterBox = new JCheckBox();
		JButton gsDefButton = new JButton("getter/setter");
		JLabel gsOrLabel = new JLabel(" or");

		getterSetterBox.setSelected(isGetterSetterMethodType());

		JCheckBox canonicalBox = new JCheckBox("canonical or");

		canonicalBox.setSelected(isCanonicalMethodType());

		JCheckBox otherTypeBox = new JCheckBox("other type");

		otherTypeBox.setSelected(isOtherMethodType());
		mtPanel.add(constructorBox, constraints);
		constraints.gridy++;
		constraints.weightx = 0;
		constraints.gridwidth = 1;
		mtPanel.add(getterSetterBox, constraints);
		constraints.gridx++;
		mtPanel.add(gsDefButton, constraints);
		constraints.gridx++;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		mtPanel.add(gsOrLabel, constraints);
		constraints.gridx = 0;
		constraints.gridy++;
		mtPanel.add(canonicalBox, constraints);
		constraints.gridy++;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.weighty = 1.0d;
		mtPanel.add(otherTypeBox, constraints);
		constructorBox.addActionListener(event -> {
			setConstructorMethodType(constructorBox.isSelected());
		});
		getterSetterBox.addActionListener(event -> {
			setGetterSetterMethodType(getterSetterBox.isSelected());
		});
		canonicalBox.addActionListener(event -> {
			setCanonicalMethodType(canonicalBox.isSelected());
		});
		otherTypeBox.addActionListener(event -> {
			setOtherMethodType(otherTypeBox.isSelected());
		});
		gsDefButton.addActionListener(event -> {
			GetterSetterDefinition tempgsd = getterSetterDefinition.deepCopy();
			JPanel gsDefPanel = tempgsd.getGSDefinitionPanel();
			JOptionPane op = new JOptionPane(
					gsDefPanel,
					JOptionPane.PLAIN_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION,
					null, null, null
			);
			JDialog jd = op.createDialog(null, "Getter/Setter definition");

			jd.setVisible(true);

			Object result = op.getValue();

			if (result != null &&
					(Integer) result == JOptionPane.OK_OPTION) {
				getterSetterDefinition = tempgsd;
			}
		});
		return mtPanel;
	}

}

