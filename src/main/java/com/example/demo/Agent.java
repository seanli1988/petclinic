
package com.example.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Agent {
 
//	String SYSTEM_PROMPT = "You are an internal tech support agent of a pet clinic. You will answer question from a customer service agent.";
//	@SystemMessage(SYSTEM_PROMPT)
//	String chat(@UserMessage String message);

	String chat(String userMessage);
}
