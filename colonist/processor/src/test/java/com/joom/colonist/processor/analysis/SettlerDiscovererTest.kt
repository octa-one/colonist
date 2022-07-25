/*
 * Copyright 2022 SIA Joom
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

package com.joom.colonist.processor.analysis

import com.joom.colonist.processor.ErrorReporter
import com.joom.colonist.processor.integration.IntegrationTestRule
import com.joom.colonist.processor.integration.JvmRuntimeUtil
import com.joom.colonist.processor.integration.shouldNotHaveErrors
import com.joom.colonist.processor.model.Settler
import com.joom.colonist.processor.model.SettlerProducer
import com.joom.colonist.processor.model.SettlerSelector
import com.joom.grip.Grip
import com.joom.grip.GripFactory
import com.joom.grip.mirrors.Type
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import java.nio.file.Path
import kotlin.io.path.name
import org.junit.Rule
import org.junit.Test

class SettlerDiscovererTest {

  @get:Rule
  val rule = IntegrationTestRule(PACKAGE)

  @Test
  fun `public settlers selected by annotation do not raise errors`() {
    rule.assertValidProject("public_by_annotation")
  }

  @Test
  fun `public settlers selected by super type do not raise errors`() {
    rule.assertValidProject("public_by_supertype")
  }

  @Test
  fun `private settlers selected by annotation raise an error`() {
    rule.assertInvalidProject(
      "private_by_annotation", "Settler selected by " +
          "@com.joom.colonist.processor.analysis.settlerdiscoverer.private_by_annotation.TestSettler should be a " +
          "public class [com.joom.colonist.processor.analysis.settlerdiscoverer.private_by_annotation.PrivateSettler]"
    )
  }

  @Test
  fun `private settlers selected by super type raise an error`() {
    rule.assertInvalidProject(
      "private_by_supertype", "Settler selected by " +
          "com.joom.colonist.processor.analysis.settlerdiscoverer.private_by_supertype.TestSettler should be a " +
          "public class [com.joom.colonist.processor.analysis.settlerdiscoverer.private_by_supertype.PrivateSettler]"
    )
  }

  @Test
  fun `abstract settlers selected by annotation and produced via constructor do not raise an error`() {
    rule.assertValidProject("abstract_by_annotation")
  }

  @Test
  fun `settlers selected by annotation without no arg constructor produced via constructor raise an error`() {
    rule.assertInvalidProject(
      "non_instantiable_by_annotation", "Settler selected by " +
          "@com.joom.colonist.processor.analysis.settlerdiscoverer.non_instantiable_by_annotation.TestSettler and produced via constructor " +
          "does not have public default constructor " +
          "[com.joom.colonist.processor.analysis.settlerdiscoverer.non_instantiable_by_annotation.PrivateConstructorSettler]"
    )
  }

  @Test
  fun `abstract settlers selected by super type and produced via constructor do not raise an error`() {
    rule.assertValidProject("abstract_by_supertype")
  }

  @Test
  fun `settlers selected by super type without no arg constructor produced via constructor raise an error`() {
    rule.assertInvalidProject(
      "non_instantiable_by_supertype", "Settler selected by " +
          "com.joom.colonist.processor.analysis.settlerdiscoverer.non_instantiable_by_supertype.TestSettler and produced via constructor " +
          "does not have public default constructor " +
          "[com.joom.colonist.processor.analysis.settlerdiscoverer.non_instantiable_by_supertype.PrivateConstructorSettler]"
    )
  }

  @Test
  fun `does not return abstract settlers selected by super type and produced via constructor`() {
    val settlers = discoverSettlers("abstract_by_supertype", SelectorDescription.SuperType("TestSettler"), SettlerProducer.Constructor)

    settlers.shouldHaveSingleElement { it.type.className.endsWith("ConcreteSettler") }
  }

  @Test
  fun `returns all settlers selected by super type and produced as classes`() {
    val settlers = discoverSettlers("abstract_by_supertype", SelectorDescription.SuperType("TestSettler"), SettlerProducer.Class)

    settlers.shouldHaveSize(4)
    settlers.shouldExist { it.type.className.endsWith("TestSettler") }
    settlers.shouldExist { it.type.className.endsWith("AbstractSettler") }
    settlers.shouldExist { it.type.className.endsWith("InterfaceSettler") }
    settlers.shouldExist { it.type.className.endsWith("ConcreteSettler") }
  }

  @Test
  fun `returns all settlers selected by super type and produced as callback`() {
    val settlers = discoverSettlers("abstract_by_supertype", SelectorDescription.SuperType("TestSettler"), SettlerProducer.Callback)

    settlers.shouldHaveSize(4)
    settlers.shouldExist { it.type.className.endsWith("TestSettler") }
    settlers.shouldExist { it.type.className.endsWith("AbstractSettler") }
    settlers.shouldExist { it.type.className.endsWith("InterfaceSettler") }
    settlers.shouldExist { it.type.className.endsWith("ConcreteSettler") }
  }

  @Test
  fun `does not return abstract settlers selected by annotation and produced via constructor`() {
    val settlers = discoverSettlers("abstract_by_annotation", SelectorDescription.Annotation("TestSettler"), SettlerProducer.Constructor)

    settlers.shouldHaveSingleElement { it.type.className.endsWith("ConcreteSettler") }
  }

  @Test
  fun `returns all settlers selected by annotation and produced as classes`() {
    val settlers = discoverSettlers("abstract_by_annotation", SelectorDescription.Annotation("TestSettler"), SettlerProducer.Class)

    settlers.shouldHaveSize(3)
    settlers.shouldExist { it.type.className.endsWith("AbstractSettler") }
    settlers.shouldExist { it.type.className.endsWith("InterfaceSettler") }
    settlers.shouldExist { it.type.className.endsWith("ConcreteSettler") }
  }

  @Test
  fun `returns all settlers selected by annotation and produced as callback`() {
    val settlers = discoverSettlers("abstract_by_annotation", SelectorDescription.Annotation("TestSettler"), SettlerProducer.Callback)

    settlers.shouldHaveSize(3)
    settlers.shouldExist { it.type.className.endsWith("AbstractSettler") }
    settlers.shouldExist { it.type.className.endsWith("InterfaceSettler") }
    settlers.shouldExist { it.type.className.endsWith("ConcreteSettler") }
  }

  private fun discoverSettlers(sourceCodeDir: String, selectorDescription: SelectorDescription, producer: SettlerProducer): Collection<Settler> {
    val reporter = ErrorReporter()
    val (discoverer, resolver) = createDiscovererWithResolver(sourceCodeDir, reporter)
    reporter.shouldNotHaveErrors()
    val selector = when (selectorDescription) {
      is SelectorDescription.SuperType -> SettlerSelector.SuperType(resolver.getType(selectorDescription.className))
      is SelectorDescription.Annotation -> SettlerSelector.Annotation(resolver.getType(selectorDescription.className))
    }

    return discoverer.discoverSettlers(selector, producer)
  }

  private fun createDiscovererWithResolver(sourceCodeDir: String, errorReporter: ErrorReporter): Pair<SettlerDiscoverer, ClassResolver> {
    val path = rule.compileProject(sourceCodeDir).normalize()
    val grip = GripFactory.INSTANCE.create(listOf(path) + JvmRuntimeUtil.computeRuntimeClasspath())
    val settlerParser = SettlerParserImpl(grip, SettlerProducerParserImpl, SettlerAcceptorParserImpl)

    val discoverer = SettlerDiscovererImpl(grip, inputs = listOf(path), settlerParser = settlerParser, errorReporter = errorReporter)
    val resolver = ClassResolver(path, grip)

    return discoverer to resolver
  }

  private sealed class SelectorDescription {
    class SuperType(val className: String) : SelectorDescription()
    class Annotation(val className: String) : SelectorDescription()
  }

  private class ClassResolver(private val path: Path, private val grip: Grip) {
    fun getType(name: String): Type.Object {
      val fullClassName = "$PACKAGE.${path.last().name}.$name"

      return grip.fileRegistry.findTypesForPath(path).find { it.className == fullClassName } ?: error("Failed to find class $name in $path")
    }
  }

  private companion object {
    private const val PACKAGE = "com.joom.colonist.processor.analysis.settlerdiscoverer"
  }
}
