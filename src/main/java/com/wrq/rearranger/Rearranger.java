package com.wrq.rearranger;

import com.intellij.openapi.options.Configurable;
import com.wrq.rearranger.settings.RearrangerSettings;

/**
 * @author brandon-enochs
 */
public interface Rearranger extends Configurable {

// -------------------------- OTHER METHODS --------------------------

	RearrangerSettings getSettings();

}
