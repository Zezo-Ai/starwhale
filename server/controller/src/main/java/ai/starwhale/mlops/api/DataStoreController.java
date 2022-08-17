/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.datastore.ColumnDesc;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.RecordListVO;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryFilterDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryOperandDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreQueryRequest;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.exception.SWValidationException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${sw.controller.apiPrefix}")
@Slf4j
public class DataStoreController implements DataStoreApi {

    @Resource
    @Setter
    private DataStore dataStore;

    @Override
    public ResponseEntity<ResponseMessage<String>> updateTable(UpdateTableRequest request) {
        try {
            if (request.getTableName() == null) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                        .tip("table name should not be null");
            }
            List<Map<String, String>> records;
            if (request.getRecords() == null) {
                records = null;
            } else {
                records = request.getRecords()
                        .stream()
                        .map(x -> {
                            if (x.getValues() == null) {
                                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                                        .tip("values should not be null. " + x);
                            }
                            var ret = new HashMap<String, String>();
                            for (var r : x.getValues()) {
                                ret.put(r.getKey(), r.getValue());
                            }
                            return ret;
                        })
                        .collect(Collectors.toList());
            }
            this.dataStore.update(request.getTableName(), request.getTableSchemaDesc(), records);
            return ResponseEntity.ok(Code.success.asResponse("success"));
        } catch (SWValidationException e) {
            throw e.tip("request=" + request);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<RecordListVO>> queryTable(QueryTableRequest request) {
        try {
            if (request.getTableName() == null) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                        .tip("table name should not be null");
            }
            var recordList = this.dataStore.query(DataStoreQueryRequest.builder()
                    .tableName(request.getTableName())
                    .columns(DataStoreController.convertColumns(request.getColumns()))
                    .filter(DataStoreController.convertFilter(request.getFilter()))
                    .orderBy(request.getOrderBy())
                    .start(request.getStart())
                    .limit(request.getLimit())
                    .build());
            return ResponseEntity.ok(Code.success.asResponse(RecordListVO.builder()
                    .columnTypes(recordList.getColumnTypeMap())
                    .records(recordList.getRecords())
                    .build()));
        } catch (SWValidationException e) {
            throw e.tip("request=" + request);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<RecordListVO>> scanTable(ScanTableRequest request) {
        try {
            if (request.getTables() == null || request.getTables().isEmpty()) {
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                        .tip("tables should not be null or empty.");
            }
            var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                    .tables(request.getTables().stream()
                            .map(x -> {
                                if (x == null) {
                                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                                            .tip("table description should not be null");
                                }
                                if (x.getTableName() == null || x.getTableName().isEmpty()) {
                                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                                            .tip("table name should not be null or empty: " + x);
                                }
                                return DataStoreScanRequest.TableInfo.builder()
                                        .tableName(x.getTableName())
                                        .columns(DataStoreController.convertColumns(x.getColumns()))
                                        .keepNone(x.isKeepNone())
                                        .build();
                            })
                            .collect(Collectors.toList()))
                    .start(request.getStart())
                    .end(request.getEnd())
                    .startInclusive(request.isStartInclusive())
                    .endInclusive(request.isEndInclusive())
                    .limit(request.getLimit())
                    .keepNone(request.isKeepNone())
                    .build());
            return ResponseEntity.ok(Code.success.asResponse(RecordListVO.builder()
                    .columnTypes(recordList.getColumnTypeMap())
                    .records(recordList.getRecords())
                    .lastKey(recordList.getLastKey())
                    .build()));
        } catch (SWValidationException e) {
            throw e.tip("request=" + request);
        }
    }

    private static TableQueryFilter convertFilter(TableQueryFilterDesc input) {
        if (input == null) {
            return null;
        }
        if (input.getOperator() == null) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "operator should not be empty. " + input);
        }
        if (input.getOperands() == null || input.getOperands().isEmpty()) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "operands should not be empty. " + input);
        }

        TableQueryFilter.Operator operator;
        try {
            operator = TableQueryFilter.Operator.valueOf(input.getOperator());
        } catch (IllegalArgumentException e) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "invalid operator " + input.getOperator() + ". " + input);
        }
        switch (operator) {
            case NOT:
                if (input.getOperands().size() != 1) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                            "'NOT' should have only one operand. " + input);
                }
                break;
            case AND:
            case OR:
            case EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                if (input.getOperands().size() != 2) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                            "operator '" + operator + "' should have 2 operands. " + input);
                }
                break;
            default:
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                        "unexpected operator " + operator);
        }
        var ret = TableQueryFilter.builder()
                .operator(operator)
                .operands(input.getOperands()
                        .stream()
                        .map(DataStoreController::convertOperand)
                        .collect(Collectors.toList()))
                .build();
        switch (operator) {
            case NOT:
            case AND:
            case OR:
                for (var operand : ret.getOperands()) {
                    if (!(operand instanceof TableQueryFilter)) {
                        throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                                MessageFormat.format("unsupported operand {0} for operator {1}", operand, operator));
                    }
                }
                break;
            case EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                boolean hasColumn = false;
                for (var operand : ret.getOperands()) {
                    if (operand instanceof TableQueryFilter
                            || (operand == null && operator != TableQueryFilter.Operator.EQUAL)) {
                        throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                                MessageFormat.format("unsupported operand {0} for operator {1}", operand, operator));
                    }
                    if (operand instanceof TableQueryFilter.Column) {
                        hasColumn = true;
                    }
                }
                if (!hasColumn) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                            "operator " + operator + " should have at least one column operand");
                }
                break;
            default:
                throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                        "unexpected operator " + operator);
        }
        return ret;
    }

    private static Object convertOperand(TableQueryOperandDesc operand) {
        if (operand == null) {
            throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE).tip(
                    "operand should not be null");
        }
        if (operand.getFilter() != null) {
            return DataStoreController.convertFilter(operand.getFilter());
        }
        if (operand.getColumnName() != null) {
            return new TableQueryFilter.Column(operand.getColumnName());
        }
        if (operand.getBoolValue() != null) {
            return operand.getBoolValue();
        }
        if (operand.getIntValue() != null) {
            return operand.getIntValue();
        }
        if (operand.getFloatValue() != null) {
            return operand.getFloatValue();
        }
        if (operand.getStringValue() != null) {
            return operand.getStringValue();
        }
        if (operand.getBytesValue() != null) {
            return ColumnType.BYTES.decode(operand.getBytesValue());
        }
        return null;
    }

    private static Map<String, String> convertColumns(List<ColumnDesc> columns) {
        Map<String, String> ret = null;
        if (columns != null) {
            ret = new HashMap<>();
            for (var col : columns) {
                if (col == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                            .tip("column should not be null");
                }
                var name = col.getColumnName();
                if (name == null) {
                    throw new SWValidationException(SWValidationException.ValidSubject.DATASTORE)
                            .tip("column name should not be null. " + col);
                }
                var alias = col.getAlias();
                if (alias == null) {
                    alias = name;
                }
                ret.put(name, alias);
            }
        }
        return ret;
    }
}