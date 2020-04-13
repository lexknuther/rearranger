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
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.wrq.rearranger.entry.ClassContentsEntry;
import com.wrq.rearranger.popup.FileStructurePopup;
import com.wrq.rearranger.rearrangement.Emitter;
import com.wrq.rearranger.rearrangement.Mover;
import com.wrq.rearranger.rearrangement.Parser;
import com.wrq.rearranger.rearrangement.Spacer;
import com.wrq.rearranger.ruleinstance.IRuleInstance;
import com.wrq.rearranger.settings.RearrangerSettings;
import com.wrq.rearranger.util.CommentUtil;
import java.awt.dnd.DragSource;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class performs rearrangement actions requested by the user.
 */
public class RearrangerActionHandler extends EditorActionHandler {

// ------------------------------ FIELDS ------------------------------

	private Logger logger = Logger.getInstance(getClass());

	private static int rightMargin;

	private static int tabSize;

// -------------------------- STATIC METHODS --------------------------

	public static int getRightMargin() {
		return rightMargin;
	}

	public static int getTabSize() {
		return tabSize;
	}

// -------------------------- OTHER METHODS --------------------------

	@Override
	protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
		Project project = (Project) dataContext.getData(DataConstants.PROJECT);
		Document document = editor.getDocument();
		PsiFile psiFile = getFile(editor, dataContext);

		logger.debug("suggested tool window = " + WindowManager.getInstance().suggestParentWindow(project));
		logger.debug("drag source image supported = " + DragSource.isDragImageSupported());
		if (!psiFile.getName().endsWith(".java")) {
			logger.debug("not a .java file -- skipping " + psiFile.getName());
			return;
		}
		if (!psiFile.isWritable()) {
			logger.debug("not a writable .java file -- skipping " + psiFile.getName());
			return;
		}
		rightMargin = editor.getSettings().getRightMargin(project);
		tabSize = editor.getSettings().getTabSize(project);
		logger.debug("right margin=" + rightMargin + ", tabSize=" + tabSize);

		Application application = ApplicationManager.getApplication();

		application.runWriteAction(() -> {
					Rearranger rearranger = application.getService(Rearranger.class);
					RearrangerSettings settings = rearranger.getSettings();

					runWriteActionRearrangement(project, document, psiFile, settings);
				}
		);
	}

	private static PsiFile getFile(Editor editor, DataContext context) {
		Project project = context.getData(CommonDataKeys.PROJECT);
		Document document = editor.getDocument();
		FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
		VirtualFile virtualFile = fileDocumentManager.getFile(document);
		PsiManager psiManager = PsiManager.getInstance(project);

		return psiManager.findFile(virtualFile);
	}

	/**
	 * must be called from within an IDEA write-action thread.
	 */
	void runWriteActionRearrangement(
			final Project project,
			final Document document,
			final PsiFile psiFile,
			final RearrangerSettings settings) {
		/**
		 * Per instructions from IntelliJ, we have to commit any changes to the document to the Psi
		 * tree.
		 */
		final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
		documentManager.commitDocument(document);
		final WaitableBoolean wb = new WaitableBoolean();
		if (psiFile != null &&
				isFileWritable(psiFile) &&
				psiFile.getName().endsWith(".java")) {
			logger.debug("schedule rearranger task");

			Runnable task = new RearrangerTask(project, psiFile, settings, document, wb);

			CommandProcessor.getInstance().executeCommand(project, task, "Rearrange", null);
		}
		try {
			logger.debug("wait for rearranger task to complete.");
			wb.whenTrue();
		} catch (InterruptedException e) {
			e.printStackTrace(); //To change body of catch statement use Options | File Templates.
		}
		logger.debug("end execute");
	}

	static boolean isFileWritable(PsiElement element) {
		VirtualFile file = element.getContainingFile().getVirtualFile();

		return file != null && file.isWritable();
	}

	private void rearrangeDocument(Project project, PsiFile psiFile, RearrangerSettings settings, Document document) {
		new CommentUtil(settings); // create CommentUtil singleton

		Parser parser = new Parser(project, settings, psiFile);
		List<ClassContentsEntry> outerClasses = parser.parseOuterLevel();

		if (!outerClasses.isEmpty()) {
			Mover mover = new Mover(outerClasses, settings);
			List<IRuleInstance> resultRuleInstances = mover.rearrangeOuterClasses();

			if (!settings.isAskBeforeRearranging() ||
					new FileStructurePopup(settings, resultRuleInstances, psiFile).displayRearrangement()) {
				Emitter emitter = new Emitter(psiFile, resultRuleInstances, document);

				emitter.emitRearrangedDocument();
			}
		}
		logger.debug("respacing document");

		PsiDocumentManager.getInstance(project).commitDocument(document);
		Spacer spacer = new Spacer(project, psiFile, document, settings);

		if (spacer.respace()) {
			PsiDocumentManager.getInstance(project).commitDocument(document);
		}
		logger.debug("exit rearrangeDocument");
	}

	public void setTabSize(int tabSize) {
		RearrangerActionHandler.tabSize = tabSize;
	}

// -------------------------- INNER CLASSES --------------------------

	private class RearrangerTask implements Runnable {

// ------------------------------ FIELDS ------------------------------

		private Project project;

		private PsiFile psiFile;

		private RearrangerSettings settings;

		private Document document;

		private WaitableBoolean wb;

// --------------------------- CONSTRUCTORS ---------------------------

		RearrangerTask(
				Project project, PsiFile psiFile, RearrangerSettings settings, Document document, WaitableBoolean wb) {
			this.project = project;
			this.psiFile = psiFile;
			this.settings = settings;
			this.document = document;
			this.wb = wb;
		}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Runnable ---------------------

		@Override
		public final void run() {
			try {
				rearrangeDocument(project, psiFile, settings, document);
			} finally {
				wb.set();
			}
		}

	}

}
