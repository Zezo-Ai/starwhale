package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.datastore.ColumnDesc;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.RecordDesc;
import ai.starwhale.mlops.api.protocol.datastore.RecordValueDesc;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryFilterDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryOperandDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.datastore.ColumnSchemaDesc;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.OrderByDesc;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.TableSchemaDesc;
import ai.starwhale.mlops.exception.SWValidationException;
import brave.internal.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataStoreControllerTest {
    private DataStoreController controller;

    @BeforeEach
    public void setUp() {
        this.controller = new DataStoreController();
        this.controller.setDataStore(new DataStore());
    }

    @Test
    public void testUpdate() {
        this.controller.updateTable(new UpdateTableRequest() {{
            setTableName("t1");
            setTableSchemaDesc(new TableSchemaDesc("k",
                    List.of(new ColumnSchemaDesc("k", "INT32"),
                            new ColumnSchemaDesc("a", "INT32"))));
            setRecords(List.of(new RecordDesc() {{
                setValues(List.of(new RecordValueDesc() {{
                    setKey("k");
                    setValue("0");
                }}, new RecordValueDesc() {{
                    setKey("a");
                    setValue("1");
                }}));
            }}));
        }});
        this.controller.updateTable(new UpdateTableRequest() {{
            setTableName("t1");
            setRecords(List.of(new RecordDesc() {{
                setValues(List.of(new RecordValueDesc() {{
                    setKey("k");
                    setValue("1");
                }}, new RecordValueDesc() {{
                    setKey("a");
                    setValue("2");
                }}));
            }}));
        }});
        this.controller.updateTable(new UpdateTableRequest() {{
            setTableName("t2");
            setTableSchemaDesc(new TableSchemaDesc("k",
                    List.of(new ColumnSchemaDesc("k", "INT32"),
                            new ColumnSchemaDesc("x", "INT32"))));
            setRecords(List.of(new RecordDesc() {{
                setValues(List.of(new RecordValueDesc() {{
                    setKey("k");
                    setValue("3");
                }}, new RecordValueDesc() {{
                    setKey("x");
                    setValue("2");
                }}));
            }}));
        }});
        this.controller.updateTable(new UpdateTableRequest() {{
            setTableName("t1");
            setRecords(List.of(new RecordDesc() {{
                setValues(List.of(new RecordValueDesc() {{
                    setKey("k");
                    setValue("0");
                }}, new RecordValueDesc() {{
                    setKey("-");
                    setValue("1");
                }}));
            }}, new RecordDesc() {{
                setValues(List.of(new RecordValueDesc() {{
                    setKey("k");
                    setValue("4");
                }}, new RecordValueDesc() {{
                    setKey("-");
                    setValue("1");
                }}));
            }}));
        }});
        var resp = this.controller.scanTable(new ScanTableRequest() {{
            setTables(List.of(new TableDesc() {{
                setTableName("t1");
                setColumns(List.of(new ColumnDesc() {{
                    setColumnName("k");
                }}, new ColumnDesc() {{
                    setColumnName("a");
                    setAlias("b");
                }}));
            }}));
        }});
        assertThat("t1", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("t1",
                Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                is(Map.of("k", ColumnType.INT32, "b", ColumnType.INT32)));
        assertThat("t1",
                Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                is(List.of(Map.of("k", "1", "b", "2"))));
        resp = this.controller.scanTable(new ScanTableRequest() {{
            setTables(List.of(new TableDesc() {{
                setTableName("t2");
                setColumns(List.of(new ColumnDesc() {{
                    setColumnName("k");
                }}, new ColumnDesc() {{
                    setColumnName("x");
                    setAlias("b");
                }}));
            }}));
        }});
        assertThat("t2", resp.getStatusCode().is2xxSuccessful(), is(true));
        assertThat("t2",
                Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                is(Map.of("k", ColumnType.INT32, "b", ColumnType.INT32)));
        assertThat("t2",
                Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                is(List.of(Map.of("k", "3", "b", "2"))));
    }

    @Nested
    public class UpdateTest {
        private UpdateTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new UpdateTableRequest() {{
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "INT32"),
                                new ColumnSchemaDesc("a", "INT32"))));
                setRecords(List.of(new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("0");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("1");
                    }}));
                }}));
            }};
        }

        @Test
        public void ensureRequest() {
            DataStoreControllerTest.this.controller.updateTable(req);
        }

        @Test
        public void testNullTableTable() {
            req.setTableName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullSchema() {
            req.setTableSchemaDesc(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullKeyInSchema() {
            req.getTableSchemaDesc().setKeyColumn(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullNameInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullTypeInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setType(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidTypeInColumnSchema() {
            req.getTableSchemaDesc().getColumnSchemaList().get(0).setType("null");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNoKeyColumnSchema() {
            req.getTableSchemaDesc().setColumnSchemaList(req.getTableSchemaDesc().getColumnSchemaList().subList(1, 1));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testDuplicateColumnSchema() {
            req.getTableSchemaDesc().setColumnSchemaList(
                    Lists.concat(req.getTableSchemaDesc().getColumnSchemaList(),
                            List.of(req.getTableSchemaDesc().getColumnSchemaList().get(1))));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testNullKeyInUpdates() {
            req.getRecords().get(0).getValues().get(0).setKey(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidKeyInUpdates() {
            req.getRecords().get(0).getValues().get(0).setKey("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }

        @Test
        public void testInvalidValueInUpdates() {
            req.getRecords().get(0).getValues().get(1).setValue("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.updateTable(req),
                    "");
        }
    }

    @Nested
    public class QueryTest {
        private QueryTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new QueryTableRequest() {{
                setTableName("t1");
                setColumns(List.of(new ColumnDesc() {{
                    setColumnName("k");
                }}, new ColumnDesc() {{
                    setColumnName("a");
                    setAlias("b");
                }}));
                setFilter(new TableQueryFilterDesc() {{
                    setOperator(TableQueryFilter.Operator.NOT.toString());
                    setOperands(List.of(new TableQueryOperandDesc() {{
                        setFilter(new TableQueryFilterDesc() {{
                            setOperator(TableQueryFilter.Operator.AND.toString());
                            setOperands(List.of(new TableQueryOperandDesc() {{
                                setFilter(new TableQueryFilterDesc() {{
                                    setOperator(TableQueryFilter.Operator.GREATER.toString());
                                    setOperands(List.of(new TableQueryOperandDesc() {{
                                        setColumnName("a");
                                    }}, new TableQueryOperandDesc() {{
                                        setIntValue(1L);
                                    }}));
                                }});
                            }}, new TableQueryOperandDesc() {{
                                setFilter(new TableQueryFilterDesc() {{
                                    setOperator(TableQueryFilter.Operator.LESS.toString());
                                    setOperands(List.of(new TableQueryOperandDesc() {{
                                        setColumnName("a");
                                    }}, new TableQueryOperandDesc() {{
                                        setIntValue(4L);
                                    }}));
                                }});
                            }}));
                        }});
                    }}));
                }});
                setOrderBy(List.of(new OrderByDesc("a")));
                setStart(1);
                setLimit(2);
            }};
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {{
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "INT32"),
                                new ColumnSchemaDesc("a", "INT32"))));
                setRecords(List.of(new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("0");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("5");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("1");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("4");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("2");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("3");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("3");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("2");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("4");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("1");
                    }}));
                }}));
            }});
        }

        @Test
        public void testQueryDefault() {
            var resp = DataStoreControllerTest.this.controller.queryTable(new QueryTableRequest() {{
                setTableName("t1");
            }});
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "a", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "0", "a", "5"),
                            Map.of("k", "1", "a", "4"),
                            Map.of("k", "2", "a", "3"),
                            Map.of("k", "3", "a", "2"),
                            Map.of("k", "4", "a", "1"))));
        }

        @Test
        public void testQuery() {
            var resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "b", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "4"),
                            Map.of("k", "0", "b", "5"))));

            this.req.getOrderBy().get(0).setDescending(true);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "4"),
                            Map.of("k", "4", "b", "1"))));

            this.req.setLimit(1);
            resp = DataStoreControllerTest.this.controller.queryTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "b", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "4"))));
        }

        @Test
        public void testNullTableName() {
            this.req.setTableName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullColumnName() {
            this.req.getColumns().get(0).setColumnName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullColumnDesc() {
            this.req.setColumns(Lists.concat(this.req.getColumns(), new ArrayList<>() {{
                add(null);
            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidColumnName() {
            this.req.getColumns().get(0).setColumnName("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOrderBy() {
            this.req.setOrderBy(Lists.concat(this.req.getOrderBy(), new ArrayList<>() {{
                add(null);
            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOrderBy() {
            this.req.setOrderBy(Lists.concat(this.req.getOrderBy(), new ArrayList<>() {{
                add(new OrderByDesc("i"));
            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOperator() {
            this.req.getFilter().setOperator(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOperator() {
            this.req.getFilter().setOperator("invalid");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullOperands() {
            this.req.getFilter().setOperands(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testNullValueInOperands() {
            this.req.getFilter().setOperands(new ArrayList<>() {{
                add(null);
            }});
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidNumberOfOperands() {
            var dummyFilter = new TableQueryOperandDesc() {{
                setFilter(new TableQueryFilterDesc() {{
                    setOperator(TableQueryFilter.Operator.EQUAL.toString());
                    setOperands(List.of(new TableQueryOperandDesc() {{
                        setColumnName("a");
                    }}, new TableQueryOperandDesc() {{
                        setStringValue("1");
                    }}));
                }});
            }};
            var dummyColumn = new TableQueryOperandDesc() {{
                setColumnName("a");
            }};
            var dummyIntValue = new TableQueryOperandDesc() {{
                setIntValue(1L);
            }};

            this.req.getFilter().setOperator(TableQueryFilter.Operator.NOT.toString());
            this.req.getFilter().setOperands(List.of());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperands(List.of(dummyFilter, dummyFilter));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperator(TableQueryFilter.Operator.AND.toString());
            this.req.getFilter().setOperands(List.of(dummyFilter));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperands(List.of(dummyFilter, dummyFilter, dummyFilter));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperator(TableQueryFilter.Operator.OR.toString());
            this.req.getFilter().setOperands(List.of(dummyFilter));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperands(List.of(dummyFilter, dummyFilter, dummyFilter));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn, dummyIntValue, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testInvalidOperands() {
            var dummyFilter = new TableQueryOperandDesc() {{
                setFilter(new TableQueryFilterDesc() {{
                    setOperator(TableQueryFilter.Operator.EQUAL.toString());
                    setOperands(List.of(new TableQueryOperandDesc() {{
                        setColumnName("a");
                    }}, new TableQueryOperandDesc() {{
                        setStringValue("1");
                    }}));
                }});
            }};
            var dummyColumn = new TableQueryOperandDesc() {{
                setColumnName("a");
            }};
            var dummyIntValue = new TableQueryOperandDesc() {{
                setIntValue(1L);
            }};

            this.req.getFilter().setOperands(List.of(dummyColumn));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.NOT.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyColumn, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.AND.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.OR.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");

            this.req.getFilter().setOperands(List.of(dummyFilter, dummyIntValue));
            this.req.getFilter().setOperator(TableQueryFilter.Operator.EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.LESS_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
            this.req.getFilter().setOperator(TableQueryFilter.Operator.GREATER_EQUAL.toString());
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }

        @Test
        public void testIncomparableOperands() {
            this.req.getFilter().setOperator("EQUAL");
            this.req.getFilter().setOperands(List.of(new TableQueryOperandDesc() {{
                setColumnName("a");
            }}, new TableQueryOperandDesc() {{
                setStringValue("1");
            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.queryTable(this.req),
                    "");
        }
    }

    @Nested
    public class ScanTest {
        private ScanTableRequest req;

        @BeforeEach
        public void setUp() {
            this.req = new ScanTableRequest() {{
                setTables(List.of(new TableDesc() {{
                    setTableName("t1");
                    setColumns(List.of(new ColumnDesc() {{
                        setColumnName("k");
                        setAlias("b");
                    }}, new ColumnDesc() {{
                        setColumnName("a");
                    }}));
                }}, new TableDesc() {{
                    setTableName("t2");
                    setKeepNone(true);
                }}));
                setStart("1");
                setEnd("4");
                setKeepNone(true);
            }};
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {{
                setTableName("t1");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "INT32"),
                                new ColumnSchemaDesc("a", "INT32"))));
                setRecords(List.of(new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("0");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("5");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("1");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("4");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("2");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("3");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("3");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("2");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("4");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("1");
                    }}));
                }}));
            }});
            DataStoreControllerTest.this.controller.updateTable(new UpdateTableRequest() {{
                setTableName("t2");
                setTableSchemaDesc(new TableSchemaDesc("k",
                        List.of(new ColumnSchemaDesc("k", "INT32"),
                                new ColumnSchemaDesc("a", "INT32"))));
                setRecords(List.of(new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("1");
                    }}, new RecordValueDesc() {{
                        setKey("a");
                        setValue("10");
                    }}));
                }}, new RecordDesc() {{
                    setValues(List.of(new RecordValueDesc() {{
                        setKey("k");
                        setValue("2");
                    }},new RecordValueDesc() {{
                        setKey("a");
                    }}));
                }}));
            }});
        }

        @Test
        public void testScanDefault() {
            var resp = DataStoreControllerTest.this.controller.scanTable(new ScanTableRequest() {{
                setTables(List.of(new TableDesc() {{
                    setTableName("t1");
                }}));
            }});
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "a", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "0", "a", "5"),
                            Map.of("k", "1", "a", "4"),
                            Map.of("k", "2", "a", "3"),
                            Map.of("k", "3", "a", "2"),
                            Map.of("k", "4", "a", "1"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("4"));
        }

        @Test
        public void testScan() {
            var resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "a", ColumnType.INT32, "b", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "1", "a", "10"),
                            new HashMap<>() {{
                                put("k", "2");
                                put("b", "2");
                                put("a", null);
                            }},
                            Map.of("b", "3", "a", "2"))));

            this.req.setLimit(1);
            resp = DataStoreControllerTest.this.controller.scanTable(this.req);
            assertThat("test", resp.getStatusCode().is2xxSuccessful(), is(true));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getColumnTypes(),
                    is(Map.of("k", ColumnType.INT32, "a", ColumnType.INT32, "b", ColumnType.INT32)));
            assertThat("test",
                    Objects.requireNonNull(resp.getBody()).getData().getRecords(),
                    is(List.of(Map.of("k", "1", "b", "1", "a", "10"))));
            assertThat("test", Objects.requireNonNull(resp.getBody()).getData().getLastKey(), is("1"));
        }

        @Test
        public void testNullTableDesc() {
            this.req.setTables(Lists.concat(this.req.getTables(), new ArrayList<>() {{
                add(null);
            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testNullTableName() {
            this.req.getTables().get(0).setTableName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidTableName() {
            this.req.getTables().get(0).setTableName("invalid");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testNullColumn() {
            this.req.getTables().get(0).setColumns(
                    Lists.concat(this.req.getTables().get(0).getColumns(),
                            new ArrayList<>() {{
                                add(null);
                            }}));
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testNullColumnName() {
            this.req.getTables().get(0).getColumns().get(0).setColumnName(null);
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidColumnName() {
            this.req.getTables().get(0).getColumns().get(0).setColumnName("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidStart() {
            this.req.setStart("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }

        @Test
        public void testInvalidEnd() {
            this.req.setEnd("i");
            assertThrows(SWValidationException.class,
                    () -> DataStoreControllerTest.this.controller.scanTable(this.req),
                    "");
        }
    }
}