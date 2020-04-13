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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.wrq.rearranger.LiveRearrangerActionHandler;
import com.wrq.rearranger.rearrangement.Emitter;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.util.Constraints;
import com.wrq.rearranger.util.IconUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * Contains logic to display a rearrangement dialog, allow user to perform drag&drop rearrangement, and rearrange the
 * code accordingly.
 */
public class LiveRearrangerDialog
		implements IHasScrollPane,
		ILiveRearranger {

// ------------------------------ FIELDS ------------------------------

	private Logger logger = Logger.getInstance(getClass());

	final RearrangerSettings settings;

	PopupTreeComponent treeComponent;

	List<IRuleInstance> resultRuleInstances;

	final Window outerPanel;

	final Document document;

	final PsiFile psiFile;

	IFilePopupEntry psiFileEntry;

	TreeDragSource tds;

	TreeDropTarget tdt;

	boolean rearrangementOccurred;

	int cursorOffset;

	private JPanel containerPanel;

	private PopupTree popupTree;

// --------------------------- CONSTRUCTORS ---------------------------

	public LiveRearrangerDialog(
			RearrangerSettings settings, PsiFile psiFile, Document document,
			final Window outerPanel, int cursorOffset) {
		this.settings = settings;
		this.document = document;
		this.psiFile = psiFile;
		createFilePopupEntry(psiFile);
		this.outerPanel = outerPanel;
		this.cursorOffset = cursorOffset;
	}

	private void createFilePopupEntry(final PsiFile psiFile) {
		psiFileEntry = new IFilePopupEntry() {

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
				return new JLabel(psiFile.getName());
			}

		};
	}

// --------------------- GETTER / SETTER METHODS ---------------------

	private JPanel getContainerPanel() {
		final JPanel containerPanel = new JPanel(new GridBagLayout());

		final GridBagConstraints scrollPaneConstraints = new GridBagConstraints();
		scrollPaneConstraints.insets = new Insets(3, 3, 3, 3);
		scrollPaneConstraints.fill = GridBagConstraints.BOTH;
		scrollPaneConstraints.gridwidth = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.gridheight = GridBagConstraints.REMAINDER;
		scrollPaneConstraints.weightx = 1;
		scrollPaneConstraints.weighty = 1;
		scrollPaneConstraints.gridx = 0;
		scrollPaneConstraints.gridy = 1;

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
		Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);
		constraints.insets = new Insets(5, 5, 5, 5);
		containerPanel.add(showTypesBox, constraints.firstCol());
		containerPanel.add(showNamesBox, constraints.nextCol());
		containerPanel.add(showFieldsBox, constraints.nextCol());
		containerPanel.add(showTypeAfterMethodBox, constraints.lastCol());
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
		popupTree = treeComponent.createLiveRearrangerTree();

//        /** only expand node where cursor is located.  Inspect all rows; deepest node that covers
//         * cursor location is the best to expand.  (Parent node like a class contains a method where
//         * the cursor is; we want to expand the method, not just the class.
//         */
//        int expandRow = -1;
//        for (int i = 0; i < tree.getRowCount(); i++)
//        {
//            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(i).getLastPathComponent();
//            if (node.getUserObject() instanceof RangeEntry)
//            {
//                RangeEntry re = (RangeEntry) node.getUserObject();
//                if (re.getStart().getTextRange().getStartOffset() <= cursorOffset &&
//                        re.getEnd().getTextRange().getEndOffset() >= cursorOffset)
//                {
//                    logger.debug(
//                            "node " +
//                            i +
//                            " contained cursor (offset=" +
//                            cursorOffset +
//                            "): " + re
//                    );
//                    expandRow = i;
//                }
//            }
//            else
//            {
//                logger.debug("expand node candidate not RangeEntry; node=" + node);
//            }
//        }
//        if (expandRow >= 0)
//        {
//            logger.debug("expand row " + expandRow);
//            tree.expandRow(expandRow);
//        }
		/** simply expand all nodes. */
		for (int i = 0; i < popupTree.getRowCount(); i++) {
			popupTree.expandRow(i);
		}
		JScrollPane treeView = new JScrollPane(popupTree);
		treeView.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		treeView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeView.getVerticalScrollBar().setFocusable(false);
		treeView.getHorizontalScrollBar().setFocusable(false);
		Dimension d = treeView.getPreferredSize();
		if (d.width < 400) {
			d.width = 400;
		}
		if (d.height < 300) {
			d.height = 300;
		}
		treeView.setPreferredSize(d);
		tdt = new TreeDropTarget(popupTree, this);
		tds = new TreeDragSource(popupTree, DnDConstants.ACTION_MOVE, tdt);
		return treeView;
	}

	@Override
	public void setRearrangementOccurred(boolean rearrangementOccurred) {
		this.rearrangementOccurred = rearrangementOccurred;
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface ILiveRearranger ---------------------

	/**
	 * Display a live rearrangement window.
	 */
	@Override
	public void liveRearranger() {
		containerPanel = getContainerPanel();
		JOptionPane pane = new JOptionPane(
				containerPanel,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION
		);
		JDialog dialog = pane.createDialog(null, "Live Rearranger");
		dialog.setResizable(true);
		dialog.setVisible(true);
		Object selectedValue = pane.getValue();
		rearrangementOccurred =
				treeComponent.isRearrangementOccurred() &&
						(popupTree.isExitedWithEnterKey() ||
								selectedValue != null &&
										(Integer) selectedValue == JOptionPane.OK_OPTION);
		finish();
		logger.debug("exit liveRearranger");
	}

	@Override
	public void setResultRuleInstances(List<IRuleInstance> resultRuleInstances) {
		this.resultRuleInstances = resultRuleInstances;
		treeComponent = new PopupTreeComponent(settings, resultRuleInstances, psiFileEntry);
	}

// -------------------------- OTHER METHODS --------------------------

	public void finish() {
		logger.debug("entered finish() on thread " + Thread.currentThread().getName());
		if (!rearrangementOccurred) {
			logger.debug("no rearrangement occurred, not rearranging document");
			return;
		}
		logger.debug("rearranging document");
		if (document != null) {
			final Emitter e = new Emitter(psiFile, resultRuleInstances, document);
			e.emitRearrangedDocument();
		}

		LiveRearrangerActionHandler.setInProgress(false);
		logger.debug("exit finish() on thread " + Thread.currentThread().getName());
	}

//    class RearrangerTest
//            extends LightCodeInsightTestCase
//    {
//        private RearrangerSettings rs;
//
//        protected final void setUp() throws Exception
//        {
//            super.setUp();
//        }
//
//
//

}