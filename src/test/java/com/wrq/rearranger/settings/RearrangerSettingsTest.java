package com.wrq.rearranger.settings;

import com.intellij.util.xmlb.XmlSerializer;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author brandon-enochs
 */
public class RearrangerSettingsTest {

// -------------------------- OTHER METHODS --------------------------

	@Test
	public void testSerialize() {
		RearrangerSettings oldStyleSettings = RearrangerSettingsImplementation.getDefaultSettings();

		Assert.assertThat(oldStyleSettings, IsNull.notNullValue());

		RearrangerSettings newStyleSettings = XmlSerializer.deserialize(
				XmlSerializer.serialize(oldStyleSettings), RearrangerSettingsImplementation.class
		);

		Assert.assertThat(oldStyleSettings, Is.is(newStyleSettings));
	}

}
