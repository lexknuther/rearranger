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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.util.Constraints;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.log4j.BasicConfigurator;

/**
 * Handles right-click in project view; applies rearrangement to a directory and its files recursively.
 */
public class ProjectTreeActionHandler extends AnAction {

// -------------------------- OTHER METHODS --------------------------

	@Override
	public final void actionPerformed(final AnActionEvent anActionEvent) {
		// we're being called on the Swing event dispatch thread.  Spin off another thread to do the
		// rearranging and let Swing's thread go.
		Thread thread = new Thread(new RearrangeIt(anActionEvent.getDataContext()), "RearrangerThread");

		thread.start();
	}

// -------------------------- INNER CLASSES --------------------------

	private abstract static class VirtualFileVisitor {

// ------------------------------ FIELDS ------------------------------

		volatile boolean cancel;

// -------------------------- OTHER METHODS --------------------------

		void accept(VirtualFile virtualFile) {
			if (virtualFile != null) {
				visitVirtualFile(virtualFile);
			}
			VirtualFile[] children = virtualFile.getChildren();

			if (children != null) {
				for (VirtualFile aVfa : children) {
					if (!cancel) {
						accept(aVfa);
					}
				}
			}
		}

		abstract void visitVirtualFile(VirtualFile virtualFile);

	}

	private static class RearrangeIt implements Runnable {

// ------------------------------ FIELDS ------------------------------

		private DataContext dataContext;

		private Project project;

		private JProgressBar progressBar = new JProgressBar(SwingConstants.HORIZONTAL);

		private JLabel filename = new JLabel();

		private Logger logger = Logger.getInstance(getClass());

// --------------------------- CONSTRUCTORS ---------------------------

		private RearrangeIt(final DataContext dataContext) {
			this.dataContext = dataContext;
			if (dataContext != null) {
				project = dataContext.getData(CommonDataKeys.PROJECT);
			} else {
				project = null;
			}
		}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Runnable ---------------------

		@Override
		public void run() {
			VirtualFile virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
			List<VirtualFile> files = new ArrayList<VirtualFile>();
			IntHolder count = new IntHolder();
			VirtualFileVisitor counter = new VirtualFileVisitor() {

				@Override
				void visitVirtualFile(VirtualFile virtualFile) {
					if (!virtualFile.isDirectory()) {
						count.value++;
						logger.debug("" + count.value + ": file " + virtualFile.getName());
						files.add(virtualFile);
					}
				}

			};

			Application application = ApplicationManager.getApplication();

			application.runReadAction(() -> counter.accept(virtualFile));
			logger.debug("counted " + count.value + " files");

			RearrangerActionHandler rah = new RearrangerActionHandler();
			PsiDocumentManager dm = PsiDocumentManager.getInstance(project);
			PsiManager pm = PsiManager.getInstance(project);
			BooleanHolder cancelled = new BooleanHolder();
			JDialog dialog = getProgressFrame(cancelled, count.value);

			dialog.setVisible(true);
			for (int currentCount = 0; !cancelled.value && currentCount < files.size(); currentCount++) {
				VirtualFile f = files.get(currentCount);
				int k = currentCount;
				Runnable fn = () -> {
					PsiFile psiFile = pm.findFile(f);

					logger.debug("SDT setting filename to " + psiFile.getName());
					filename.setText(psiFile.getName());
				};
				Runnable r = () -> {
					PsiFile psiFile = pm.findFile(f);

					if (psiFile != null &&
							psiFile.getName().endsWith(".java") &&
							psiFile.isWritable()) {
						logger.debug("SDT rearranging file " + psiFile.getName());

						Document document = dm.getDocument(psiFile);

						application.runWriteAction(
								() -> {
									Rearranger rearranger = application.getService(Rearranger.class);
									RearrangerSettings settings = rearranger.getState();

									settings = settings.deepCopy();
									// avoid showing confirmation dialog for each file done
									settings.setAskBeforeRearranging(false);
									rah.runWriteActionRearrangement(project, document, psiFile, settings);
								}
						);
					}
					if (!cancelled.value) {
						logger.debug("SDT setting progress bar value to " + (k + 1));
						progressBar.setValue(k + 1);
						progressBar.repaint();
					}
				};

				try {
					SwingUtilities.invokeAndWait(fn);
					SwingUtilities.invokeAndWait(r);
				} catch (InterruptedException exception) {
					exception.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				} catch (InvocationTargetException exception) {
					exception.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
			dialog.setVisible(false);
			dialog.dispose();
		}

// -------------------------- OTHER METHODS --------------------------

		JDialog getProgressFrame(BooleanHolder cancelled, int max) {
			Object parent = null;

			if (project != null) {
				parent = WindowManager.getInstance().suggestParentWindow(project);
			}
			logger.debug("suggested parent window=" + parent);

			JDialog result = parent == null ?
					new JDialog() :
					parent instanceof JDialog ?
							new JDialog((JDialog) parent) :
							new JDialog((JFrame) parent);
			Container pane = result.getContentPane();
			JLabel rearrangingLabel = new JLabel("Rearranging files, please wait...");
			JPanel progressPanel = new JPanel(new GridBagLayout());
			Constraints constraints = new Constraints(GridBagConstraints.NORTHWEST);

			constraints.newRow();
			constraints.insets = new Insets(15, 15, 0, 0);
			progressPanel.add(rearrangingLabel, constraints.weightedLastCol());
			constraints.newRow();
			progressBar.setMinimum(0);
			progressBar.setMaximum(max);
			progressBar.setPreferredSize(new Dimension(500, 15));
			progressBar.setMinimumSize(new Dimension(500, 15));
			progressBar.setStringPainted(true);
			constraints.insets = new Insets(15, 15, 5, 15);
			progressPanel.add(progressBar, constraints.weightedFirstCol());
			constraints.insets = new Insets(9, 0, 5, 15);

			JButton cancelButton = new JButton("Cancel");

			cancelButton.addActionListener(
					event -> {
						logger.debug("cancel button pressed");
						filename.setText("cancelling...");
						cancelled.value = true;
					}
			);

			progressPanel.add(cancelButton, constraints.weightedLastCol());
			constraints.weightedLastRow();
			constraints.insets = new Insets(0, 15, 15, 0);
			filename.setSize(500, 15);
			filename.setPreferredSize(new Dimension(500, 15));
			progressPanel.add(filename, constraints.weightedLastCol());
			pane.add(progressPanel, BorderLayout.CENTER);
			result.setTitle(RearrangerImplementation.COMPONENT_NAME);
			result.addWindowListener(
					new WindowAdapter() {

						@Override
						public void windowClosing(WindowEvent event) {
							logger.debug("dialog closed, cancel Rearranger");
							filename.setText("cancelling...");
							cancelled.value = true;
						}

					}
			);
			result.pack();
			return result;
		}

	}

	private static class BooleanHolder {

// ------------------------------ FIELDS ------------------------------

		boolean value;

	}

	private static class IntHolder {

// ------------------------------ FIELDS ------------------------------

		public int value;

	}

// --------------------------- main() method ---------------------------

	/**
	 * Test progress bar functionality.  Read 10 "filenames" from console, pausing between each to update the progress
	 * bar dialog.  Handle cancel button and dialog window close button properly.
	 */
	public static void main(String[] arguments) {
		BasicConfigurator.configure();
		BufferedReader reader;
		RearrangeIt ri = new RearrangeIt(null);
		IntHolder cancelled = new IntHolder();

		reader = new BufferedReader(new InputStreamReader(System.in));

		BooleanHolder c = new BooleanHolder();
		JDialog dialog = ri.getProgressFrame(c, 10);
		int count = 0;

		dialog.pack();
		dialog.setVisible(true);
		while (count <= 10 && cancelled.value == 0) {
			String line = null;

			try {
				line = reader.readLine();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
			if (cancelled.value == 0) {
				ri.filename.setText(line);
				count++;
				ri.progressBar.setValue(count);
			}
		}
		dialog.setVisible(false);
		dialog.dispose();
		System.exit(0);
	}

}

