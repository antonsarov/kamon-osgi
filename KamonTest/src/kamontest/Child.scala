package kamontest

import akka.actor.Actor
import scala.util.Random

class Child extends Actor {
  
  println(context.self.path)
  
  def receive: Actor.Receive = {
    case _ => {
      println("received something")
      try {
        val i = 3///Random.nextInt(3)
      } catch {
        case t: Throwable => throw t
      }
    }
  }
}