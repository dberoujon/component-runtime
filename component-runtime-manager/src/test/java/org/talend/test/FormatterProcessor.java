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
package org.talend.test;

import java.io.Serializable;

import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;

@Processor(family = "chain", name = "formatter")
public class FormatterProcessor implements Serializable {

    @ElementListener
    public void length(@Input("firstName") final String firstName, @Input("lastName") final String lastName,
            @Output("firstName") final OutputEmitter<String> lowerCase,
            @Output("lastName") final OutputEmitter<String> upperCase) {

        lowerCase.emit(firstName == null ? null : firstName.toLowerCase());
        upperCase.emit(lastName == null ? null : lastName.toUpperCase());
    }
}