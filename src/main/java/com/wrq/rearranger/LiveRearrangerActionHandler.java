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

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.wrq.rearranger.entry.ClassContentsEntry;
import com.wrq.rearranger.popup.ILiveRearranger;
import com.wrq.rearranger.popup.LiveRearrangerDialog;
import com.wrq.rearranger.popup.LiveRearrangerPopup;
import com.wrq.rearranger.rearrangement.Mover;
import com.wrq.rearranger.rearrangement.Parser;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettingsImplementation;
import com.wrq.rearranger.util.CommentUtil;
import java.awt.Window;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supports manual rearrangement of a file.
 */
public class LiveRearrangerActionHandler extends EditorActionHandler {

// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getInstance(LiveRearrangerActionHandler.class);

	private static boolean inProgress;

	private static final boolean useDialog = true; // set to false for Popup

// -------------------------- OTHER METHODS --------------------------

	@Override
	protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
		Project project = dataContext.getData(CommonDataKeys.PROJECT);

		logger.debug("project=" + project);
		logger.debug("editor=" + editor);
		Document document = editor.getDocument();
		int cursorOffset = caret.getOffset();
		PsiFile psiFile = getFile(editor, dataContext);

		if (!psiFile.getName().endsWith(".java")) {
			logger.debug("not a .java file -- skipping " + psiFile.getName());
			return;
		}
		if (!RearrangerActionHandler.isFileWritable(psiFile)) {
			logger.debug("not a writable .java file -- skipping " + psiFile.getName());
			return;
		}
		logger.debug("inProgress=" + inProgress);
		if (!useDialog) {
			if (inProgress) {
				return;
			}
			setInProgress(true);
		}
		buildLiveRearrangerData(project, document, psiFile, cursorOffset);
	}

	private static PsiFile getFile(Editor editor, DataContext context) {
		Project project = context.getData(CommonDataKeys.PROJECT);
		Document document = editor.getDocument();
		FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
		VirtualFile virtualFile = fileDocumentManager.getFile(document);
		PsiManager psiManager = PsiManager.getInstance(project);

		return psiManager.findFile(virtualFile);
	}

	public static void setInProgress(boolean inProgress) {
		logger.debug("set inProgress=" + inProgress);
		LiveRearrangerActionHandler.inProgress = inProgress;
	}

	/**
	 * must be called from within an IDEA read-action thread.
	 *
	 * @param project
	 * @param document
	 * @param psiFile
	 */
	void buildLiveRearrangerData(Project project, Document document, PsiFile psiFile, int cursorOffset) {
		/**
		 * Per instructions from IntelliJ, we have to commit any changes to the document to the Psi
		 * tree.
		 */
		PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);

		documentManager.commitDocument(document);

		RearrangerSettingsImplementation settings = new RearrangerSettingsImplementation(); // use default settings with no rules

		settings.setAskBeforeRearranging(true);
		settings.setRearrangeInnerClasses(true);
		if (useDialog) {
			Application application = ApplicationManager.getApplication();

			application.runWriteAction(
					new Runnable() {

						@Override
						public void run() {
							liveRearrangeDocument(project, psiFile, settings, document, cursorOffset);
						}

					}
			);
		} else {
			Runnable task = new Runnable() {

				@Override
				public void run() {
					logger.debug("liveRearrangeDocument task started");
					liveRearrangeDocument(project, psiFile, settings, document, cursorOffset);
				}

			};
			Thread t = new Thread(
					new Runnable() {

						@Override
						public void run() {
							logger.debug("started thread " + Thread.currentThread().getName());
							final Application application = ApplicationManager.getApplication();
							application.runReadAction(
									new Runnable() {

										@Override
										public void run() {
											logger.debug(
													"enter application.runReadAction() on thread " +
															Thread.currentThread().getName()
											);
											task.run();
											logger.debug(
													"exit application.runReadAction() on thread " +
															Thread.currentThread().getName()
											);
										}

									}
							);
						}

					}, "Live Rearranger parser"
			);
			t.start();
		}
		logger.debug("exit buildLiveRearrangerData on thread " + Thread.currentThread().getName());
	}

	public void liveRearrangeDocument(
			Project project,
			PsiFile psiFile,
			RearrangerSettingsImplementation settings,
			Document document,
			int cursorOffset) {
		logger.debug("enter liveRearrangeDocument on thread " + Thread.currentThread().getName());

		new CommentUtil(settings); // create CommentUtil singleton

		Window window = WindowManager.getInstance().suggestParentWindow(project);
		ILiveRearranger fsp;

		if (useDialog) {
			fsp = new LiveRearrangerDialog(settings, psiFile, document, window, cursorOffset);
		} else {
			fsp = new LiveRearrangerPopup(settings, psiFile, document, project, window, cursorOffset);
		}

		Parser parser = new Parser(project, settings, psiFile);
		List<ClassContentsEntry> outerClasses = parser.parseOuterLevel();

		if (!outerClasses.isEmpty()) {
			Mover mover = new Mover(outerClasses, settings);
			List<IRuleInstance> resultRuleInstances = mover.rearrangeOuterClasses();

			fsp.setResultRuleInstances(resultRuleInstances);
			fsp.liveRearranger();
		}
		logger.debug("exit liveRearrangeDocument on thread " + Thread.currentThread().getName());
	}

}

