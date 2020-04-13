package com.wrq.rearranger;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;

/**
 * @author brandon-enochs
 */
public class RearrangerProvider extends ConfigurableProvider {

// -------------------------- OTHER METHODS --------------------------

	@Override
	public Configurable createConfigurable() {
		Application application = ApplicationManager.getApplication();
		Rearranger result = application.getService(Rearranger.class);

		return result;
	}

}
