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
package org.apache.jmeter.protocol.http.gui.action;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FileUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.KeystoreConfig;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.ReplaceableController;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.AbstractAction;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.plugin.MenuCreator;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.EscapeDialog;
import org.apache.jmeter.gui.util.FilePanel;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.DNSCacheManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.curl.BasicCurlParser;
import org.apache.jmeter.protocol.http.curl.BasicCurlParser.Request;
import org.apache.jmeter.protocol.http.gui.AuthPanel;
import org.apache.jmeter.protocol.http.gui.CookiePanel;
import org.apache.jmeter.protocol.http.gui.DNSCachePanel;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerFactory;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPFileArg;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.gui.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens a popup where user can enter a cURL command line and create a test plan
 * from it
 * 
 * @since 5.1
 */
public class ParseCurlCommandAction extends AbstractAction implements MenuCreator, ActionListener { // NOSONAR
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseCurlCommandAction.class);
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final Set<String> commands = new HashSet<>();
    public static final String IMPORT_CURL = "import_curl";
    private static final String CREATE_REQUEST = "CREATE_REQUEST";
    private static final String TYPE_FORM = ";type=";
    /** A panel allowing results to be saved. */
    private FilePanel filePanel = null;
    static {
        commands.add(IMPORT_CURL);
    }
    private JSyntaxTextArea cURLCommandTA;
    private JLabel statusText;
    private static Pattern cookiePattern = Pattern.compile("(.+)=(.+)(;?)");

    public ParseCurlCommandAction() {
        super();
    }

    @Override
    public void doAction(ActionEvent e) {
        showInputDialog(e);
    }

    /**
     * Show popup where user can import cURL command
     * 
     * @param event {@link ActionEvent}
     */
    private final void showInputDialog(ActionEvent event) {
        EscapeDialog messageDialog = new EscapeDialog(getParentFrame(event), JMeterUtils.getResString("curl_import"), //$NON-NLS-1$
                false);
        Container contentPane = messageDialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        statusText = new JLabel("", JLabel.CENTER);
        statusText.setForeground(Color.RED);
        contentPane.add(statusText, BorderLayout.NORTH);
        cURLCommandTA = JSyntaxTextArea.getInstance(10, 80, false);
        cURLCommandTA.setCaretPosition(0);
        contentPane.add(JTextScrollPane.getInstance(cURLCommandTA), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        filePanel = new FilePanel("Read from file"); // $NON-NLS-1$
        buttonPanel.add(filePanel);
        JButton button = new JButton(JMeterUtils.getResString("curl_create_request"));
        button.setActionCommand(CREATE_REQUEST);
        button.addActionListener(this);
        buttonPanel.add(button);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        messageDialog.pack();
        ComponentUtil.centerComponentInComponent(GuiPackage.getInstance().getMainFrame(), messageDialog);
        SwingUtilities.invokeLater(() -> messageDialog.setVisible(true));
    }

    /**
     * Finds the first enabled node of a given type in the tree.
     *
     * @param type class of the node to be found
     * @return the first node of the given type in the test component tree, or
     *         <code>null</code> if none was found.
     */
    private JMeterTreeNode findFirstNodeOfType(Class<?> type) {
        JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();
        return treeModel.getNodesOfType(type).stream().filter(JMeterTreeNode::isEnabled).findFirst().orElse(null);
    }

    private void createTestPlan(ActionEvent e, Request request, String statusText)
            throws MalformedURLException, IllegalUserActionException {
        ActionRouter.getInstance().doActionNow(new ActionEvent(e.getSource(), e.getID(), ActionNames.CLOSE));
        GuiPackage guiPackage = GuiPackage.getInstance();
        guiPackage.clearTestPlan();
        FileServer.getFileServer().setScriptName(null);
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        threadGroup.setProperty(TestElement.NAME, "Thread Group");
        threadGroup.setNumThreads(10);
        threadGroup.setRampUp(10);
        threadGroup.setScheduler(true);
        threadGroup.setDuration(3600);
        threadGroup.setDelay(5);
        LoopController loopCtrl = new LoopController();
        loopCtrl.setLoops(-1);
        loopCtrl.setContinueForever(true);
        threadGroup.setSamplerController(loopCtrl);
        TestPlan testPlan = new TestPlan();
        testPlan.setProperty(TestElement.NAME, "Test Plan");
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        HashTree tree = new HashTree();
        HashTree testPlanHT = tree.add(testPlan);
        HashTree threadGroupHT = testPlanHT.add(threadGroup);
        createHttpRequest(request, threadGroupHT,statusText);
        if (!request.getAuthorization().getUser().isEmpty()) {
            AuthManager authManager = new AuthManager();
            createAuthManager(request, authManager);
            threadGroupHT.add(authManager);
        }
        if (!request.getDnsServers().isEmpty()) {
            DNSCacheManager dnsCacheManager = new DNSCacheManager();
            createDnsCacheManager(request, dnsCacheManager);
            threadGroupHT.add(dnsCacheManager);
        }
        ResultCollector resultCollector = new ResultCollector();
        resultCollector.setProperty(TestElement.NAME, "View Results Tree");
        resultCollector.setProperty(TestElement.GUI_CLASS, ViewResultsFullVisualizer.class.getName());
        tree.add(tree.getArray()[0], resultCollector);
        final HashTree newTree = guiPackage.addSubTree(tree);
        guiPackage.updateCurrentGui();
        guiPackage.getMainFrame().getTree()
                .setSelectionPath(new TreePath(((JMeterTreeNode) newTree.getArray()[0]).getPath()));
        final HashTree subTree = guiPackage.getCurrentSubTree();
        // Send different event wether we are merging a test plan into another test
        // plan,
        // or loading a testplan from scratch
        ActionEvent actionEvent = new ActionEvent(subTree.get(subTree.getArray()[subTree.size() - 1]), e.getID(),
                ActionNames.SUB_TREE_LOADED);
        ActionRouter.getInstance().actionPerformed(actionEvent);
        ActionRouter.getInstance().doActionNow(new ActionEvent(e.getSource(), e.getID(), ActionNames.EXPAND_ALL));
    }

    private HTTPSamplerProxy createHttpRequest(Request request, HashTree parentHT, String commentText) throws MalformedURLException {
        HTTPSamplerProxy httpSampler = createSampler(request,commentText);
        HashTree samplerHT = parentHT.add(httpSampler);
        samplerHT.add(httpSampler.getHeaderManager());
        if (request.getCookie() != null) {
            samplerHT.add(httpSampler.getCookieManager());
        }
        if (request.getCacert().equals("cert")) {
            samplerHT.add(httpSampler.getKeystoreConfig());
        }
        return httpSampler;
    }

    /**
     * @param request    {@link Request}
     * @param statusText
     * @return {@link HTTPSamplerProxy}
     * @throws MalformedURLException
     */
    private HTTPSamplerProxy createSampler(Request request, String commentText) throws MalformedURLException {
        HTTPSamplerProxy httpSampler = (HTTPSamplerProxy) HTTPSamplerFactory
                .newInstance(HTTPSamplerFactory.DEFAULT_CLASSNAME);
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        httpSampler.setProperty(TestElement.NAME, "HTTP Request");
        if (!commentText.isEmpty()) {
            httpSampler.setProperty(TestElement.COMMENTS,commentText); // NOSONAR
        } else {
            httpSampler.setProperty(TestElement.COMMENTS,
                    "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } // NOSONAR
        httpSampler.setProtocol(new URL(request.getUrl()).getProtocol());
        httpSampler.setPath(request.getUrl());
        httpSampler.setUseKeepAlive(request.isKeepAlive());
        httpSampler.setFollowRedirects(true);
        httpSampler.setMethod(request.getMethod());
        if (request.getInterfaceName() != null) {
            httpSampler.setIpSourceType(1);
            httpSampler.setIpSource(request.getInterfaceName());
        }
        configureTimeout(request, httpSampler);
        createProxyServer(request, httpSampler);
        if (!"GET".equals(request.getMethod()) && request.getPostData() != null) {
            Arguments arguments = new Arguments();
            httpSampler.setArguments(arguments);
            httpSampler.addNonEncodedArgument("", request.getPostData(), "");
        }
        if (!request.getFormData().isEmpty() || !request.getFormStringData().isEmpty()) {
            setFormData(request, httpSampler);
            httpSampler.setDoMultipart(true);
        }
        HeaderManager headerManager = createHeaderManager(request);
        httpSampler.addTestElement(headerManager);
        if (request.getCookie() != null) {
            CookieManager cookieManager = createCookieManager(request);
            httpSampler.addTestElement(cookieManager);
        }
        if (request.getCacert().equals("cert")) {
            KeystoreConfig keystoreConfig = createKeystoreConfiguration();
            httpSampler.addTestElement(keystoreConfig);
        }
        return httpSampler;
    }

    private void configureTimeout(Request request, HTTPSamplerProxy httpSampler) {
        double connectTimeout = request.getConnectTimeout();
        double maxTime = request.getMaxTime();
        if (connectTimeout >= 0) {
            httpSampler.setConnectTimeout(String.valueOf((int) request.getConnectTimeout()));
            if (maxTime >= 0) {
                maxTime = maxTime - connectTimeout;
            }
        }
        if (maxTime >= 0) {
            httpSampler.setResponseTimeout(String.valueOf((int) maxTime));
        }
    }

    /**
     * 
     * @param request {@link Request}
     * @return {@link HeaderManager} element
     */
    private HeaderManager createHeaderManager(Request request) {
        HeaderManager headerManager = new HeaderManager();
        headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
        headerManager.setProperty(TestElement.NAME, "HTTP HeaderManager");
        headerManager.setProperty(TestElement.COMMENTS,
                "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Map<String, String> map = request.getHeaders();
        boolean hasAcceptEncoding = false;
        for (Map.Entry<String, String> header : map.entrySet()) {
            String key = header.getKey();
            hasAcceptEncoding = hasAcceptEncoding || key.equalsIgnoreCase(ACCEPT_ENCODING);
            headerManager.getHeaders().addItem(new Header(key, header.getValue()));
        }
        if (!hasAcceptEncoding && request.isCompressed()) {
            headerManager.getHeaders().addItem(new Header(ACCEPT_ENCODING, "gzip, deflate"));
        }
        return headerManager;
    }

    /**
     * Create Cookie Manager
     * 
     * @param request {@link Request}
     * @return{@link CookieManager} element
     */
    private CookieManager createCookieManager(Request request) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setProperty(TestElement.GUI_CLASS, CookiePanel.class.getName());
        cookieManager.setProperty(TestElement.NAME, "HTTP CookieManager");
        cookieManager.setProperty(TestElement.COMMENTS,
                "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        Matcher m = cookiePattern.matcher(request.getCookie());
        if (m.matches()) {
            List<Cookie> cookies = stringToCookie(request.getCookie(), request.getUrl());
            for (Cookie c : cookies) {
                cookieManager.getCookies().addItem(c);
            }
        } else {
            File file = new File(request.getCookie());
            if (file.isFile() && file.exists()) {
                try {
                    cookieManager.addFile(request.getCookie());
                } catch (IOException e) {
                    LOGGER.error("Failed to read from File {}", request.getCookie());
                    throw new IllegalArgumentException("Failed to read from File " + request.getCookie());
                }
            } else {
                LOGGER.error("File {} doesn't exist", request.getCookie());
                throw new IllegalArgumentException("File " + request.getCookie() + " doesn't exist");
            }
        }
        return cookieManager;
    }

    /**
     * Create Keystore Configuration
     * 
     * @param request {@link Request}
     * @return{@link KeystoreConfig} element
     */
    private KeystoreConfig createKeystoreConfiguration() {
        KeystoreConfig keystoreConfig = new KeystoreConfig();
        keystoreConfig.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
        keystoreConfig.setProperty(TestElement.NAME, "Keystore Configuration");
        keystoreConfig.setProperty(TestElement.COMMENTS,
                "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return keystoreConfig;
    }

    /**
     * Create Authorization manager
     * 
     * @param request {@link Request}
     * @return {@link AuthManager} element
     */
    private void createAuthManager(Request request, AuthManager authManager) {
        Authorization auth = request.getAuthorization();
        authManager.setProperty(TestElement.GUI_CLASS, AuthPanel.class.getName());
        authManager.setProperty(TestElement.NAME, "HTTP AuthorizationManager");
        authManager.setProperty(TestElement.COMMENTS,
                "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        authManager.getAuthObjects().addItem(auth);
    }

    /**
     * Whether to update Authorization Manager in http request
     * 
     * @param request     {@link Request}
     * @param authManager {@link AuthManager} element
     * @return whether to update Authorization Manager in http request
     */
    private boolean canUpdateAuthManagerInHttpRequest(Request request, AuthManager authManager) {
        Authorization auth = request.getAuthorization();
        for (int i = 0; i < authManager.getAuthObjects().size(); i++) {
            if (auth.getURL().equals(authManager.getAuthObjectAt(i).getURL())
                    && (!authManager.getAuthObjectAt(i).getUser().equals(auth.getUser())
                            || !authManager.getAuthObjectAt(i).getPass().equals(auth.getPass()))
                    || !authManager.getAuthObjectAt(i).getMechanism().equals(auth.getMechanism())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether to update Authorization Manager in Thread Group
     * 
     * @param request     {@link Request}
     * @param authManager {@link AuthManager} element
     * @return whether to update Authorization Manager in Thread Group
     */
    private boolean canUpdateAuthManagerInThreadGroup(Request request, AuthManager authManager) {
        Authorization auth = request.getAuthorization();
        for (int i = 0; i < authManager.getAuthObjects().size(); i++) {
            if (auth.getURL().equals(authManager.getAuthObjectAt(i).getURL())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create DnsCacheManager
     * 
     * @param request {@link Request}
     * @return{@link DnsCacheManager} element
     */
    private void createDnsCacheManager(Request request, DNSCacheManager dnsCacheManager) {
        List<String> dnsServers = request.getDnsServers();
        dnsCacheManager.setProperty(TestElement.GUI_CLASS, DNSCachePanel.class.getName());
        dnsCacheManager.setProperty(TestElement.NAME, "DNS Cache Manager");
        dnsCacheManager.setProperty(TestElement.COMMENTS,
                "Created from cURL on " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        dnsCacheManager.getServers().clear();
        for (String dnsServer : dnsServers) {
            dnsCacheManager.addServer(dnsServer);
        }
    }

    /**
     * Set parameters in http request
     * 
     * @param request     {@link Request}
     * @param httpSampler
     */
    private void setFormData(Request request, HTTPSamplerProxy httpSampler) {
        if (request.getPostData() != null) {
            throw new IllegalArgumentException("--form and --data cant appear in the same command");
        }
        List<HTTPFileArg> httpFileArgs = new ArrayList<>();
        for (Map.Entry<String, String> entry : request.getFormStringData().entrySet()) {
            String formName = entry.getKey();
            String formValue = entry.getValue();
            httpSampler.addNonEncodedArgument(formName, formValue, "");
        }
        for (Map.Entry<String, String> entry : request.getFormData().entrySet()) {
            String formName = entry.getKey();
            String formValue = entry.getValue();
            String contentType = "";
            boolean isContainsFile = formValue.substring(0, 1).equals("@");
            boolean isContainsContentType = formValue.toLowerCase().contains(TYPE_FORM);
            if (isContainsFile) {
                formValue = formValue.substring(1, formValue.length());
                if (isContainsContentType) {
                    String[] formValueWithType = formValue.split(TYPE_FORM);
                    formValue = formValueWithType[0];
                    contentType = formValueWithType[1];
                } else {
                    contentType = new MimetypesFileTypeMap().getContentType(formValue);
                }
                httpFileArgs.add(new HTTPFileArg(formValue, formName, contentType));
            } else {
                if (isContainsContentType) {
                    String[] formValueWithType = formValue.split(TYPE_FORM);
                    formValue = formValueWithType[0];
                    contentType = formValueWithType[1];
                    httpSampler.addNonEncodedArgument(formName, formValue, "", contentType);
                } else {
                    httpSampler.addNonEncodedArgument(formName, formValue, "");
                }
            }
        }
        if (!httpFileArgs.isEmpty()) {
            httpSampler.setHTTPFiles(httpFileArgs.toArray(new HTTPFileArg[httpFileArgs.size()]));
        }
    }
    /**
     * 
     * @param request     {@link Request}
     * @param httpSampler
     */
    private void createProxyServer(Request request, HTTPSamplerProxy httpSampler) {
        Map<String, String> proxyServer = request.getProxyServer();
        for (Map.Entry<String, String> proxyPara : proxyServer.entrySet()) {
            String key = proxyPara.getKey();
            switch (key) {
            case "servername":
                httpSampler.setProxyHost(proxyPara.getValue());
                break;
            case "port":
                httpSampler.setProxyPortInt(proxyPara.getValue());
                break;
            case "scheme":
                httpSampler.setProxyScheme(proxyPara.getValue());
                break;
            case "username":
                httpSampler.setProxyUser(proxyPara.getValue());
                break;
            case "password":
                httpSampler.setProxyPass(proxyPara.getValue());
                break;
            default:
                break;
            }
        }
    }

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.TOOLS) {
            JMenuItem menuItemIC = new JMenuItem(JMeterUtils.getResString("curl_import_menu"), KeyEvent.VK_UNDEFINED);
            menuItemIC.setName(IMPORT_CURL);
            menuItemIC.setActionCommand(IMPORT_CURL);
            menuItemIC.setAccelerator(null);
            menuItemIC.addActionListener(ActionRouter.getInstance());
            return new JMenuItem[] { menuItemIC };
        }
        return new JMenuItem[0];
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        statusText.setText("");
        statusText.setForeground(Color.GREEN);
        boolean isReadFromFile = false;
        if (e.getActionCommand().equals(CREATE_REQUEST)) {
            List<String> commandsList = null;
            List<BasicCurlParser.Request> requests = new ArrayList<>();
            if (!filePanel.getFilename().trim().isEmpty()) {
                commandsList = readFromFile(filePanel.getFilename().trim());
                isReadFromFile = true;
            } else {
                commandsList = readFromTextPanel(cURLCommandTA.getText().trim());
            }
            try {
                parseCommands(isReadFromFile, commandsList, requests);
                Iterator<Request> it = requests.iterator();
                while (it.hasNext()) {
                    BasicCurlParser.Request request = it.next();
                    String commentText = createCommentText(request);
                    GuiPackage guiPackage = GuiPackage.getInstance();
                    guiPackage.updateCurrentNode();
                    JMeterTreeNode treeNode = findFirstNodeOfType(AbstractThreadGroup.class);
                    if (treeNode == null) {
                        LOGGER.info("No AbstractThreadGroup found, potentially empty plan, creating a new plan");
                        createTestPlan(e, request, commentText);
                    } else {
                        JMeterTreeNode currentNode = guiPackage.getCurrentNode();
                        Object userObject = currentNode.getUserObject();
                        if (userObject instanceof Controller && !(userObject instanceof ReplaceableController)) {
                            LOGGER.info("Newly created element will be placed under current selected node {}",
                                    currentNode.getName());
                            addToTestPlan(currentNode, request, commentText);
                        } else {
                            LOGGER.info("Newly created element will be placed under first AbstractThreadGroup node {}",
                                    treeNode.getName());
                            addToTestPlan(treeNode, request, commentText);
                        }
                    }
                }
                statusText.setText(JMeterUtils.getResString("curl_create_success"));
            } catch (Exception ex) {
                statusText.setText(
                        MessageFormat.format(JMeterUtils.getResString("curl_create_failure"), ex.getMessage()));
                statusText.setForeground(Color.RED);
            }
        }
    }


    private void parseCommands(boolean isReadFromFile, List<String> commandsList,
            List<BasicCurlParser.Request> requests) {
        BasicCurlParser basicCurlParser = new BasicCurlParser();
        for (int i = 0; i < commandsList.size(); i++) {
            try {
                BasicCurlParser.Request q = basicCurlParser.parse(commandsList.get(i));
                requests.add(q);
                LOGGER.info("Parsed CURL command {} into {}", commandsList.get(i), q);
            } catch (IllegalArgumentException ie) {
                if (isReadFromFile) {
                    int line = i + 1;
                    LOGGER.error("Error creating test plan from line {} of file, command:{}, error:{}", line,
                            commandsList.get(i), ie.getMessage(), ie);
                    throw new IllegalArgumentException(
                            "Error creating tast plan from file in line " + line + ", see log file");
                } else {
                    LOGGER.error("Error creating test plan from cURL command:{}, error:{}", commandsList.get(i),
                            ie.getMessage(), ie);
                    throw ie;
                }
            }
        }
    }

    private void addToTestPlan(final JMeterTreeNode currentNode, Request request,String statusText) throws MalformedURLException {
        final HTTPSamplerProxy sampler = createSampler(request,statusText);
        JMeterTreeModel treeModel = GuiPackage.getInstance().getTreeModel();
        JMeterUtils.runSafe(true, () -> {
            try {
                boolean canUpdateAuthManagerInHttpRequest = false;
                if (!request.getAuthorization().getUser().isEmpty()) {
                    JMeterTreeNode jMeterTreeNodeAuth = findFirstNodeOfType(AuthManager.class);
                    if (jMeterTreeNodeAuth == null) {
                        AuthManager authManager = new AuthManager();
                        createAuthManager(request, authManager);
                        treeModel.addComponent(authManager, currentNode);
                    } else {
                        AuthManager authManager = (AuthManager) jMeterTreeNodeAuth.getTestElement();
                        if (canUpdateAuthManagerInThreadGroup(request, authManager)) {
                            createAuthManager(request, authManager);
                        } else {
                            canUpdateAuthManagerInHttpRequest = canUpdateAuthManagerInHttpRequest(request, authManager);
                        }
                    }
                }
                if (!request.getDnsServers().isEmpty()) {
                    JMeterTreeNode jMeterTreeNodeDns = findFirstNodeOfType(DNSCacheManager.class);
                    DNSCacheManager dnsCacheManager = new DNSCacheManager();
                    if (jMeterTreeNodeDns == null) {
                        createDnsCacheManager(request, dnsCacheManager);
                        treeModel.addComponent(dnsCacheManager, currentNode);
                    } else {
                        dnsCacheManager = (DNSCacheManager) jMeterTreeNodeDns.getTestElement();
                        createDnsCacheManager(request, dnsCacheManager);
                    }
                }
                CookieManager cookieManager = sampler.getCookieManager();
                HeaderManager headerManager = sampler.getHeaderManager();
                KeystoreConfig keystoreConfig = sampler.getKeystoreConfig();
                final JMeterTreeNode newNode = treeModel.addComponent(sampler, currentNode);
                treeModel.addComponent(headerManager, newNode);
                if (request.getCookie() != null) {
                    treeModel.addComponent(cookieManager, newNode);
                }
                if (request.getCacert().equals("cert")) {
                    treeModel.addComponent(keystoreConfig, newNode);
                }
                if (canUpdateAuthManagerInHttpRequest) {
                    AuthManager authManager = new AuthManager();
                    createAuthManager(request, authManager);
                    treeModel.addComponent(authManager, newNode);
                }
            } catch (IllegalUserActionException ex) {
                LOGGER.error("Error placing sampler", ex);
                JMeterUtils.reportErrorToUser(ex.getMessage());
            }
        });
    }

    @Override
    public Set<String> getActionNames() {
        return commands;
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
        // NOOP
    }

    /**
     * Convert string to cookie
     * 
     * @param cookieStr
     * @param url
     * @return list of cookies
     */
    public List<Cookie> stringToCookie(String cookieStr, String url) {
        List<Cookie> cookies = new ArrayList<>();
        final StringTokenizer tok = new StringTokenizer(cookieStr, "; ", true);
        while (tok.hasMoreTokens()) {
            String nextCookie = tok.nextToken();
            if (nextCookie.contains("=")) {
                String[] cookieParameters = nextCookie.split("=");
                Cookie newCookie = new Cookie();
                newCookie.setName(cookieParameters[0]);
                newCookie.setValue(cookieParameters[1]);
                URL newUrl;
                try {
                    newUrl = new URL(url);
                    newCookie.setDomain(newUrl.getHost());
                    newCookie.setPath(newUrl.getPath());
                    cookies.add(newCookie);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("unqualified url" + url);
                }
            }
        }
        return cookies;
    }

    public static List<String> readFromFile(String pathname) {
        String encoding = StandardCharsets.UTF_8.name();
        File file = new File(pathname);
        List<String> res = new ArrayList<>();
        if (file.exists() && file.isFile()) {
            try {
                res = FileUtils.readLines(file, encoding);
            } catch (IOException e) {
                LOGGER.error("can't find or open the file {}", pathname);
            }
        }
        return res;
    }

    public static List<String> readFromTextPanel(String commands) {
        String[] cs = commands.split("curl");
        List<String> s = new ArrayList<>();
        for (int i = 1; i < cs.length; i++) {
            s.add("curl " + cs[i].trim());
        }
        return s;
    }
    
    private String createCommentText(Request request) {
        StringBuilder commentText = new StringBuilder();
        if (!request.getOptionsIgnored().isEmpty()) {
            for (String s : request.getOptionsIgnored()) {
                commentText.append(s + " ");
            }
            commentText.append("ignore; ");
        }
        if (!request.getOptionsNoSupport().isEmpty()) {
            for (String s : request.getOptionsNoSupport()) {
                commentText.append(s + " ");
            }
            commentText.append("not support; ");
        }
        if (!request.getCacert().isEmpty()) {
            commentText.append("Configure the SSL file with CA certificates in 'system.properties;");
            commentText.append("Look: https://jmeter.apache.org/usermanual/properties_reference.html#ssl_config");
        }
        return commentText.toString();
    }

    
}
