/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.scio

import com.google.cloud.spanner.{Mutation, Struct}
import com.spotify.scio.coders.Coder
import com.spotify.scio.io.Tap
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.io.gcp.spanner.SpannerConfig
import org.apache.beam.sdk.io.gcp.spanner.SpannerIO.FailureMode

import scala.concurrent.Future

package object spanner {

  implicit class SpannerScioContext(@transient private val self: ScioContext) extends AnyVal {
    import SpannerRead.ReadParam._

    def spannerTable(spannerConfig: SpannerConfig,
                     table: String,
                     columns: Seq[String],
                     withBatching: Boolean = DefaultWithBatching,
                     withTransaction: Boolean = DefaultWithTransaction): SCollection[Struct] = {
      val params = SpannerRead.ReadParam(
        SpannerRead.FromTable(table, columns),
        withTransaction,
        withBatching
      )

      self.read(SpannerRead(spannerConfig))(params)
    }

    def spannerQuery(spannerConfig: SpannerConfig,
                     query: String,
                     withBatching: Boolean = DefaultWithBatching,
                     withTransaction: Boolean = DefaultWithTransaction): SCollection[Struct] = {
      val params = SpannerRead.ReadParam(
        SpannerRead.FromQuery(query),
        withTransaction,
        withBatching
      )

      self.read(SpannerRead(spannerConfig))(params)
    }
  }

  implicit class SpannerSCollection(@transient private val self: SCollection[Mutation])
      extends AnyVal {
    import SpannerWrite.WriteParam._

    def saveAsSpanner(spannerConfig: SpannerConfig,
                      failureMode: FailureMode = DefaultFailureMode,
                      batchSizeBytes: Long = DefaultBatchSizeBytes)(
      implicit coder: Coder[Mutation]): Future[Tap[Nothing]] = {
      val params = SpannerWrite.WriteParam(failureMode, batchSizeBytes)

      self
        .asInstanceOf[SCollection[Mutation]]
        .write(SpannerWrite(spannerConfig))(params)
    }
  }

}
