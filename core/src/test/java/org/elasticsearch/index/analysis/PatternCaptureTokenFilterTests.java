/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ESTokenStreamTestCase;
import org.elasticsearch.test.IndexSettingsModule;

import java.util.Collections;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.containsString;

public class PatternCaptureTokenFilterTests extends ESTokenStreamTestCase {
    public void testPatternCaptureTokenFilter() throws Exception {
        String json = "/org/elasticsearch/index/analysis/pattern_capture.json";
        Index index = new Index("test");
        Settings settings = settingsBuilder()
                .put("path.home", createTempDir())
                .loadFromStream(json, getClass().getResourceAsStream(json))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(settings), new EnvironmentModule(new Environment(settings))).createInjector();
        Injector injector = new ModulesBuilder().add(
                new IndexSettingsModule(index, settings),
                new AnalysisModule(settings, parentInjector.getInstance(IndicesAnalysisService.class)))
                .createChildInjector(parentInjector);

        AnalysisService analysisService = injector.getInstance(AnalysisService.class);

        NamedAnalyzer analyzer1 = analysisService.analyzer("single");

        assertTokenStreamContents(analyzer1.tokenStream("test", "foobarbaz"), new String[]{"foobarbaz","foobar","foo"});

        NamedAnalyzer analyzer2 = analysisService.analyzer("multi");

        assertTokenStreamContents(analyzer2.tokenStream("test", "abc123def"), new String[]{"abc123def","abc","123","def"});

        NamedAnalyzer analyzer3 = analysisService.analyzer("preserve");

        assertTokenStreamContents(analyzer3.tokenStream("test", "foobarbaz"), new String[]{"foobar","foo"});
    }

    public void testNoPatterns() {
        try {
            new PatternCaptureGroupTokenFilterFactory(IndexSettingsModule.newIndexSettings(new Index("test"), Settings.EMPTY, Collections.EMPTY_LIST), "pattern_capture", settingsBuilder().put("pattern", "foobar").build());
            fail ("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("required setting 'patterns' is missing"));
        }
    }

}
