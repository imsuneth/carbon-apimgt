/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY

 * * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.common.gateway.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.schema.GraphQLSchema;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.common.gateway.dto.QueryAnalyzerResponseDTO;

import java.util.List;

/**
 * This class contains methods using for Graphql query depth and complexity analysis.
 */
public class QueryAnalyzer {

    private static final Log log = LogFactory.getLog(QueryAnalyzer.class);
    private final GraphQLSchema schema;

    public QueryAnalyzer(GraphQLSchema schema) {
        this.schema = schema;
    }

    /**
     * This method analyses the query depth.
     *
     * @param maxQueryDepth maximum query depth
     * @param payload       payload of the request
     * @return true, if the query depth does not exceed the maximum value or false, if query depth exceeds the maximum
     */
    public QueryAnalyzerResponseDTO analyseQueryDepth(int maxQueryDepth, String payload) {

        QueryAnalyzerResponseDTO queryAnalyzerResponseDTO = new QueryAnalyzerResponseDTO();
        if (maxQueryDepth > 0) {
            MaxQueryDepthInstrumentation maxQueryDepthInstrumentation =
                    new MaxQueryDepthInstrumentation(maxQueryDepth);
            GraphQL runtime = GraphQL.newGraphQL(schema).instrumentation(maxQueryDepthInstrumentation).build();

            try {
                ExecutionResult executionResult = runtime.execute(payload);
                List<GraphQLError> errors = executionResult.getErrors();
                if (errors.size() > 0) {
                    for (GraphQLError error : errors) {
                        queryAnalyzerResponseDTO.addErrorToList((error.getMessage()));
                    }
                    // TODO: https://github.com/wso2/carbon-apimgt/issues/8147
                    queryAnalyzerResponseDTO.getErrorList().removeIf(s -> s.contains("non-nullable"));
                    if (queryAnalyzerResponseDTO.getErrorList().size() == 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Maximum query depth of " + maxQueryDepth + " was not exceeded");
                        }
                        queryAnalyzerResponseDTO.setSuccess(true);
                        return queryAnalyzerResponseDTO;
                    }
                    log.error(queryAnalyzerResponseDTO.getErrorList().toString());
                    queryAnalyzerResponseDTO.setSuccess(false);
                    return queryAnalyzerResponseDTO;
                }
                queryAnalyzerResponseDTO.setSuccess(true);
                return queryAnalyzerResponseDTO;
            } catch (Throwable e) {
                log.error(e);
            }
        } else {
            queryAnalyzerResponseDTO.setSuccess(true); // No depth limitation check
            return queryAnalyzerResponseDTO;
        }
        queryAnalyzerResponseDTO.setSuccess(false);
        return queryAnalyzerResponseDTO;
    }

    /**
     * This method analyses the query complexity.
     *
     * @param fieldComplexityCalculator Field Complexity Calculator
     * @param maxQueryComplexity        Maximum query complexity value
     * @param payload                   payload of the request
     * @return true, if query complexity does not exceed the maximum or false, if query complexity exceeds the maximum
     */
    public QueryAnalyzerResponseDTO analyseQueryComplexity(int maxQueryComplexity, String payload,
                                                           FieldComplexityCalculator fieldComplexityCalculator) {

        QueryAnalyzerResponseDTO queryAnalyzerResponseDTO = new QueryAnalyzerResponseDTO();
        if (maxQueryComplexity > 0) {
            MaxQueryComplexityInstrumentation maxQueryComplexityInstrumentation =
                    new MaxQueryComplexityInstrumentation(maxQueryComplexity, fieldComplexityCalculator);
            GraphQL runtime = GraphQL.newGraphQL(schema).instrumentation(maxQueryComplexityInstrumentation).build();

            try {
                ExecutionResult executionResult = runtime.execute(payload);
                List<GraphQLError> errors = executionResult.getErrors();
                if (errors.size() > 0) {
                    for (GraphQLError error : errors) {
                        queryAnalyzerResponseDTO.addErrorToList((error.getMessage()));
                    }
                    // TODO: https://github.com/wso2/carbon-apimgt/issues/8147
                    queryAnalyzerResponseDTO.getErrorList().removeIf(s -> s.contains("non-nullable"));
                    if (queryAnalyzerResponseDTO.getErrorList().size() == 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Maximum query complexity was not exceeded");
                        }
                        queryAnalyzerResponseDTO.setSuccess(true);
                    } else {
                        log.error(queryAnalyzerResponseDTO.getErrorList());
                        queryAnalyzerResponseDTO.getErrorList().clear();
                        queryAnalyzerResponseDTO.addErrorToList("maximum query complexity exceeded");
                    }
                    queryAnalyzerResponseDTO.setSuccess(false);
                    return queryAnalyzerResponseDTO;
                }
                queryAnalyzerResponseDTO.setSuccess(true);
                return queryAnalyzerResponseDTO;
            } catch (Throwable e) {
                log.error(e);
            }
        } else {
            queryAnalyzerResponseDTO.setSuccess(true); // No complexity limitation check
            return queryAnalyzerResponseDTO;
        }
        queryAnalyzerResponseDTO.setSuccess(false);
        return queryAnalyzerResponseDTO;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }
}