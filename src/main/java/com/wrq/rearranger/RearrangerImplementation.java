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
package com.wrq.rearranger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMExternalizable;
import com.wrq.rearranger.configuration.RearrangerSettingsPanel;
import com.wrq.rearranger.settings.RearrangerSettings;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.List;

import javax.swing.JComponent;
import org.jdom.Element;

/**
 * The rearranger is an IntelliJ IDEA plugin that rearranges the order of class declarations and class member
 * declarations within a Java file.  The user configures an ordering of fields, methods, and classes. Subsequently,
 * whenever the rearranger plugin is invoked on a Java file, the order of declarations is rearranged according to the
 * configuration.
 */
public class RearrangerImplementation implements JDOMExternalizable, Rearranger {

// ------------------------------ FIELDS ------------------------------

	public static final String COMPONENT_NAME = "Rearranger";

	public static final String VERSION = "5.5";

	private static final Logger logger = Logger.getInstance(RearrangerImplementation.class);

	private RearrangerSettingsPanel preferencesPanel;

	private RearrangerSettings settings = new RearrangerSettings();

	private boolean haveReadExternalSettings;

// --------------------------- CONSTRUCTORS ---------------------------

	public RearrangerImplementation() {
//        logger.setAdditivity(false);
//        logger.addAppender(new ConsoleAppender(new PatternLayout("[%7r] %6p - %30.30c - %m \n")));
//        logger.setLevel(Level.DEBUG);
//        logger.setLevel(Level.INFO);
		logger.debug("constructor called for " + COMPONENT_NAME + " plugin version " + VERSION);
		preferencesPanel = null;
		/**
		 * If the Reformat plugin is present, register for a callback after code reformatting
		 * takes place.  This allows user to optimize imports, reformat code, and rearrange all
		 * with one keystroke.
		 */
		try {
			if (Class.forName("org.intellij.psi.codeStyle.ReformatManager") != null) {
				/**
				 * Reformat plugin is available.  Hook it by calling code in RearrangeUtility.
				 * Do this dynamically using reflection so we don't get class loading problems.
				 */
				logger.debug("found ReformatManager class, attempting to hook.");
				/**
				 * if the Reformat plugin is present, obtain a reference to our RearrangerUtility class, which will register
				 * for callbacks from the Reformat plugin.
				 */
				Class rearrangerUtilityClass = Class.forName("org.intellij.psi.codeStyle.RearrangerUtility");
				final Method hookit = rearrangerUtilityClass.getMethod("hookReformatPlugin");
				hookit.invoke(null, (Object) null);
			}
		} catch (ClassNotFoundException e) {
			/**
			 * no reformat plugin available. Just continue with normal rearranger plugin behavior.
			 */
			logger.debug("did not find class org.intellij.psi.codeStyle.RearrangerUtility");
		} catch (NoSuchMethodException e) {
			logger.info(e.toString(), e);
		} catch (IllegalAccessException e) {
			logger.info(e.toString(), e);
		} catch (InvocationTargetException e) {
			logger.info(e.toString(), e);
		}
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	@Override
	public RearrangerSettings getSettings() {
		return settings;
	}

	public void setSettings(RearrangerSettings settings) {
		this.settings = settings;
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Configurable ---------------------

	@Override
	public String getDisplayName() {
		return COMPONENT_NAME;
	}

	@Override
	public String getHelpTopic() {
		return null;
	}

// --------------------- Interface JDOMExternalizable ---------------------

	@Override
	public void readExternal(final Element element) //throws InvalidDataException
	{
		logger.debug("rearranger.readExternal()");
		haveReadExternalSettings = true;
		final List entries = element.getChildren(COMPONENT_NAME);
		if (entries.size() > 0) {
			logger.debug("element 'Rearranger' seen; configuration loading");
			final Element entry = (Element) entries.get(0);
			logger.debug("element has " + entry.getChildren().size() + " children");
			settings.readExternal(entry);
		}
	}

	@Override
	public void writeExternal(final Element element) //throws WriteExternalException
	{
		logger.debug("rearranger.writeExternal()");
		final Element our_element = new Element(COMPONENT_NAME);
		settings.writeExternal(our_element);
		element.getChildren().clear();
		element.addContent(our_element);
	}

// --------------------- Interface UnnamedConfigurable ---------------------

	@Override
	public JComponent createComponent() {
		logger.debug("createComponent: haveReadExternalSettings=" + haveReadExternalSettings);
		RearrangerSettings rs;
		if (!haveReadExternalSettings) {
			logger.debug("createComponent: reading default settings");
			rs = RearrangerSettings.getDefaultSettings();
			if (rs == null) {
				logger.debug("createComponent: could not find default settings");
			}
			haveReadExternalSettings = true;
		} else {
			rs = settings;
		}
		preferencesPanel = new RearrangerSettingsPanel(rs);
		return preferencesPanel;
	}

	@Override
	public boolean isModified() {
		final boolean result = !preferencesPanel.settings.equals(settings);
		logger.debug("rearranger.isModified(): returning " + result);
		return result;
	}

	@Override
	public void apply() {
		logger.debug("rearranger.apply()");
		settings = preferencesPanel.settings.deepCopy();
	}

	@Override
	public void reset() {
		logger.debug("rearranger.reset()");
	}

	@Override
	public void disposeUIResources() {
		preferencesPanel = null;
	}

}

