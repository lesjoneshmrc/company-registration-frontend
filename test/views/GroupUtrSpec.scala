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

package views

import _root_.helpers.SCRSSpec
import builders.AuthBuilder
import controllers.groups.GroupUtrController
import fixtures.UserDetailsFixture
import models.{NewAddress, _}
import org.jsoup.Jsoup
import org.mockito.{ArgumentMatcher, Matchers}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.GroupPageEnum
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class GroupUtrSpec extends SCRSSpec with UserDetailsFixture
  with WithFakeApplication with AuthBuilder {

  class Setup {
    implicit val r = FakeRequest()
    val controller = new GroupUtrController {
      override val authConnector = mockAuthConnector
      override val groupService = mockGroupService
      override val compRegConnector = mockCompanyRegistrationConnector
      override val keystoreConnector= mockKeystoreConnector
      override val appConfig = mockAppConfig
      override val messagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      val theFunction = showFunc(_:Groups)(_:Request[_])
    }

    case class funcMatcher(func: Groups => Future[Result]) extends ArgumentMatcher[Groups => Future[Result]] {
      override def matches(oarg :scala.Any): Boolean = true
      }


    val ctDocFirstTimeThrough =
    Json.parse(
      s"""
         |{
         |    "OID" : "123456789",
         |    "registrationID" : "1",
         |    "status" : "draft",
         |    "formCreationTimestamp" : "2016-10-25T12:20:45Z",
         |    "language" : "en",
         |    "verifiedEmail" : {
         |        "address" : "user@test.com",
         |        "type" : "GG",
         |        "link-sent" : true,
         |        "verified" : true,
         |        "return-link-email-sent" : false
         |    }
         |}""".stripMargin)
  }

  "show" should{
    "display the Group UTR page with no UTR pre-popped if no UTR in CR (first time through)" in new Setup {
    val testGroups = Groups(true,Some(GroupCompanyName("testGroupCompanyname1", "type")),
                 Some(GroupsAddressAndType("type", NewAddress("l1", "l2", None, None, None, None, None))),
                 None)
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      val mockOfFunc = (g:Groups) => Future.successful(Results.Ok(""))
      CTRegistrationConnectorMocks.retrieveCTRegistration(ctDocFirstTimeThrough)
      when(mockCompanyRegistrationConnector.retrieveEmail(any())(any())).thenReturn(Future.successful(Some(Email("verified@email","GG",true,true,true))))
      when(mockGroupService.retrieveGroups(any())(any())).thenReturn(Future.successful(Some(testGroups)))
      val res:Future[Result] = Future.successful(await(controller.theFunction(testGroups,r)))
      when(mockGroupService.groupsUserSkippedPage(any[Option[Groups]](),any[GroupPageEnum.Value]())(Matchers.argThat(funcMatcher(mockOfFunc)))).thenReturn(res)

      showWithAuthorisedUser(controller.show) {
        result =>
          val document = Jsoup.parse(contentAsString(result))

          document.title() shouldBe "Do you know testGroupCompanyname1's Unique Taxpayer Reference (UTR)?"
          document.getElementById("main-heading").text() shouldBe "Do you know testGroupCompanyname1's Unique Taxpayer Reference (UTR)?"
          document.getElementById("utr").attr("value") shouldBe ""
      }
    }

    "display the Group UTR page with the UTR pre-popped if a UTR has already been saved in CR (second time through)" in new Setup {
    val testGroups = Groups(true,Some(GroupCompanyName("testGroupCompanyname1", "type")),
                 Some(GroupsAddressAndType("type", NewAddress("l1", "l2", None, None, None, None, None))),
                 Some(GroupUTR(Some("1234567890"))))
      mockKeystoreFetchAndGet("registrationID", Some("1"))
      val mockOfFunc = (g:Groups) => Future.successful(Results.Ok(""))
      CTRegistrationConnectorMocks.retrieveCTRegistration(ctDocFirstTimeThrough)
      when(mockCompanyRegistrationConnector.retrieveEmail(any())(any())).thenReturn(Future.successful(Some(Email("verified@email","GG",true,true,true))))
      when(mockGroupService.retrieveGroups(any())(any())).thenReturn(Future.successful(Some(testGroups)))
      val res:Future[Result] = Future.successful(await(controller.theFunction(testGroups,r)))
      when(mockGroupService.groupsUserSkippedPage(any[Option[Groups]](),any[GroupPageEnum.Value]())(Matchers.argThat(funcMatcher(mockOfFunc)))).thenReturn(res)

      showWithAuthorisedUser(controller.show) {
        result =>
          val document = Jsoup.parse(contentAsString(result))

          document.title() shouldBe "Do you know testGroupCompanyname1's Unique Taxpayer Reference (UTR)?"
          document.getElementById("main-heading").text() shouldBe "Do you know testGroupCompanyname1's Unique Taxpayer Reference (UTR)?"
          document.getElementById("utr").attr("value") shouldBe "1234567890"
      }
    }
  }
}