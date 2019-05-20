/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.parse.rule.registry;

import com.google.common.base.Optional;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.parse.filler.api.SQLSegmentFiller;
import org.apache.shardingsphere.core.parse.rule.jaxb.loader.RuleDefinitionFileConstant;
import org.apache.shardingsphere.core.parse.rule.jaxb.loader.extractor.ExtractorRuleDefinitionEntityLoader;
import org.apache.shardingsphere.core.parse.rule.jaxb.loader.filler.FillerRuleDefinitionEntityLoader;
import org.apache.shardingsphere.core.parse.rule.jaxb.loader.statement.SQLStatementRuleDefinitionEntityLoader;
import org.apache.shardingsphere.core.parse.rule.registry.statement.SQLStatementRule;
import org.apache.shardingsphere.core.parse.sql.segment.SQLSegment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse rule registry.
 *
 * @author zhangliang
 * @author duhongjun
 */
public abstract class ParseRuleRegistry {
    
    private final ParseRuleDefinition commonRuleDefinition = new ParseRuleDefinition();
    
    private final ExtractorRuleDefinitionEntityLoader extractorRuleDefinitionLoader = new ExtractorRuleDefinitionEntityLoader();
    
    private final FillerRuleDefinitionEntityLoader fillerRuleDefinitionLoader = new FillerRuleDefinitionEntityLoader();
    
    private final SQLStatementRuleDefinitionEntityLoader statementRuleDefinitionLoader = new SQLStatementRuleDefinitionEntityLoader();
    
    private final Map<DatabaseType, ParseRuleDefinition> parseRuleDefinitions = new HashMap<>(4, 1);
    
    protected final void init() {
        initGeneralParseRuleDefinition();
        initDialectParseRuleDefinition();
    }
    
    private void initGeneralParseRuleDefinition() {
        commonRuleDefinition.getExtractorRuleDefinition().init(extractorRuleDefinitionLoader.load(RuleDefinitionFileConstant.getCommonExtractorRuleDefinitionFileName()));
        commonRuleDefinition.getFillerRuleDefinition().init(fillerRuleDefinitionLoader.load(RuleDefinitionFileConstant.getCommonFillerRuleDefinitionFileName()));
    }
    
    private void initDialectParseRuleDefinition() {
        for (DatabaseType each : DatabaseType.values()) {
            if (DatabaseType.H2 == each) {
                continue;
            }
            ParseRuleDefinition shardingRuleDefinition = new ParseRuleDefinition();
            initParseRuleDefinitionFromCommon(shardingRuleDefinition, getExtractorFile(each), getFillerFiles(each), getStatementRuleFile(each));
            parseRuleDefinitions.put(each, shardingRuleDefinition);
        }
    }
    
    protected abstract String getExtractorFile(DatabaseType databaseType);
    
    protected abstract Collection<String> getFillerFiles(DatabaseType databaseType);
    
    protected abstract String getStatementRuleFile(DatabaseType databaseType);
    
    private void initParseRuleDefinitionFromCommon(final ParseRuleDefinition parseRuleDefinition, 
                                                   final String extractorFile, final Collection<String> fillerFilePaths, final String statementRuleFile) {
        parseRuleDefinition.getExtractorRuleDefinition().getRules().putAll(commonRuleDefinition.getExtractorRuleDefinition().getRules());
        parseRuleDefinition.getFillerRuleDefinition().getRules().putAll(commonRuleDefinition.getFillerRuleDefinition().getRules());
        initParseRuleDefinition(parseRuleDefinition, extractorFile, fillerFilePaths, statementRuleFile);
    }
    
    private void initParseRuleDefinition(final ParseRuleDefinition parseRuleDefinition, 
                                         final String extractorFile, final Collection<String> fillerFilePaths, final String statementRuleFile) {
        parseRuleDefinition.getExtractorRuleDefinition().init(extractorRuleDefinitionLoader.load(extractorFile));
        for (String each : fillerFilePaths) {
            parseRuleDefinition.getFillerRuleDefinition().init(fillerRuleDefinitionLoader.load(each));
        }
        parseRuleDefinition.getSqlStatementRuleDefinition().init(statementRuleDefinitionLoader.load(statementRuleFile), parseRuleDefinition.getExtractorRuleDefinition());
    }
    
    /**
     * Find SQL statement rule.
     *
     * @param databaseType database type
     * @param contextClassName context class name
     * @return SQL statement rule
     */
    public Optional<SQLStatementRule> findSQLStatementRule(final DatabaseType databaseType, final String contextClassName) {
        return Optional.fromNullable(parseRuleDefinitions.get(DatabaseType.H2 == databaseType ? DatabaseType.MySQL : databaseType).getSqlStatementRuleDefinition().getRules().get(contextClassName));
    }
    
    /**
     * Find SQL segment rule.
     *
     * @param databaseType database type
     * @param sqlSegmentClass SQL segment class
     * @return SQL segment rule
     */
    public Optional<SQLSegmentFiller> findSQLSegmentFiller(final DatabaseType databaseType, final Class<? extends SQLSegment> sqlSegmentClass) {
        return Optional.fromNullable(parseRuleDefinitions.get(DatabaseType.H2 == databaseType ? DatabaseType.MySQL : databaseType).getFillerRuleDefinition().getRules().get(sqlSegmentClass));
    }
}