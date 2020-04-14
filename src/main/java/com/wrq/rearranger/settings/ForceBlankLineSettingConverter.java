package com.wrq.rearranger.settings;

import com.intellij.util.xmlb.Converter;
import com.intellij.util.xmlb.XmlSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author brandon-enochs
 */
public class ForceBlankLineSettingConverter extends Converter<ForceBlankLineSetting> {

// -------------------------- OTHER METHODS --------------------------

	@Nullable
	@Override
	public ForceBlankLineSetting fromString(@NotNull String value) {
		return null;
	}

	@Nullable
	@Override
	public String toString(@NotNull ForceBlankLineSetting value) {
		return XmlSerializer.serialize(value).toString();
	}

}
