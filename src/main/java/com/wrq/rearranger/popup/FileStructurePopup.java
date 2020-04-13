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
package com.wrq.rearranger.popup;

import com.intellij.psi.PsiFile;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.util.Constraints;
import com.wrq.rearranger.util.IconUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import org.apache.log4j.BasicConfigurator;

/**
 * Contains code to pop up a file structure dialog, showing proposed rearrangement.
 */
public class FileStructurePopup
		implements IHasScrollPane {

// ------------------------------ FIELDS ------------------------------

	final RearrangerSettings settings;

	final PopupTreeComponent treeComponent;

// --------------------------- CONSTRUCTORS ---------------------------

	public FileStructurePopup(
			RearrangerSettings settings,
			List<IRuleInstance> resultRuleInstances,
			final IFilePopupEntry psiFileEntry) {
		this.settings = settings;
		treeComponent = new PopupTreeComponent(settings, resultRuleInstances, psiFileEntry);
	}

	public FileStructurePopup(
			RearrangerSettings settings,
			List<IRuleInstance> resultRuleInstances,
			final PsiFile psiFile) {
		this.settings = settings;
		IFilePopupEntry psiFileEntry = new IFilePopupEntry() {

			@Override
			public String getTypeIconName() {
				return "ppFile";
			}

			@Override
			public String[] getAdditionalIconNames() {
				return null;
			}

			@Override
			public JLabel getPopupEntryText(RearrangerSettings settings) {
				return new JLabel(psiFile.getName());
			}

		};
		treeComponent = new PopupTreeComponent(settings, resultRuleInstances, psiFileEntry);
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	// Level 2 methods
	private JPanel getContainerPanel() {
		final JPanel containerPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints scrollPaneConstraints = new GridBagConstraints();
		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);

		final JScrollPane treeView = getScrollPane();
		final JComponent showTypesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowParameterTypes();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowParameterTypes(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowParamTypes");
					}

					@Override
					String getToolTipText() {
						return "Show parameter types";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_T;
					}
				}.getIconBox();
		final JComponent showNamesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowParameterNames();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowParameterNames(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowParamNames");
					}

					@Override
					String getToolTipText() {
						return "Show parameter names";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_N;
					}
				}.getIconBox();

		final JComponent showFieldsBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowFields();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowFields(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowFields");
					}

					@Override
					String getToolTipText() {
						return "Show fields";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_F;
					}
				}.getIconBox();
		final JComponent showTypeAfterMethodBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowTypeAfterMethod();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowTypeAfterMethod(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowTypeAfterMethod");
					}

					@Override
					String getToolTipText() {
						return "Show type after method";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_A;
					}
				}.getIconBox();
		final JComponent showRulesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowRules();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowRules(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowRules");
					}

					@Override
					String getToolTipText() {
						return "Show rules";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_R;
					}
				}.getIconBox();
		final JComponent showMatchedRulesBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowMatchedRules();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowMatchedRules(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowMatchedRules");
					}

					@Override
					String getToolTipText() {
						return "Show matched rules only";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_M;
					}
				}.getIconBox();
		final JComponent showCommentsBox =
				new IconBox(containerPanel, scrollPaneConstraints, this) {
					@Override
					boolean getSetting() {
						return settings.isShowComments();
					}

					@Override
					void setSetting(boolean value) {
						settings.setShowComments(value);
					}

					@Override
					Icon getIcon() {
						return IconUtil.getIcon("ShowComments");
					}

					@Override
					String getToolTipText() {
						return "Show generated separator comments";
					}

					@Override
					int getShortcut() {
						return KeyEvent.VK_C;
					}
				}.getIconBox();
		((JCheckBox) showRulesBox.getComponent(0)).addActionListener(
				new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						showMatchedRulesBox.setEnabled(((JCheckBox) showRulesBox.getComponent(0)).isSelected());
					}

				}
		);
		showMatchedRulesBox.setEnabled(((JCheckBox) showRulesBox.getComponent(0)).isSelected());
		scrollPaneConstraints.insets = new Insets(3, 3, 3, 3);
		scrollPaneConstraints.fill = GridBagConstraints.BOTH;
		scrollPaneConstraints.gridwidth = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.gridheight = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.weightx = 1;
		scrollPaneConstraints.weighty = 1;
		scrollPaneConstraints.gridx = 0;
		scrollPaneConstraints.gridy = 1;

		constraints.insets = new Insets(5, 5, 5, 5);

		containerPanel.add(showTypesBox, constraints.firstCol());
		containerPanel.add(showNamesBox, constraints.nextCol());
		containerPanel.add(showFieldsBox, constraints.nextCol());
		containerPanel.add(showTypeAfterMethodBox, constraints.nextCol());
		containerPanel.add(showCommentsBox, constraints.nextCol());
		containerPanel.add(showRulesBox, constraints.nextCol());
		containerPanel.add(showMatchedRulesBox, constraints.lastCol());
		containerPanel.add(treeView, scrollPaneConstraints);
		return containerPanel;
	}

	/**
	 * build a JTree containing classes, fields and methods, in accordance with settings.
	 *
	 * @return
	 */
	@Override
	public JScrollPane getScrollPane() {
		// Create the nodes.
		JTree tree = treeComponent.createTree();
		/** expand all nodes. */
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		JScrollPane treeView = new JScrollPane(tree);
		treeView.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		treeView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Dimension d = treeView.getPreferredSize();
		if (d.width < 400) {
			d.width = 400;
		}
		if (d.height < 300) {
			d.height = 300;
		}
		treeView.setPreferredSize(d);
		return treeView;
	}

// -------------------------- OTHER METHODS --------------------------

	/**
	 * Display a pane showing the rearrangement.
	 *
	 * @return true if rearrangement should take place
	 */
	public boolean displayRearrangement() {
		JPanel containerPanel = getContainerPanel();
		JOptionPane pane = new JOptionPane(
				containerPanel,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION
		);
		JDialog dialog = pane.createDialog(null, "Rearrangement Results");
		dialog.setResizable(true);
		dialog.setVisible(true);
		Object selectedValue = pane.getValue();
		return (Integer) selectedValue == JOptionPane.OK_OPTION;
	}

// --------------------------- main() method ---------------------------

	public static void main(String[] args) {
		final RearrangerSettings settings = new RearrangerSettings();
		IFilePopupEntry pf = new IFilePopupEntry() {

			@Override
			public String getTypeIconName() {
				return "nodes/ppFile";
			}

			@Override
			public String[] getAdditionalIconNames() {
				return null;
			}

			@Override
			public JLabel getPopupEntryText(RearrangerSettings settings) {
				return new JLabel("FileName.java");
			}

		};
		FileStructurePopup fsp = new FileStructurePopup(settings, new ArrayList<IRuleInstance>(), pf);
		BasicConfigurator.configure();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		final JFrame frame = new JFrame("SwingApplication");
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0d;
		constraints.weighty = 1.0d;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.gridx = 0;
		constraints.gridy = 0;
		final JPanel object = fsp.getContainerPanel();
		frame.getContentPane().setLayout(new GridBagLayout());
		frame.getContentPane().add(object, constraints);

		//Finish setting up the frame, and show it.
		frame.addWindowListener(
				new WindowAdapter() {

					@Override
					public void windowClosing(final WindowEvent e) {
						System.exit(0);
					}

				}
		);
		frame.pack();
		frame.setVisible(true);
	}

}
