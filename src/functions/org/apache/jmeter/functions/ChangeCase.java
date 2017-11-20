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

package org.apache.jmeter.functions;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Change Case Function
 * 
 * Support String manipulations of:
 * <ul>
 * <li>upper case</li>
 * <li>lower case</li>
 * <li>capitalize</li>
 * <li>camel cases</li>
 * <li></li>
 * 
 * 
 * @since 4.0
 *
 */
public class ChangeCase extends AbstractFunction {

    private static final Pattern NOT_ALPHANUMERIC_REGEX = 
            Pattern.compile("[^a-zA-Z]");
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeCase.class);
    private static final List<String> DESC = new LinkedList<>();
    private static final String KEY = "__changeCase";

    private static final int MIN_PARAMETER_COUNT = 1;
    private static final int MAX_PARAMETER_COUNT = 3;

    static {
        DESC.add(JMeterUtils.getResString("change_case_string"));
        DESC.add(JMeterUtils.getResString("change_case_mode"));
        DESC.add(JMeterUtils.getResString("function_name_paropt"));
    }

    private CompoundVariable[] values;

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) throws InvalidVariableException {
        String originalString = values[0].execute();
        String mode = null; // default
        if (values.length > 1) {
            mode = values[1].execute();
        }
        if(StringUtils.isEmpty(mode)){
            mode = ChangeCaseMode.UPPER.getName(); // default
        }
        String targetString = changeCase(originalString, mode);
        addVariableValue(targetString, values, 2);
        return targetString;
    }

    protected String changeCase(String originalString, String mode) {
        String targetString = originalString;
        // mode is case insensitive, allow upper for example
        ChangeCaseMode changeCaseMode = ChangeCaseMode.typeOf(mode.toUpperCase());
        if (changeCaseMode != null) {
            switch (changeCaseMode) {
            case UPPER:
                targetString = StringUtils.upperCase(originalString);
                break;
            case LOWER:
                targetString = StringUtils.lowerCase(originalString);
                break;
            case CAPITALIZE:
                targetString = StringUtils.capitalize(originalString);
                break;
            case CAMEL_CASE:
                targetString = camel(originalString, false);
                break;
            case CAMEL_CASE_FIRST_LOWER:
                targetString = camel(originalString, true);
                break;
            default:
                // default not doing nothing to string
            }
        } else {
            LOGGER.error("Unknown mode {}, returning {} unchanged", mode, targetString);
        }
        return targetString;
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkParameterCount(parameters, MIN_PARAMETER_COUNT, MAX_PARAMETER_COUNT);
        values = parameters.toArray(new CompoundVariable[parameters.size()]);
    }

    @Override
    public String getReferenceKey() {
        return KEY;
    }

    @Override
    public List<String> getArgumentDesc() {
        return DESC;
    }

    private static String camel(String str, boolean isFirstCapitalized) {
        StringBuilder builder = new StringBuilder(str.length());
        String[] tokens = NOT_ALPHANUMERIC_REGEX.split(str);
        for (int i = 0; i < tokens.length; i++) {
            if(i == 0) {
                builder.append(isFirstCapitalized ? decapitalize(tokens[0]):
                    StringUtils.capitalize(tokens[i]));
            } else {
                builder.append(StringUtils.capitalize(tokens[i]));
            }
        }
        return builder.toString();
    }

    /**
     * @param string to decapitalize
     */
    private static String decapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        char[] c = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }
    
    /**
     * ChangeCase Modes
     * 
     * Modes for different cases
     *
     */
    public enum ChangeCaseMode {
        UPPER("UPPER"), LOWER("LOWER"), CAPITALIZE("CAPITALIZE"), CAMEL_CASE("CAMEL_CASE"), CAMEL_CASE_FIRST_LOWER(
                "CAMEL_CASE_FIRST_LOWER");
        private String mode;

        private ChangeCaseMode(String mode) {
            this.mode = mode;
        }

        public String getName() {
            return this.mode;
        }

        /**
         * Get ChangeCaseMode by mode
         * 
         * @param mode
         * @return relevant ChangeCaseMode
         */
        public static ChangeCaseMode typeOf(String mode) {
            EnumSet<ChangeCaseMode> allOf = EnumSet.allOf(ChangeCaseMode.class);
            for (ChangeCaseMode zs : allOf) {
                if (zs.getName().equals(mode))
                    return zs;
            }
            return null;
        }
    }
}
