package com.c8.examples;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.c8.examples")
@Deployment(
		resources = {
				"modeler/chatbot.bpmn",
				"modeler/banking-support-agent.bpmn",
				"modeler/Loan Application.bpmn",
				"modeler/Loan Support Agent.bpmn",
				"modeler/Policy.dmn",
				"modeler/fakeSynchronousProcess.bpmn"
		})
public class ExamplesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamplesApplication.class, args);
	}

}
