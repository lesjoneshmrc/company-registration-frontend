/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.reg

import config.FrontendAppConfig
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

class WelcomeControllerImpl @Inject()(val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents) extends WelcomeController(mcc)

abstract class WelcomeController(mcc: MessagesControllerComponents) extends FrontendController(mcc) with I18nSupport {
  implicit val appConfig: FrontendAppConfig
  val show = Action.async { implicit request =>
    Future.successful(PermanentRedirect(routes.ReturningUserController.show().url))

  }
  val submit = Action.async { implicit request =>
    Future.successful(Redirect(routes.ReturningUserController.show()))
  }
}