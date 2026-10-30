package com.bank.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private McpSyncServer mcpSyncServer;

	@Test
	void contextLoads() {
	}

	@Test
	void registersReadOnlyBudgetToolsWithMcpServer() {
		var toolNames = mcpSyncServer.listTools().stream()
				.map(tool -> tool.name())
				.toList();

		assertTrue(toolNames.contains("get_limits"), toolNames.toString());
		assertTrue(toolNames.contains("get_exceeded_transactions"), toolNames.toString());
	}

}
