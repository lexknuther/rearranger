package com.wrq.rearranger;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.Configurable;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;

/**
 * @author brandon-enochs
 */
public interface Rearranger extends Configurable, PersistentStateComponent<RearrangerSettingsImplementation> {

}
