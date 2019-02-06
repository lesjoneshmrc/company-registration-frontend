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

package controllers

import java.util.UUID

import builders.AuthBuilder
import config.FrontendAppConfig
import controllers.auth.SCRSExternalUrls
import controllers.reg.SummaryController
import fixtures.{AccountingDetailsFixture, CorporationTaxFixture, SCRSFixtures, TradingDetailsFixtures}
import helpers.SCRSSpec
import models._
import models.handoff._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.NavModelNotFoundException
import sun.misc.resources.Messages_zh_CN
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.JweCommon

import scala.concurrent.Future

class SummaryControllerSpec extends SCRSSpec with SCRSFixtures with WithFakeApplication with AccountingDetailsFixture with TradingDetailsFixtures
  with CorporationTaxFixture with AuthBuilder {

  val aboutYouData = AboutYouChoice("Director")
  val mockNavModelRepoObj = mockNavModelRepo

  val handOffNavModel = HandOffNavModel(
    Sender(
      Map(
        "1" -> NavLinks("testForwardLinkFromSender1", "testReverseLinkFromSender1"),
        "3" -> NavLinks("testForwardLinkFromSender3", "testReverseLinkFromSender3")
      )
    ),
    Receiver(
      Map(
        "0" -> NavLinks("testForwardLinkFromReceiver0", "testReverseLinkFromReceiver0"),
        "2" -> NavLinks("testForwardLinkFromReceiver2", "testReverseLinkFromReceiver2"),
        "4" -> NavLinks("testForwardLinkFromReceiver4", "testReverseLinkFromReceiver4")
      ),
      Map("testJumpKey" -> "testJumpLink"),
      Some(Json.parse("""{"testCHBagKey": "testValue"}""").as[JsObject])
    )
  )

  class Setup {
    val controller = new SummaryController {
      override val s4LConnector = mockS4LConnector
      override val authConnector = mockAuthConnector
      override val compRegConnector = mockCompanyRegistrationConnector
      override val keystoreConnector = mockKeystoreConnector
      override val metaDataService = mockMetaDataService
      override val handOffService = mockHandOffService
      implicit val appConfig: FrontendAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]
      override val jwe: JweCommon = mockJweCommon
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }
  lazy val regID = UUID.randomUUID.toString

  val corporationTaxModel = buildCorporationTaxModel()

  "Sending a GET request to Summary Controller" should {

    "return a 303 and redirect to the sign in page for an unauthorised user" in new Setup {
      showWithUnauthorisedUser(controller.show) {
        result =>
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some(authUrl)
      }
    }

    "return a 200 whilst authorised " in new Setup {
      mockS4LFetchAndGet("HandBackData", Some(validCompanyNameHandBack))

      when(mockMetaDataService.getApplicantData(Matchers.any())(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(aboutYouData))

      mockKeystoreFetchAndGet("registrationID", Some("12345"))

      mockS4LFetchAndGet("CompanyContactDetails", Some(validCompanyContactDetailsModel))
      CTRegistrationConnectorMocks.retrieveCompanyDetails(Some(validCompanyDetailsResponse))
      CTRegistrationConnectorMocks.retrieveTradingDetails(Some(tradingDetailsTrue))
      CTRegistrationConnectorMocks.retrieveContactDetails(CompanyContactDetailsSuccessResponse(validCompanyContactDetailsResponse))
      CTRegistrationConnectorMocks.retrieveAccountingDetails(validAccountingResponse)

      when(mockCompanyRegistrationConnector.retrieveCorporationTaxRegistration(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(corporationTaxModel))

      showWithAuthorisedUser(controller.show) {
        result =>
          status(result) shouldBe OK
      }
    }
  }

  "Post to the Summary Controller" should {
    "return a 303 whilst authorised " in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody()
      submitWithAuthorisedUser(controller.submit, request){
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/register-your-company/incorporation-summary")
      }
    }
  }

  "back" should {
    "redirect to post sign in if no navModel exists" in new Setup {
      when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq("registrationID"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("12354")))
      when(mockHandOffService.fetchNavModel(Matchers.any())(Matchers.any())).thenReturn(Future.failed(new NavModelNotFoundException))
      showWithAuthorisedUserRetrieval(controller.back, Some("extID")) {
        res =>
          status(res) shouldBe SEE_OTHER
          redirectLocation(res) shouldBe Some("/register-your-company/post-sign-in")
      }
    }
    "redirect to the previous stub page" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody()

      when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq("registrationID"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("12354")))
      when(mockJweCommon.encrypt[BackHandoff](Matchers.any[BackHandoff]())(Matchers.any[Writes[BackHandoff]])).thenReturn(Some("foo"))

      when(mockHandOffService.fetchNavModel(Matchers.any())(Matchers.any())).thenReturn(Future.successful(handOffNavModel))

      when(mockHandOffService.buildBackHandOff(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(BackHandoff("EXT-123456", "12354", Json.obj(), Json.obj(), Json.obj())))

      submitWithAuthorisedUserRetrieval(controller.back, request, Some("extID")){
        result =>
          status(result) shouldBe SEE_OTHER
      }
    }
  }

  "summaryBackLink" should {

    "redirect to the specified jump link in the nav model" in new Setup {

      val navModel = HandOffNavModel(
        Sender(
          nav = Map("1" -> NavLinks("testForwardLinkFromSender1", "testReverseLinkFromSender1"))),
        Receiver(
          nav = Map("0" -> NavLinks("testForwardLinkFromReceiver0", "testReverseLinkFromReceiver0")),
          jump = Map("testJumpKey" -> "testJumpLink")
        )
      )
      when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq("registrationID"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("12354")))
      when(mockHandOffService.fetchNavModel(Matchers.any())(Matchers.any())).thenReturn(Future.successful(navModel))
      when(mockJweCommon.encrypt[JsObject](Matchers.any[JsObject]())(Matchers.any[Writes[JsObject]])).thenReturn(Some("foo"))
      showWithAuthorisedUserRetrieval(controller.summaryBackLink("testJumpKey"), Some("extID")) {
        res =>
          status(res) shouldBe SEE_OTHER
      }
    }

    "throw an error when an unkown key is passed" in new Setup {

      val navModel = HandOffNavModel(
        Sender(Map("1" -> NavLinks("testForwardLinkFromSender1", "testReverseLinkFromSender1"))),
        Receiver(Map("0" -> NavLinks("testForwardLinkFromReceiver0", "testReverseLinkFromReceiver0")))
      )
      when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq("registrationID"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("12354")))
      when(mockJweCommon.encrypt[JsObject](Matchers.any[JsObject]())(Matchers.any[Writes[JsObject]])).thenReturn(Some("foo"))
      when(mockHandOffService.fetchNavModel(Matchers.any())(Matchers.any())).thenReturn(Future.successful(handOffNavModel))
      showWithAuthorisedUserRetrieval(controller.summaryBackLink("foo"), Some("extID")) {
        res =>
          val ex = intercept[NoSuchElementException](status(res) shouldBe SEE_OTHER)
          ex.getMessage shouldBe "key not found: foo"
      }
    }
  }
}