/*
 * Copyright 2019 SIA Joom
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

package com.joom.colonist.processor

import java.nio.file.Path

data class ColonistParameters(
  val inputs: List<Path>,
  val outputs: List<Path>,
  val generationOutput: Path,
  val discoveryClasspath: List<Path>,
  val classpath: List<Path>,
  val bootClasspath: List<Path>,
  val discoverSettlers: Boolean,
)
