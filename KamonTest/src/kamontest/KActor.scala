package kamontest

import kamon.Kamon
import kamon.trace.TraceInfo
import kamon.metric.SubscriptionsDispatcher.TickMetricSnapshot
import akka.actor._

class KActor extends Actor {

  implicit val system = context.system
  val counter = Kamon.metrics.registerCounter("test-counter")
  
  import akka.actor.OneForOneStrategy
    import akka.actor.SupervisorStrategy._
    import scala.concurrent.duration._

    override val supervisorStrategy =
      AllForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
        case _: ArithmeticException      => Escalate
        case _: Exception                => Escalate
      }


  def receive: Actor.Receive = {
    case x: String => {
      counter.increment()
      println(self.path.name + x)
      context.actorSelection("akka://kamonSystem/user/main1/child1") !""
    }
    
    case x: Create => {
      println("spawning a child")
      context.actorOf(Props[Child], "child"+(context.children.size+1));
    }

    case tickSnapshot: TickMetricSnapshot => {
      val counters = tickSnapshot.metrics.filterKeys(_.category == "counter")
    	val histograms = tickSnapshot.metrics.filterKeys(_.category == "histogram")
    	val traces = tickSnapshot.metrics.filterKeys(_.category == "trace")
//      println("Got [%d] counters".format(counters.size))
//      println("Got [%d] histograms".format(histograms.size))
//      println("Got [%d] traces".format(traces.size))
//
//      println("#################################################")
//      println("From: " + tickSnapshot.from)
//      println("To: " + tickSnapshot.to)
//      println("Metrics: " + tickSnapshot.metrics)

      counters.foreach {
        case (e, s) =>
          val counterSnapshot = s.counter("counter").get
          println("Counter [%s] was incremented [%d] times.".format(e.name, counterSnapshot.count))
      }

      histograms.foreach {
        case (e, s) =>
          val histogramSnapshot = s.histogram("histogram").get
          println("Histogram [%s] has [%d] recordings.".format(e.name, histogramSnapshot.numberOfMeasurements))
      }
      
      traces.foreach {
        case (e, s) =>
          val traceSnapshot = s.minMaxCounter("mailbox-size").get
          println("Trace [%s] has [%d] recordings.".format(e.name, traceSnapshot.numberOfMeasurements))
      }
    }
  }
}