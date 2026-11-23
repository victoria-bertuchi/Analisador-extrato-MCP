package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/**
 * Teste de integração: sobe o servidor MCP (o .jar gerado pelo build) e executa
 * a ferramenta resumo_do_mes, que não usa IA, como um cliente MCP faria.
 */
class McpServer01IT {
	private static final Logger LOG = LoggerFactory.getLogger(McpServer01IT.class);
	// nome e caminho para o servidor MCP
	private static final String SERVER_PATH = "server/mcp_server01_server.jar";
	private static final String CSV_EXEMPLO = "src/test/resources/extratos/fatura_nubank.csv";

	private static McpSyncClient client;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		// antes que a classe de testes seja carregada, este método providencia
		// alguma inicialização ou alocação de recursos, neste caso, o servidor
		// MCP é executado e o cliente se conecta a ele
		File serverJar = new File(SERVER_PATH);
		assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " + serverJar.getAbsolutePath());

		String jarPath = serverJar.getAbsolutePath();
		ServerParameters params = ServerParameters.builder("java").args("-jar", jarPath).build();
		JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
		StdioClientTransport transport = new StdioClientTransport(params, jsonMapper);

		transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));

		client = McpClient.sync(transport).build();
		client.initialize();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		// ao terminar todos os testes, todos os métodos nesta classe foram
		// executados, o cliente se desconecta do servidor
		if (client != null)
			client.closeGracefully();
	}

	private String texto(CallToolResult resultado) {
		return ((TextContent) resultado.content().get(0)).text();
	}

	private CallToolResult executar(String ferramenta, Map<String, Object> argumentos) {
		return client.callTool(CallToolRequest.builder(ferramenta).arguments(argumentos).build());
	}

	// ==========================================================================
	// Os métodos de teste à seguir simulam a aplicação cliente:
	// - há ferramentas (tools) disponíveis???
	// - as ferramentas do analisador existem, com descrição e esquema???
	// - a ferramenta 'resumo_do_mes' executa com sucesso e devolve erros claros???
	// ==========================================================================

	@Test
	void deveDisponibilizarFerramentas() {
		ListToolsResult result = client.listTools();

		assertNotNull(result, "O servidor não retornou o resultado.");
		assertNotNull(result.tools(), "A lista de ferramentas não pode ser null.");
		assertFalse(result.tools().isEmpty(), "O servidor deveria disponibilizar" + " pelo menos uma ferramenta.");
	}

	@Test
	void deveDisponibilizarAsFerramentasDoAnalisador() {
		List<Tool> tools = client.listTools().tools();
		List<String> nomes = tools.stream().map(Tool::name).toList();

		assertTrue(nomes.contains("resumo_do_mes"), "resumo_do_mes não encontrada.");
		assertTrue(nomes.contains("gastos_por_categoria"), "gastos_por_categoria não encontrada.");
		for (Tool tool : tools) {
			assertNotNull(tool.description(), "A ferramenta " + tool.name() + " deveria possuir descrição.");
			assertNotNull(tool.inputSchema(), "A ferramenta " + tool.name() + " deveria possuir 'inputSchema'.");
		}
	}

	@Test
	void deveExecutarResumoDoMes() {
		CallToolResult result = executar("resumo_do_mes", Map.of("arquivo", new File(CSV_EXEMPLO).getAbsolutePath()));

		assertFalse(result.isError(), texto(result));
		assertTrue(texto(result).contains("Total gasto: R$ 104,30 em 3 compras"));
		assertTrue(texto(result).contains("Média por dia: R$ 26,08"));
	}

	@Test
	void deveExecutarResumoDoMesPeloConteudo() {
		CallToolResult result = executar("resumo_do_mes",
				Map.of("conteudo", "date,title,amount\n2026-11-02,Uber *Trip,18.50\n"));

		assertFalse(result.isError(), texto(result));
		assertTrue(texto(result).contains("Total gasto: R$ 18,50 em 1 compra"));
	}

	@Test
	void deveDevolverErroSemParametros() {
		CallToolResult result = executar("resumo_do_mes", Map.of());

		assertTrue(result.isError());
		assertTrue(texto(result).contains("Informe o caminho"));
	}

	@Test
	void deveDevolverErroParaArquivoInexistente() {
		CallToolResult result = executar("resumo_do_mes", Map.of("arquivo", "nao_existe.csv"));

		assertTrue(result.isError());
		assertTrue(texto(result).contains("Arquivo não encontrado"));
	}
}