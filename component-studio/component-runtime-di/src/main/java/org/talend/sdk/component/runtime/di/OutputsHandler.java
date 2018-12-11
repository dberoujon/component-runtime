/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di;

import java.util.Map;
import java.util.TreeMap;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.output.OutputFactory;

public class OutputsHandler extends BaseIOHandler {

    private final JsonProvider jsonProvider;

    private final JsonBuilderFactory jsonBuilderFactory;

    /**
     * Copy of input connections and their data
     */
    protected final Map<String, IO> cachedConnections = new TreeMap<>();

    public OutputsHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        super(jsonb, servicesMapper);
        this.jsonProvider = (JsonProvider) servicesMapper.get(JsonProvider.class);
        this.jsonBuilderFactory = (JsonBuilderFactory) servicesMapper.get(JsonBuilderFactory.class);
    }

    public OutputFactory asOutputFactory() {
        return name -> value -> {
            final BaseIOHandler.IO ref = connections.get(getActualName(name));
            if (ref != null && value != null) {
                final String jsonValueMapper;
                if (value instanceof javax.json.JsonValue) {
                    jsonValueMapper = value.toString();
                } else if (value instanceof Record) {
                    jsonValueMapper = converters
                            .toType(converters.toRecord(value, () -> jsonb, () -> recordBuilderMapper),
                                    JsonObject.class, () -> jsonBuilderFactory, () -> jsonProvider, () -> jsonb)
                            .toString();
                } else {
                    jsonValueMapper = jsonb.toJson(value);
                }

                ref.add(jsonb.fromJson(jsonValueMapper, ref.getType()));
            }
        };
    }

    public void addCachedConnection(final String connectorName, final Class<?> type) {
        cachedConnections.put(connectorName, new IO<>(type));
    }

    public void resetCached() {
        cachedConnections.values().forEach(IO::reset);
    }

    public <T> void setCachedValue(final String name, final T value) {
        final IO input = cachedConnections.get(getActualName(name));
        if (input != null) {
            input.add(value);
        }
    }

    /**
     * Copies records from all inputs to reject
     */
    public void processException(Exception e) {
        final BaseIOHandler.IO reject = connections.get("REJECT");

        cachedConnections.values().forEach(io -> {
            while (io.hasNext()) {
                reject.add(io.next());
            }
        });
    }

}
