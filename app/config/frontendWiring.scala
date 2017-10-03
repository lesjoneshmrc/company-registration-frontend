/*
 * Copyright 2017 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import play.api.Logger
import play.api.mvc.Call
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

object FrontendAuditConnector extends Auditing with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with RunMode {
  override val hooks = NoneRequired
}

object WSHttpProxy extends WSHttp with WSProxy with RunMode with HttpAuditing with ServicesConfig {
  override val appName = getString("appName")
  override val wsProxyServer = WSProxyConfiguration(s"proxy")
  override val hooks = Seq(AuditingHook)
  override val auditConnector = FrontendAuditConnector
}

object WSHttpWithAudit extends WSGet with WSPut with WSPost with WSDelete with AppName with HttpAuditing with RunMode{
  override val hooks = Seq(AuditingHook)
  override val auditConnector = FrontendAuditConnector
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
  override val httpGet = WSHttp
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  override val serviceUrl = baseUrl("auth")
  override lazy val http = WSHttp
}

object SCRSShortLivedHttpCaching extends ShortLivedHttpCaching with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.short-lived-cache")
  override lazy val domain = getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

object SCRSShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = ApplicationCrypto.JsonCrypto
  override lazy val shortLiveCache = SCRSShortLivedHttpCaching
}

object SCRSSessionCache extends SessionCache with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain",
    throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

object WhitelistFilter extends AkamaiWhitelistFilter
  with RunMode {

  implicit val system = ActorSystem("crf")
  implicit def mat: Materializer = ActorMaterializer()

  override def whitelist: Seq[String] = FrontendAppConfig.whitelist


  override def excludedPaths: Seq[Call] = {
    FrontendAppConfig.whitelistExcluded.map { path =>
      Call("GET", path)
    }
  }

  override def destination: Call = Call("GET", "https://www.tax.service.gov.uk/outage-register-your-company")
}