package us.dot.its.jpo.ode.vsdm;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;

@Controller
public class VsdmServicesController {
	
	private static Logger logger = LoggerFactory.getLogger(VsdmServicesController.class);
	
	@Autowired
	public VsdmServicesController(OdeProperties odeProps) {
		super();
		
		logger.info("Starting {} ...", this.getClass().getSimpleName());

        VsdDepositor vsdDepositor = new VsdDepositor(odeProps);
        logger.info("Launching {} ...", vsdDepositor.getClass().getSimpleName());
        /* 
         * TODO ODE-314
         * Will be changed to MessageConsumer.defaultStringMessageConsumer() method 
         */
        MessageConsumer<String, byte[]> consumer = 
                MessageConsumer.defaultByteArrayMessageConsumer(
                        odeProps.getKafkaBrokers(), 
                        odeProps.getHostId() + vsdDepositor.getClass().getSimpleName(),
                        vsdDepositor);

		Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                /* 
                 * TODO ODE-314
                 * The argument to subscribe method will be changed to 
                 * odeProps.getKafkaTopicFilteredBsmJson()
                 */
                consumer.subscribe(odeProps.getKafkaTopicVsd());
            }
        });
        
		VsdmReceiver vsdReceiver = new VsdmReceiver(odeProps);
        logger.info("Launching {} ...", vsdReceiver.getClass().getSimpleName());
		Executors.newSingleThreadExecutor().submit(vsdReceiver);
		
	}

}
