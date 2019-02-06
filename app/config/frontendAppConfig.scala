/*
 * Copyright 2019 HM Revenue & Customs
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

package config

import java.util.Base64

import controllers.reg.routes
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class FrontendAppConfigImpl @Inject()(val environment:Environment, val runModeConfiguration: Configuration) extends FrontendAppConfig {
  override protected def mode: Mode = environment.mode
}

trait FrontendAppConfig extends ServicesConfig {
  private def loadConfig(key: String)  = getString(key)

  private def loadOptionalConfig(key: String) = { try {
    Option(getString(key))
  } catch {
    case _ : Throwable => None
    }
  }
    private lazy val contactFormServiceIdentifier = "SCRS"

    lazy val assetsPrefix = loadConfig(s"assets.url") + loadConfig(s"assets.version")
    lazy val analyticsToken = loadConfig(s"google-analytics.token")
    lazy val analyticsHost = loadConfig(s"google-analytics.host")
    lazy val piwikURL = loadOptionalConfig(s"piwik.url")
    lazy val analyticsAutoLink = loadConfig(s"google-analytics.autolink")
    lazy val reportAProblemPartialUrl = s"/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
    lazy val reportAProblemNonJSUrl = s"/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
    lazy val govHostUrl = loadConfig(s"microservice.services.gov-uk.gov-host-domain")

    lazy val contactFrontendPartialBaseUrl = baseUrl("contact-frontend")
    lazy val serviceId = contactFormServiceIdentifier

    lazy val corsRenewHost = loadOptionalConfig("cors-host.renew-session")

    lazy val timeoutInSeconds: String = loadConfig("microservice.timeoutInSeconds")
    lazy val timeoutDisplayLength: String = loadConfig("microservice.timeoutDisplayLength")

  private lazy val IR_CT           = "IR-CT"
  private lazy val IR_PAYE         = "IR-PAYE"
  private lazy val HMCE_VATDEC_ORG = "HMCE-VATDEC-ORG"
  private lazy val HMCE_VATVAR_ORG = "HMRC-VATVAR-ORG"
  private lazy val IR_SA_PART_ORG  = "IR-SA-PART-ORG"
  private lazy val IR_SA_TRUST_ORG = "IR-SA-TRUST-ORG"
  private lazy val IR_SA           = "IR-SA"

  lazy val SAEnrolments         = List(IR_SA_PART_ORG, IR_SA_TRUST_ORG, IR_SA)
  lazy val restrictedEnrolments = List(IR_CT, IR_PAYE, HMCE_VATDEC_ORG, HMCE_VATVAR_ORG) ++ SAEnrolments


  lazy val self = getConfString("comp-reg-frontend.url", throw new Exception("Could not find config for comp-reg-frontend url"))
  lazy val selfFull = getConfString("comp-reg-frontend.fullurl", self)
  lazy val selfFullLegacy = getConfString("comp-reg-frontend.legacyfullurl", selfFull)

  lazy val incorporationInfoUrl = baseUrl("incorp-info")

  lazy val companyAuthHost = try{
    getString(s"microservice.services.auth.company-auth.url")
  } catch {case _ : Throwable => ""}
  lazy val loginCallback = try { getString(s"microservice.services.auth.login-callback.url")}
  catch {case _ : Throwable => ""}
  lazy val loginPath = try {getString(s"microservice.services.auth.login_path")
  } catch {case _ : Throwable => ""}
  lazy val logoutPath = try {getString(s"microservice.services.auth.logout_path")
  } catch {case _ : Throwable => ""}

  lazy val loginURL = s"$companyAuthHost$loginPath"
  lazy val logoutURL = s"$companyAuthHost$logoutPath"
  def continueURL(handOffID: Option[String], payload: Option[String]) = s"$loginCallback${routes.SignInOutController.postSignIn(None, handOffID, payload).url}"
}