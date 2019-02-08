/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.record;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.spi.JsonProvider;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

import routines.system.IPersistableRow;

class RecordConvertersTest {

    private final RecordConverters converter = new RecordConverters();

    private final JsonProvider jsonProvider = JsonProvider.provider();

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(emptyMap());

    private final RecordBuilderFactoryImpl recordBuilderFactory = new RecordBuilderFactoryImpl("test");

    @Test
    void nullSupport() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", null).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertNull(json.getJsonString("value"));
        }
    }

    @Test
    void booleanRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withBoolean("value", true).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertTrue(json.getBoolean("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertTrue(toRecord.getBoolean("value"));
        }
    }

    @Test
    void intRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withInt("value", 2).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final IntStruct struct = IntStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, IntStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.INT, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(2, toRecord.getInt("value"));
        }
    }

    @Test
    void booleanRoundTripPojo() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withBoolean("value", true).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final BoolStruct struct = BoolStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, BoolStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.BOOLEAN, toRecord.getSchema().getEntries().iterator().next().getType());
            assertTrue(toRecord.getBoolean("value"));
        }
    }

    @Test
    void stringRoundTrip() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", "yes").build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals("yes", json.getString("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals("yes", toRecord.getString("value"));
        }
    }

    @Test
    void bytesRoundTrip() throws Exception {
        final byte[] bytes = new byte[] { 1, 2, 3 };
        final Record record = recordBuilderFactory.newRecordBuilder().withBytes("value", bytes).build();
        try (final Jsonb jsonb =
                JsonbBuilder.create(new JsonbConfig().withBinaryDataStrategy(BinaryDataStrategy.BASE_64))) {
            final JsonObject json = JsonObject.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, JsonObject.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(Base64.getEncoder().encodeToString(bytes), json.getString("value"));
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertArrayEquals(bytes, toRecord.getBytes("value"));

            // now studio generator kind of convertion
            final BytesStruct struct = jsonb.fromJson(json.toString(), BytesStruct.class);
            assertArrayEquals(bytes, struct.value);
            final String jsonFromStruct = jsonb.toJson(struct);
            assertEquals("{\"value\":\"AQID\"}", jsonFromStruct);
            final Record structToRecordFromJson = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), json, () -> jsonb,
                            () -> recordBuilderFactory);
            assertArrayEquals(bytes, structToRecordFromJson.getBytes("value"));
        }
    }

    @Test
    void convertByteToInt() throws Exception {
        final byte byteValue = 42;
        final Record record = recordBuilderFactory.newRecordBuilder().withInt("value", byteValue).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final NullableByteStruct struct = NullableByteStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, NullableByteStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(byteValue, struct.value);
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.INT, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(byteValue, toRecord.getInt("value"));
        }
    }

    @Test
    void convertByteToIntNull() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final NullableByteStruct struct = NullableByteStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, NullableByteStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(null, struct.value);
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.INT, toRecord.getSchema().getEntries().iterator().next().getType());
            assertFalse(toRecord.getOptionalInt("value").isPresent());
        }
    }

    @Test
    void convertCharToString() throws Exception {
        final char expectedChar = 'c';
        final String expectedString = "c";
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", expectedString).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final NullableCharStruct struct = NullableCharStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, NullableCharStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(expectedChar, struct.value);
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.STRING, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(expectedString, toRecord.getString("value"));
        }
    }

    @Test
    void convertCharToStringTrim() throws Exception {
        final char expectedChar = 'c';
        final String charString = "char";
        final String expcetedString = "c";
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", charString).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final NullableCharStruct struct = NullableCharStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, NullableCharStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(expectedChar, struct.value);
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.STRING, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(expcetedString, toRecord.getString("value"));
        }
    }

    @Test
    void convertCharToStringNull() throws Exception {
        final Record record = recordBuilderFactory.newRecordBuilder().withString("value", null).build();
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final NullableCharStruct struct = NullableCharStruct.class
                    .cast(converter
                            .toType(new RecordConverters.MappingMetaRegistry(), record, NullableCharStruct.class,
                                    () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb,
                                    () -> recordBuilderFactory));
            assertEquals(null, struct.value);
            final Record toRecord = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(), struct, () -> jsonb,
                            () -> recordBuilderFactory);
            assertEquals(Schema.Type.STRING, toRecord.getSchema().getEntries().iterator().next().getType());
            assertEquals(null, toRecord.getString("value"));
        }
    }

    @Test
    void convertDateToString() {
        final ZonedDateTime dateTime = ZonedDateTime.of(2017, 7, 17, 9, 0, 0, 0, ZoneId.of("GMT"));
        final String stringValue = dateTime.format(ISO_ZONED_DATE_TIME);
        new RecordConverters().coerce(ZonedDateTime.class, stringValue, "foo");
        final ZonedDateTime asDate = new RecordConverters().coerce(ZonedDateTime.class, stringValue, "foo");
        assertEquals(dateTime, asDate);
        final String asString = new RecordConverters().coerce(String.class, stringValue, "foo");
        assertEquals(stringValue, asString);
    }

    @Test
    void convertListString() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            Json
                                    .createObjectBuilder()
                                    .add("list",
                                            Json
                                                    .createArrayBuilder()
                                                    .add(Json.createValue("a"))
                                                    .add(Json.createValue("b"))
                                                    .build())
                                    .build(),
                            () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<String> list = record.getArray(String.class, "list");
            assertEquals(asList("a", "b"), list);
        }
    }

    @Test
    void convertListObject() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Record record = converter
                    .toRecord(new RecordConverters.MappingMetaRegistry(),
                            Json
                                    .createObjectBuilder()
                                    .add("list",
                                            Json
                                                    .createArrayBuilder()
                                                    .add(Json.createObjectBuilder().add("name", "a").build())
                                                    .add(Json.createObjectBuilder().add("name", "b").build())
                                                    .build())
                                    .build(),
                            () -> jsonb, () -> new RecordBuilderFactoryImpl("test"));
            final Collection<Record> list = record.getArray(Record.class, "list");
            assertEquals(asList("a", "b"), list.stream().map(it -> it.getString("name")).collect(toList()));
        }
    }

    public static class BytesStruct {

        public byte[] value;
    }

    public static class IntStruct implements IPersistableRow {

        public int value;
    }

    public static class BoolStruct implements IPersistableRow {

        public boolean value;
    }

    public static class NullableByteStruct implements IPersistableRow {

        public Byte value;
    }

    public static class NullableCharStruct implements IPersistableRow {

        public Character value;
    }

    public static class BigDecimalStruct implements IPersistableRow {

        public BigDecimal value;
    }
}
