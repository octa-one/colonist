/*
 * Copyright 2019 Michael Rozumyanskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.michaelrocks.colonist.modular;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.michaelrocks.colonist.AcceptSettlersViaCallback;
import io.michaelrocks.colonist.Colony;
import io.michaelrocks.colonist.ProduceSettlersViaConstructor;
import io.michaelrocks.colonist.SelectSettlersByAnnotation;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Colony
@SelectSettlersByAnnotation(AntSettler.class)
@ProduceSettlersViaConstructor
@AcceptSettlersViaCallback
public @interface AntColony {
}
