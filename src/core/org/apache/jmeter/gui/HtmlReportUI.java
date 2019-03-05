/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.HtmlReportGenerator;
import org.apache.jmeter.gui.util.EscapeDialog;
import org.apache.jmeter.gui.util.FileDialoger;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.ComponentUtil;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlReportUI implements ActionListener {
    private static Set<String> commands = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlReportUI.class);

    private static final String CREATE_REQUEST = "CREATE_REQUEST";
    private static final String BROWSE_CSV = "BROWSE_CSV";
    private static final String BROWSE_USER_PROPERTIES = "BROWSE_USER_PROPERTIES";
    private static final String BROWSE_OUTPUT = "BROWSE_OUTPUT";

    private EscapeDialog messageDialog;

    private JTextField csvFilePathTextField;
    private JTextField userPropertiesFilePathTextField;
    private JTextField outputDirectoryPathTextField;
    private JButton reportLaunchButton;
    private JSyntaxTextArea reportingArea;
    private JButton csvFileButton;
    private JButton outputDirectoryButton;
    private JButton userPropertiesFileButton;

    static {
        commands.add(ActionNames.HTML_REPORT);
    }

    public HtmlReportUI() {
        super();
    }

    public void showInputDialog(JFrame parent) {
        setupInputDialog(parent);
        launchInputDialog();
    }

    private void launchInputDialog() {
        messageDialog.pack();
        ComponentUtil.centerComponentInWindow(messageDialog);
        messageDialog.setVisible(true);
    }

    public void setupInputDialog(JFrame parent) {
        messageDialog = new EscapeDialog(parent, JMeterUtils.getResString("generate_report_ui.html_report_menu"), false);
        setupContentPane();
    }

    private void setupContentPane() {
        Container contentPane = messageDialog.getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(setupFileChooserPanel(), BorderLayout.NORTH);

        reportingArea = JSyntaxTextArea.getInstance(10, 60, true);
        reportingArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        reportingArea.setEditable(false);
        contentPane.add(reportingArea, BorderLayout.CENTER);

        contentPane.add(setupButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel setupFileChooserPanel() {
        JPanel fileChooserPanel = new JPanel(new GridLayout(3, 3));
        fileChooserPanel.add(new JLabel(JMeterUtils.getResString("generate_report_ui.csv_file")));

        csvFilePathTextField = new JTextField();
        fileChooserPanel.add(csvFilePathTextField);

        this.csvFileButton = new JButton(JMeterUtils.getResString("browse"));
        csvFileButton.setActionCommand(BROWSE_CSV);
        csvFileButton.addActionListener(this);
        fileChooserPanel.add(csvFileButton);

        fileChooserPanel.add(new JLabel(JMeterUtils.getResString("generate_report_ui.user_properties_file")));

        userPropertiesFilePathTextField = new JTextField();
        fileChooserPanel.add(userPropertiesFilePathTextField);

        this.userPropertiesFileButton = new JButton(JMeterUtils.getResString("browse"));
        userPropertiesFileButton.setActionCommand(BROWSE_USER_PROPERTIES);
        userPropertiesFileButton.addActionListener(this);
        fileChooserPanel.add(userPropertiesFileButton);

        fileChooserPanel.add(new JLabel(JMeterUtils.getResString("generate_report_ui.output_directory")));

        outputDirectoryPathTextField = new JTextField();
        fileChooserPanel.add(outputDirectoryPathTextField);

        this.outputDirectoryButton = new JButton(JMeterUtils.getResString("browse"));
        outputDirectoryButton.setActionCommand(BROWSE_OUTPUT);
        outputDirectoryButton.addActionListener(this);
        fileChooserPanel.add(outputDirectoryButton);
        return fileChooserPanel;
    }

    private JPanel setupButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 1));

        reportLaunchButton = new JButton(JMeterUtils.getResString("generate_report_ui.html_report_request"));
        reportLaunchButton.setActionCommand(CREATE_REQUEST);
        reportLaunchButton.addActionListener(this);
        buttonPanel.add(reportLaunchButton);
        return buttonPanel;
    }

    private class ReportGenerationWorker extends SwingWorker<List<String>, String> {
        private JButton reportLaunchButton;

        public ReportGenerationWorker(JButton reportLaunchButton) {
            this.reportLaunchButton = reportLaunchButton;
        }
        @Override
        protected List<String> doInBackground() throws Exception {
            HtmlReportGenerator htmlReportAction = new HtmlReportGenerator(csvFilePathTextField.getText(),
                    userPropertiesFilePathTextField.getText(), outputDirectoryPathTextField.getText());
            SwingUtilities.invokeAndWait(() -> reportLaunchButton.setEnabled(false));
            return htmlReportAction.run();
        }

        @Override
        protected void done() {
            try {
                reportLaunchButton.setEnabled(true);
                reportToUser(get());
            } catch (InterruptedException | ExecutionException exception) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error during html report generation : {}", exception.getMessage(), exception);
                }
                List<String> reportException = new ArrayList<>();
                reportException.add(exception.getMessage());
                reportToUser(reportException);
            }
        }
    }

    private void reportingAddText(String resString) {
        reportingArea.setText(reportingArea.getText() + resString + "\n");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
        case CREATE_REQUEST:
            try {
                reportingArea.setText(JMeterUtils.getResString("generate_report_ui.html_report_processing") + "\n");
                reportLaunchButton.setForeground(Color.orange);
                new ReportGenerationWorker(reportLaunchButton).execute();
            } catch (Exception exception) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error during html report generation : {}", exception.getMessage(), exception);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("CSV file path {} \n user.properties file path : {} \nOutput directory file path : {}",
                        csvFilePathTextField.getText(), userPropertiesFilePathTextField.getText(),
                        outputDirectoryPathTextField.getText());
            }
            break;
        case BROWSE_USER_PROPERTIES:
            userPropertiesFilePathTextField.setText(showFileChooser(userPropertiesFileButton.getParent(),
                    userPropertiesFilePathTextField, false, new String[] { ".properties" }));
            break;
        case BROWSE_CSV:
            csvFilePathTextField.setText(showFileChooser(csvFileButton.getParent(), csvFilePathTextField, false,
                    new String[] { ".jtl", ".csv" }));
            break;
        case BROWSE_OUTPUT:
            outputDirectoryPathTextField.setText(
                    showFileChooser(outputDirectoryButton.getParent(), outputDirectoryPathTextField, true, null));
            break;
        default:
            break;
        }
    }

    void reportToUser(List<String> runErrors) {
        if (runErrors.isEmpty()) {
            reportingAddText(JMeterUtils.getResString(HtmlReportGenerator.HTML_REPORT_SUCCESS));
            reportLaunchButton.setForeground(Color.green);
        } else {
            reportingAddText(String.join("\n", runErrors));
            reportLaunchButton.setForeground(Color.red);
        }
    }

    /**
     * Show a file chooser to the user
     * 
     * @param locationTextField
     *            the textField that will receive the path
     * @param onlyDirectory
     *            whether or not the file chooser will only display directories
     * @param extensions File extensions to filter
     * @return the path the user selected or, if the user cancelled the file
     *         chooser, the previous path
     */
    private String showFileChooser(Component component, JTextField locationTextField, boolean onlyDirectory, String[] extensions) {
        JFileChooser fileChooser = FileDialoger.promptToOpenFile(component, extensions, System.getProperty("user.home"), onlyDirectory);
        if (fileChooser == null) {
            return locationTextField.getText();
        }
        return fileChooser.getSelectedFile().getPath();
    }

    public JTextField getCsvFilePathTextField() {
        return csvFilePathTextField;
    }

    public JTextField getUserPropertiesFilePathTextField() {
        return userPropertiesFilePathTextField;
    }

    public JTextField getOutputDirectoryPathTextField() {
        return outputDirectoryPathTextField;
    }

    public JButton getReportLaunchButton() {
        return reportLaunchButton;
    }

    public JSyntaxTextArea getReportingArea() {
        return reportingArea;
    }

    public EscapeDialog getMessageDialog() {
        return messageDialog;
    }
}
