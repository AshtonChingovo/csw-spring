package com.cosw.councilOfSocialWork;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.SpringVersion;

@Slf4j
@SpringBootApplication
public class CSWApplication {

	public static void main(String[] args) {
		SpringApplication.run(CSWApplication.class, args);

		log.info("Spring version {}", SpringVersion.getVersion());
	}
}
