package com.wrq.rearranger.settings;

import com.wrq.rearranger.settings.attributeGroups.AttributeGroup;
import com.wrq.rearranger.settings.attributeGroups.GetterSetterDefinition;
import java.io.File;

import java.util.List;

import org.jdom.Element;

/**
 * @author brandon-enochs
 */
public interface RearrangerSettings {

// -------------------------- OTHER METHODS --------------------------

	List<AttributeGroup> getClassOrderAttributeList();

	void setClassOrderAttributeList(List<AttributeGroup> value);

	List<AttributeGroup> getItemOrderAttributeList();

	void setItemOrderAttributeList(List<AttributeGroup> value);

	void setPrimaryMethodList(List<PrimaryMethodSetting> value);

	ForceBlankLineSetting getAfterClassLBrace();

	void setAfterClassLBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getAfterClassRBrace();

	void setAfterClassRBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getAfterMethodLBrace();

	void setAfterMethodLBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getAfterMethodRBrace();

	void setAfterMethodRBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getBeforeClassRBrace();

	void setBeforeClassRBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getBeforeMethodLBrace();

	void setBeforeMethodLBrace(ForceBlankLineSetting value);

	ForceBlankLineSetting getBeforeMethodRBrace();

	void setBeforeMethodRBrace(ForceBlankLineSetting value);

	GetterSetterDefinition getDefaultGSDefinition();

	void setDefaultGSDefinition(GetterSetterDefinition value);

	RelatedMethodsSettings getExtractedMethodsSettings();

	String getGlobalCommentPattern();

	void setGlobalCommentPattern(String globalCommentPattern);

	ForceBlankLineSetting getNewlinesAtEOF();

	void setNewlinesAtEOF(ForceBlankLineSetting value);

	int getOverloadedOrder();

	void setOverloadedOrder(int overloadedOrder);

	RelatedMethodsSettings getRelatedMethodsSettings();

	void setRelatedMethodsSettings(RelatedMethodsSettings value);

	boolean isAskBeforeRearranging();

	void setAskBeforeRearranging(boolean askBeforeRearranging);

	boolean isKeepGettersSettersTogether();

	void setKeepGettersSettersTogether(boolean keepGettersSettersTogether);

	boolean isKeepGettersSettersWithProperty();

	void setKeepGettersSettersWithProperty(boolean keepGettersSettersWithProperty);

	boolean isKeepOverloadedMethodsTogether();

	void setKeepOverloadedMethodsTogether(boolean keepOverloadedMethodsTogether);

	boolean isRearrangeInnerClasses();

	void setRearrangeInnerClasses(boolean rearrangeInnerClasses);

	boolean isRemoveBlanksInsideCodeBlocks();

	void setRemoveBlanksInsideCodeBlocks(boolean removeBlanksInsideCodeBlocks);

	boolean isShowComments();

	void setShowComments(boolean showComments);

	boolean isShowFields();

	void setShowFields(boolean showFields);

	boolean isShowMatchedRules();

	void setShowMatchedRules(boolean showMatchedRules);

	boolean isShowParameterNames();

	void setShowParameterNames(boolean showParameterNames);

	boolean isShowParameterTypes();

	void setShowParameterTypes(boolean showParameterTypes);

	boolean isShowRules();

	void setShowRules(boolean showRules);

	boolean isShowTypeAfterMethod();

	void setShowTypeAfterMethod(boolean showTypeAfterMethod);

	void readExternal(Element entry);

	void writeExternal(Element entry);

	void writeSettingsToFile(File file);

	RearrangerSettings deepCopy();

}
