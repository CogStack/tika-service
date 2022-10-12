package service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import service.controller.TikaServiceController;


/**
 * The main application
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class TikaServiceApplication {

	private final static Logger logger = LogManager.getLogger(TikaServiceController.class);

	public static void main(String[] args) {
		try {
			var springApplication = new SpringApplication(TikaServiceApplication.class);
			springApplication.run(args);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}