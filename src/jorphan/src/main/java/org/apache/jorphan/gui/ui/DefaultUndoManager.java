/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jorphan.gui.ui;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;

import org.checkerframework.checker.guieffect.qual.SafeType;

@SafeType
class DefaultUndoManager extends UndoManager {
    private final AtomicInteger undoEpoch;
    private int ourUndoEpoch;

    DefaultUndoManager(AtomicInteger undoEpoch) {
        this.undoEpoch = undoEpoch;
        this.ourUndoEpoch = undoEpoch.get();
    }

    @Override
    public synchronized void discardAllEdits() {
        super.discardAllEdits();
        ourUndoEpoch = undoEpoch.get();
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        int epoch = undoEpoch.get();
        if (ourUndoEpoch != epoch) {
            discardAllEdits();
        }
        super.undoableEditHappened(e);
    }

    @Override
    public synchronized boolean canUndo() {
        return ourUndoEpoch == undoEpoch.get() && super.canUndo();
    }

    @Override
    public synchronized boolean canRedo() {
        return ourUndoEpoch == undoEpoch.get() && super.canRedo();
    }
}
