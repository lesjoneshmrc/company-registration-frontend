/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors.{CompanyRegistrationConnector, IncorpInfoConnector, KeystoreConnector}
import javax.inject.{Inject, Singleton}
import models.{GroupUTR, Groups}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SCRSExceptions

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupService @Inject()(val keystoreConnector: KeystoreConnector,
                             val compRegConnector: CompanyRegistrationConnector,
                             val incorpInfoConnector: IncorpInfoConnector
                            )(implicit ec: ExecutionContext)
  extends CommonService with SCRSExceptions {

  def updateGroupUtr(groupUtr: GroupUTR, groups: Groups, registrationID: String)(implicit hc: HeaderCarrier): Future[Groups] = {
    compRegConnector.updateGroups(registrationID, groups.copy(groupUTR = Some(groupUtr)))
  }

  def retrieveGroups(registrationID: String)(implicit hc: HeaderCarrier): Future[Option[Groups]] = {
    compRegConnector.getGroups(registrationID)
  }
}
