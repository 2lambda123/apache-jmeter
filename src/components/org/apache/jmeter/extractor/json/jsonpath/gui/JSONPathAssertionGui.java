/*!
 * AtlantBH Custom Jmeter Components v1.0.0
 * http://www.atlantbh.com/jmeter-components/
 *
 * Copyright 2011, AtlantBH
 *
 * Licensed under the under the Apache License, Version 2.0.
 */
package org.apache.jmeter.extractor.json.jsonpath.gui;

import org.apache.jmeter.assertions.gui.AbstractAssertionGui;
import org.apache.jmeter.extractor.json.jsonpath.JSONPathAssertion;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledTextArea;
import org.apache.jorphan.gui.JLabeledTextField;


import javax.swing.JCheckBox;
import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

/**
 * Java class representing GUI for the JSON Path Assertion component in JMeter
 */
public class JSONPathAssertionGui extends AbstractAssertionGui implements ChangeListener {

    private static final long serialVersionUID = 1L;
    private JLabeledTextField jsonPath = null;
    private JLabeledTextArea jsonValue = null;
    private JCheckBox jsonValidation = null;
    private JCheckBox expectNull = null;
    private JCheckBox invert = null;
    private JCheckBox isRegex;

    public JSONPathAssertionGui() {
        init();
    }

    public void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        VerticalPanel panel = new VerticalPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        jsonPath = new JLabeledTextField(JMeterUtils.getResString("json_assertion_path"));
        jsonValidation = new JCheckBox(JMeterUtils.getResString("json_assertion_validation"));
        isRegex = new JCheckBox(JMeterUtils.getResString("json_assertion_regex"));
        jsonValue = new JLabeledTextArea(JMeterUtils.getResString("json_assertion_expected_value"));
        expectNull = new JCheckBox(JMeterUtils.getResString("json_assertion_null"));
        invert = new JCheckBox(JMeterUtils.getResString("json_assertion_invert"));

        jsonValidation.addChangeListener(this);
        expectNull.addChangeListener(this);

        panel.add(jsonPath);
        panel.add(jsonValidation);
        panel.add(isRegex);
        panel.add(jsonValue);
        panel.add(expectNull);
        panel.add(invert);

        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void clearGui() {
        super.clearGui();
        jsonPath.setText("$.");
        jsonValue.setText("");
        jsonValidation.setSelected(false);
        expectNull.setSelected(false);
        invert.setSelected(false);
        isRegex.setSelected(true);
    }

    @Override
    public TestElement createTestElement() {
        JSONPathAssertion jpAssertion = new JSONPathAssertion();
        modifyTestElement(jpAssertion);
        return jpAssertion;
    }

    @Override
    public String getLabelResource() {
        return "json_assertion_title";
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (element instanceof JSONPathAssertion) {
            JSONPathAssertion jpAssertion = (JSONPathAssertion) element;
            jpAssertion.setJsonPath(jsonPath.getText());
            jpAssertion.setExpectedValue(jsonValue.getText());
            jpAssertion.setJsonValidationBool(jsonValidation.isSelected());
            jpAssertion.setExpectNull(expectNull.isSelected());
            jpAssertion.setInvert(invert.isSelected());
            jpAssertion.setIsRegex(isRegex.isSelected());
        }
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        JSONPathAssertion jpAssertion = (JSONPathAssertion) element;
        jsonPath.setText(jpAssertion.getJsonPath());
        jsonValue.setText(jpAssertion.getExpectedValue());
        jsonValidation.setSelected(jpAssertion.isJsonValidationBool());
        expectNull.setSelected(jpAssertion.isExpectNull());
        invert.setSelected(jpAssertion.isInvert());
        isRegex.setSelected(jpAssertion.isUseRegex());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        jsonValue.setEnabled(jsonValidation.isSelected() && !expectNull.isSelected());
        isRegex.setEnabled(jsonValidation.isSelected() && !expectNull.isSelected());
    }
}