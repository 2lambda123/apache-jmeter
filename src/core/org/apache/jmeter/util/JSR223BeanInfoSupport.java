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

package org.apache.jmeter.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.jmeter.testbeans.TestBean;

/**
 * Parent class to handle common GUI design for JSR223 test elements
 */
public abstract class JSR223BeanInfoSupport extends ScriptingBeanInfoSupport {

    private static final String[] LANGUAGE_TAGS;

    public static final String[][] LANGUAGE_NAMES;

    static {
        Map<String, ScriptEngineFactory> nameMap = new HashMap<>();
        ScriptEngineManager sem = new ScriptEngineManager();
        final List<ScriptEngineFactory> engineFactories = sem.getEngineFactories();
        for(ScriptEngineFactory fact : engineFactories){
            List<String> names = fact.getNames();
            for(String shortName : names) {
                nameMap.put(shortName.toLowerCase(Locale.ENGLISH), fact);
            }
        }
        LANGUAGE_TAGS = nameMap.keySet().toArray(new String[nameMap.size()]);
        Arrays.sort(LANGUAGE_TAGS);
        LANGUAGE_NAMES = new String[nameMap.size()][2];
        int i = 0;
        for(Entry<String, ScriptEngineFactory> me : nameMap.entrySet()) {
            final String key = me.getKey();
            LANGUAGE_NAMES[i][0] = key;
            final ScriptEngineFactory fact = me.getValue();
            LANGUAGE_NAMES[i++][1] = key + 
                    "     (" // $NON-NLS-1$
                    + fact.getLanguageName() + " " + fact.getLanguageVersion()  // $NON-NLS-1$
                    + " / "  // $NON-NLS-1$
                    + fact.getEngineName() + " " + fact.getEngineVersion() // $NON-NLS-1$
                    + ")";   // $NON-NLS-1$
        }
    }

    protected JSR223BeanInfoSupport(Class<? extends TestBean> beanClass) {
        super(beanClass, LANGUAGE_TAGS);
    }

}
