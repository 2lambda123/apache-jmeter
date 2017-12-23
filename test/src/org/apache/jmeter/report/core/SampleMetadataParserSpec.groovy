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
 */

package org.apache.jmeter.report.core;

import org.apache.jmeter.junit.spock.JMeterSpec;
import org.apache.jmeter.report.core.SampleMetaDataParser;
import spock.lang.Unroll

class SampleMetadataParserSpec extends JMeterSpec {

    @Unroll
    def "Parse headers (#headers) using separator (#separator) and get (#expectedNumberOfColumns)"() {
        given:
            def dataParser = new SampleMetaDataParser(separator);
        when:
            def metadata = dataParser.parse(headers);
        then:
            metadata.columns.size() == expectedNumberOfColumns;
        where:
            separator   	| headers   	| expectedNumberOfColumns
            ';'	as char		| "a;b;c;d;e"  	| 5
            '|' as char 	| "a|b|c|d|e" 	| 5
            '|' as char 	| "aa|bb|cc|dd|eef" 	| 5
            '&' as char 	| "a&b&c&d&e" 	| 5
            '\t' as char 	| "a	b c	d	e" 	| 4
    }

}
