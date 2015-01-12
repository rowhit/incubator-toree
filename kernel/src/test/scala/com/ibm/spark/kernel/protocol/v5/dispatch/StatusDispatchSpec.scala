/*
 * Copyright 2014 IBM Corp.
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

package com.ibm.spark.kernel.protocol.v5.dispatch

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content.KernelStatus
import com.ibm.spark.kernel.protocol.v5.kernel.ActorLoader
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpecLike, Matchers}
import play.api.libs.json.Json

import scala.concurrent.duration._

class StatusDispatchSpec extends TestKit(ActorSystem("StatusDispatchSystem"))
with FunSpecLike with Matchers with MockitoSugar with BeforeAndAfter{
  var statusDispatchRef: ActorRef = _
  var relayProbe: TestProbe = _
  before {
    //  Mock the relay with a probe
    relayProbe = TestProbe()
    //  Mock the ActorLoader
    val mockActorLoader: ActorLoader = mock[ActorLoader]
    when(mockActorLoader.load(SystemActorType.KernelMessageRelay))
      .thenReturn(system.actorSelection(relayProbe.ref.path.toString))

    statusDispatchRef = system.actorOf(Props(classOf[StatusDispatch],mockActorLoader))
  }


  describe("StatusDispatch") {
    describe("#receive( KernelStatusType )") {
      it("should send a status message to the relay") {
        statusDispatchRef ! KernelStatusType.Busy
        //  Check the kernel message is the correct type
        val statusMessage: KernelMessage = relayProbe.receiveOne(500.milliseconds).asInstanceOf[KernelMessage]
        statusMessage.header.msg_type should be (MessageType.Outgoing.Status.toString)
        //  Check the status is what we sent
        val status: KernelStatus = Json.parse(statusMessage.contentString).as[KernelStatus]
         status.execution_state should be (KernelStatusType.Busy.toString)
      }
    }

    describe("#receive( KernelStatusType, Header )") {
      it("should send a status message to the relay") {
        val tuple = Tuple2(KernelStatusType.Busy, mock[Header])
        statusDispatchRef ! tuple
        //  Check the kernel message is the correct type
        val statusMessage: KernelMessage = relayProbe.receiveOne(500.milliseconds).asInstanceOf[KernelMessage]
        statusMessage.header.msg_type should be (MessageType.Outgoing.Status.toString)
        //  Check the status is what we sent
        val status: KernelStatus = Json.parse(statusMessage.contentString).as[KernelStatus]
        status.execution_state should be (KernelStatusType.Busy.toString)
      }
    }
  }
}
