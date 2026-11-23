package br.edu.ifpr.mcp_server.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_server.mcp.tools.ResumoDoMesTool;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ResumoDoMesToolTest {

	@TempDir
	Path pasta;

	private final ResumoDoMesTool ferramenta = new ResumoDoMesTool();

	private CallToolResult executar(Map<String, Object> argumentos) {
		// o "exchange" não é usado por esta ferramenta, então pode ser null
		return ferramenta.run(null, CallToolRequest.builder("resumo_do_mes").arguments(argumentos).build());
	}

	private String texto(CallToolResult resultado) {
		return ((TextContent) resultado.content().get(0)).text();
	}

	private Path criar(String nome, String conteudo) throws IOException {
		return Files.writeString(pasta.resolve(nome), conteudo);
	}

	@Test
	@DisplayName("Pelo caminho do arquivo: devolve o resumo")
	void sucessoPeloCaminho() throws IOException {
		Path csv = criar("extrato.csv", "date,title,amount\n2026-11-01,Sushi,45.90\n2026-11-02,Uber,18.50\n");

		CallToolResult resultado = executar(Map.of("arquivo", csv.toString()));

		assertFalse(resultado.isError(), texto(resultado));
		assertTrue(texto(resultado).contains("Total gasto: R$ 64,40 em 2 compras"));
	}

	@Test
	@DisplayName("Pelo conteúdo do CSV: devolve o resumo")
	void sucessoPeloConteudo() {
		CallToolResult resultado = executar(Map.of("conteudo", "date,title,amount\n2026-11-01,Sushi,45.90\n"));

		assertFalse(resultado.isError(), texto(resultado));
		assertTrue(texto(resultado).contains("em 1 compra"));
	}

	@Test
	@DisplayName("Extrato só com entradas: mensagem de sem gastos, sem erro")
	void semGastos() throws IOException {
		Path csv = criar("extrato.csv", "date,title,amount\n2026-11-01,Pagamento recebido,-200.00\n");

		CallToolResult resultado = executar(Map.of("arquivo", csv.toString()));

		assertFalse(resultado.isError());
		assertEquals("Nenhum gasto encontrado no extrato.", texto(resultado));
	}

	@Test
	@DisplayName("Sem parâmetros: erro")
	void semParametros() {
		CallToolResult resultado = executar(Map.of());

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("Informe o caminho"));
	}

	@Test
	@DisplayName("Arquivo inexistente: erro")
	void arquivoInexistente() {
		CallToolResult resultado = executar(Map.of("arquivo", pasta.resolve("nao_existe.csv").toString()));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("Arquivo não encontrado"));
	}

	@Test
	@DisplayName("Arquivo que não é .csv: erro")
	void naoCsv() throws IOException {
		Path txt = criar("extrato.txt", "qualquer coisa");

		CallToolResult resultado = executar(Map.of("arquivo", txt.toString()));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains(".csv"));
	}

	@Test
	@DisplayName("CSV sem coluna de valor: erro de formato")
	void formatoInvalido() throws IOException {
		Path csv = criar("extrato.csv", "Data;Descrição\n21/11/2026;UBER\n");

		CallToolResult resultado = executar(Map.of("arquivo", csv.toString()));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("formato de extrato reconhecido"));
	}
}