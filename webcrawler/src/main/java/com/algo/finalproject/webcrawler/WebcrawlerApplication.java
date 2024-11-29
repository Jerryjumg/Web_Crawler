package com.algo.finalproject.webcrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpringBootApplication
public class WebcrawlerApplication {
	private static final Logger logger = LogManager.getLogger(WebcrawlerApplication.class);
	public static void main(String[] args) {
		logger.info("Starting Webcrawler Application");
		SpringApplication.run(WebcrawlerApplication.class, args);
	}

}
