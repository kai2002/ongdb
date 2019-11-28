/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.cypher.internal.v4_0.expressions.functions

import org.neo4j.cypher.internal.v4_0.expressions.{FunctionTypeSignature, TypeSignatures}
import org.neo4j.cypher.internal.v4_0.util.symbols._

case object Substring extends Function with TypeSignatures {
  def name = "substring"

  override val signatures = Vector(
    FunctionTypeSignature(functionName = name, names = Vector("original","start"), argumentTypes = Vector(CTString, CTInteger),
      outputType = CTString, description =  "Returns a substring of the original string, beginning with a 0-based index start."),
    FunctionTypeSignature(functionName = name, names = Vector("original","start", "length"), argumentTypes = Vector(CTString, CTInteger, CTInteger),
      outputType = CTString, description = "Returns a substring of length 'length' of the original string, beginning with a 0-based index start.")
  )
}
