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
import java.awt.Point;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import javax.swing.JTree;

/**
 * Contains logic to start a drag from the popup file structure tree.
 */
public class TreeDragSource
		implements DragSourceListener,
		DragGestureListener {

// ------------------------------ FIELDS ------------------------------

	DragSource source;

	DragGestureRecognizer recognizer;

	TransferableTreeNode transferable;

	JTree sourceTree;

	TreeDropTarget tdt;

	private Logger logger = Logger.getInstance(getClass());

// --------------------------- CONSTRUCTORS ---------------------------

	public TreeDragSource(JTree sourceTree, int actions, TreeDropTarget tdt) {
		logger.debug("construct TreeDragSource, actions=" + actions);
		this.sourceTree = sourceTree;
		this.tdt = tdt;
		source = DragSource.getDefaultDragSource();
		recognizer = source.createDefaultDragGestureRecognizer(
				sourceTree, actions, this);
	}

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface DragGestureListener ---------------------

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		logger.debug("src dragGestureRecognized, action=" + dge.getDragAction() +
				", n paths in selection=" + sourceTree.getSelectionPaths().length);
		final Point point = dge.getDragOrigin();
//        TreePath path = sourceTree.getSelectionPath();
//        if (path == null ||
//                path.getPathCount() <= 1)
//        {
//            // can't move the root node or an empty selection
//            System.out.println("can't drag root node or empty selection");
//            return;
//        }
		// Make a version of the node that we can use for DnD.
		tdt.setSrcRow(sourceTree.getClosestRowForLocation(point.x, point.y));
		transferable = new TransferableTreeNode(sourceTree.getSelectionPaths());
		dge.startDrag(null, transferable, this);
	}

// --------------------- Interface DragSourceListener ---------------------

	@Override
	public void dragEnter(DragSourceDragEvent dsde) {
		logger.debug("src dragEnter, calling dragOver");
		dragOver(dsde);
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde) {
//        logger.debug("src dragOver; drop action=" + dsde.getDropAction() +
//                ", target actions=" + dsde.getTargetActions());
//        dsde.getDragSourceContext().setCursor(dsde.getDropAction() > 0
//                ? DragSource.DefaultMoveDrop
//                : DragSource.DefaultMoveNoDrop);
	}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde) {
		logger.debug("src dropActionChanged: action=" + dsde.getDropAction());
	}

	@Override
	public void dragExit(DragSourceEvent dse) {
//        logger.debug("src dragExit");
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		logger.debug("src dragDropEnd, success=" + dsde.getDropSuccess());
	}

}
