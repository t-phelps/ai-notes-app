package com.tphelps.backend;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class NotesApp {

    private static final Logger logger = LoggerFactory.getLogger(NotesApp.class);
	public static void main(String[] args) {

        SpringApplication.run(NotesApp.class, args);
	}

    @PostConstruct
    public void startRclone(){
        try{
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "rclone", "rcd",
                    "--rc-no-auth",
                    "--rc-addr", "127.0.0.1:5572"
            );
            processBuilder.redirectOutput(new File("/tmp/rclone.log"));
            processBuilder.redirectErrorStream(true);
            processBuilder.start();
            logger.info("RCLone started");
        }catch(IOException e){
            logger.error("Failed to start rclone on app startup: {}", e.getMessage());
        }
    }

}
