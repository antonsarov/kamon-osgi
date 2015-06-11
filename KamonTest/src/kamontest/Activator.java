package kamontest;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import kamon.Kamon;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import scala.concurrent.duration.Duration;


public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		
		Thread.currentThread().setContextClassLoader(Kamon.class.getClassLoader());
		
		// print the system arguments to see if the -javaagent has been passed correctly
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		System.out.println(arguments);
		
		Activator.context = bundleContext;
		Config parsedConfigFile = MyOwnConfigFactory.parseFile(new File("/somepath/to/conf/application.conf"), true, null);
		Config defaultReference = // load a default reference config
		Kamon.start(ConfigFactory.defaultReference(Kamon.class.getClassLoader()).withFallback(defaultReference));
		
		ActorSystem actorSystem = ActorSystem.create("kamonSystem", parsedConfigFile, Kamon.class.getClassLoader());
		final ActorRef myActor = actorSystem.actorOf(Props.create(KActor.class));
		final ActorRef mainActor1 = actorSystem.actorOf(Props.create(KActor.class), "main1");
		final ActorRef mainActor2 = actorSystem.actorOf(Props.create(KActor.class), "main2");
		mainActor1.tell(new Create(), null);
		mainActor2.tell(new Create(), null);
		mainActor2.tell(new Create(), null);
		
		actorSystem.scheduler().schedule(
		        Duration.create(0, "millis"), //Initial delay 0 milliseconds
		        Duration.create(1000, "millis"),     //Frequency 1 second
		        // receiver
		        myActor,
		        // message
		        "Test",
		        // executor
		        actorSystem.dispatcher(),
		        // sender
		        null
		);
				
		//Kamon.metrics().subscribe("counter", "**", myActor);
	    //Kamon.metrics().subscribe("histogram", "test-histogram", myActor);
	    Kamon.metrics().subscribe("**", "**", myActor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		Kamon.shutdown();
	}
}
