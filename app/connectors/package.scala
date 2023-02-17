/*
 * Copyright 2023 HM Revenue & Customs
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

import models.external.OtherRegStatus

import scala.util.control.NoStackTrace

package connectors {
  sealed trait CancellationResponse
  case object Cancelled extends CancellationResponse
  case object NotCancelled extends CancellationResponse

  sealed trait cantCancelT extends Throwable with NoStackTrace
  case object cantCancel extends cantCancelT

  sealed trait StatusResponse
  case object NotStarted extends StatusResponse
  case object ErrorResponse extends StatusResponse
  case class SuccessfulResponse(status: OtherRegStatus) extends StatusResponse
}