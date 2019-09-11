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

package org.apache.jmeter.protocol.bolt.sampler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.util.ConfigMergabilityIndicator;
import org.apache.jmeter.protocol.bolt.config.ConnectionElement;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestElement;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.Neo4jException;
import org.neo4j.driver.v1.summary.ResultSummary;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BoltSampler extends AbstractBoltTestElement implements Sampler, TestBean, ConfigMergabilityIndicator {

    private static final Set<String> APPLICABLE_CONFIG_CLASSES = new HashSet<>(
            Arrays.asList("org.apache.jmeter.config.gui.SimpleConfigGui"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SampleResult sample(Entry e) {
        SampleResult res = new SampleResult();
        res.setSampleLabel(getName());
        res.setSamplerData(request());
        res.setDataType(SampleResult.TEXT);
        res.setContentType("text/plain"); // $NON-NLS-1$
        res.setDataEncoding(StandardCharsets.UTF_8.name());

        // Assume we will be successful
        res.setSuccessful(true);
        res.setResponseMessageOK();
        res.setResponseCodeOK();

        res.sampleStart();

        try {
            res.setResponseHeaders("Cypher request: " + getCypher());
            res.setResponseData(execute(ConnectionElement.getDriver()), StandardCharsets.UTF_8.name());
        } catch (Neo4jException ex) {
            res.setResponseMessage(ex.toString());
            res.setResponseCode(ex.code());
            res.setResponseData(ex.getMessage().getBytes());
            res.setSuccessful(false);
        } catch (Exception ex) {
            res.setResponseMessage(ex.toString());
            res.setResponseCode("000");
            res.setResponseData(ObjectUtils.defaultIfNull(ex.getMessage(), "NO MESSAGE").getBytes());
            res.setSuccessful(false);
        } finally {
            res.sampleEnd();
        }
        return res;
    }

    /**
     * @see org.apache.jmeter.samplers.AbstractSampler#applies(org.apache.jmeter.config.ConfigTestElement)
     */
    @Override
    public boolean applies(ConfigTestElement configElement) {
        String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
        return APPLICABLE_CONFIG_CLASSES.contains(guiClass);
    }

    private String execute(Driver driver) throws IOException {
        Map<String,Object> params = new HashMap<>();
        String results;
        if (getParams() != null && getParams().length() > 0) {
            params = objectMapper.readValue(getParams(), new TypeReference<HashMap<String, Object>>() {});
        }
        try (Session session = driver.session()) {
            StatementResult statementResult = session.run(getCypher(),params);
            results = response(statementResult);
        }
        return results;
    }

    private String request() {
        StringBuilder request = new StringBuilder();
        request.append("Query: \n")
                .append(getCypher())
                .append("\n")
                .append("Parameters: \n")
                .append(getParams());
        return request.toString();
    }

    private String response(StatementResult result) {
        StringBuilder response = new StringBuilder();
        response.append("\nSummary:");
        ResultSummary summary = result.summary();
        response.append("\nConstraints Added: ")
                .append(summary.counters().constraintsAdded())
                .append("\nConstraints Removed: ")
                .append(summary.counters().constraintsRemoved())
                .append("\nContains Updates: ")
                .append(summary.counters().containsUpdates())
                .append("\nIndexes Added: ")
                .append(summary.counters().indexesAdded())
                .append("\nIndexes Removed: ")
                .append(summary.counters().indexesRemoved())
                .append("\nLabels Added: ")
                .append(summary.counters().labelsAdded())
                .append("\nLabels Removed: ")
                .append(summary.counters().labelsRemoved())
                .append("\nNodes Created: ")
                .append(summary.counters().nodesCreated())
                .append("\nNodes Deleted: ")
                .append(summary.counters().nodesDeleted())
                .append("\nRelationships Created: ")
                .append(summary.counters().relationshipsCreated())
                .append("\nRelationships Deleted: ")
                .append(summary.counters().relationshipsDeleted());
        response.append("\n\nRecords: ");
        if (isRecordQueryResults()) {
            result.stream().forEach(record -> response.append("\n")
                    .append(record)
            );
        }
        else {
            response.append("Skipped");
            result.consume();
        }


        return response.toString();
    }
}
