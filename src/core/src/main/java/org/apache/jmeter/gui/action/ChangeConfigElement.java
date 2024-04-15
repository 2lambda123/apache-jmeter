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

package org.apache.jmeter.gui.action;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.ChangeElement;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;

/**
 * Allows to change Config element implementation
 */
@AutoService(Command.class)
public class ChangeConfigElement extends AbstractAction {
    private static final Logger log = LoggerFactory.getLogger(ChangeConfigElement.class);
    private static final Set<String> commands = new HashSet<>();

    static {
        commands.add(ActionNames.CHANGE_CONFIG_ELEMENT);
    }

    @Override
    public void doAction(ActionEvent e) throws IllegalUserActionException {
        String name = ((Component) e.getSource()).getName();
        GuiPackage guiPackage = GuiPackage.getInstance();
        JMeterTreeNode currentNode = guiPackage.getTreeListener().getCurrentNode();
        if (!(currentNode.getUserObject() instanceof AbstractTestElement)) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            guiPackage.updateCurrentNode();
            AbstractTestElement configElement = (AbstractTestElement) guiPackage.createTestElement(name);
            ChangeElement.configElement(configElement, guiPackage, currentNode);
        } catch (Exception err) {
            Toolkit.getDefaultToolkit().beep();
            log.error("Failed to change config element", err);
        }
    }

    @Override
    public Set<String> getActionNames() {
        return commands;
    }
}