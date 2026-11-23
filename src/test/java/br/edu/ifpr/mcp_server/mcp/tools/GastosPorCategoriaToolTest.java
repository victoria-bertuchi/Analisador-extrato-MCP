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

import br.edu.ifpr.mcp_server.extrato.CalculadoraGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.Categoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorDeCategoria;
import br.edu.ifpr.mcp_server.extrato.FormatadorGastosPorCategoria;
import br.edu.ifpr.mcp_server.mcp.utils.ResolvedorDeArquivoDoExtrato;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Testa a ferramenta com um classificador falso no lugar da IA (injeção de
 * dependência).
 */
class GastosPorCategoriaToolTest {

	private static final String CSV = "date,title,amount\n2026-11-01,Sushi,45.90\n2026-11-02,Uber,18.50\n";

	@TempDir
	Path pasta;

	private GastosPorCategoriaTool ferramenta(ClassificadorDeCategoria classificador) {
		return new GastosPorCategoriaTool(new ResolvedorDeArquivoDoExtrato(),
				new CalculadoraGastosPorCategoria(classificador), new FormatadorGastosPorCategoria());
	}

	// classificador falso: tudo vira TRANSPORTE
	private GastosPorCategoriaTool ferramentaComClassificadorFalso() {
		return ferramenta(transacao -> Categoria.TRANSPORTE);
	}

	private CallToolResult executar(GastosPorCategoriaTool ferramenta, Map<String, Object> argumentos) {
		// o "exchange" não é usado por esta ferramenta, então pode ser null
		return ferramenta.run(null, CallToolRequest.builder("gastos_por_categoria").arguments(argumentos).build());
	}

	private String texto(CallToolResult resultado) {
		return ((TextContent) resultado.content().get(0)).text();
	}

	private Path criar(String nome, String conteudo) throws IOException {
		return Files.writeString(pasta.resolve(nome), conteudo);
	}

	@Test
	@DisplayName("Pelo caminho do arquivo: soma por categoria")
	void sucessoPeloCaminho() throws IOException {
		Path csv = criar("extrato.csv", CSV);

		CallToolResult resultado = executar(ferramentaComClassificadorFalso(), Map.of("arquivo", csv.toString()));

		assertFalse(resultado.isError(), texto(resultado));
		assertEquals("Gastos por categoria:\n- TRANSPORTE: R$ 64,40\nTotal: R$ 64,40", texto(resultado));
	}

	@Test
	@DisplayName("Pelo conteúdo do CSV: soma por categoria")
	void sucessoPeloConteudo() {
		CallToolResult resultado = executar(ferramentaComClassificadorFalso(), Map.of("conteudo", CSV));

		assertFalse(resultado.isError(), texto(resultado));
		assertTrue(texto(resultado).contains("Total: R$ 64,40"));
	}

	@Test
	@DisplayName("Extrato só com entradas: mensagem de sem gastos, sem erro")
	void semGastos() throws IOException {
		Path csv = criar("extrato.csv", "date,title,amount\n2026-11-01,Pagamento recebido,-200.00\n");

		CallToolResult resultado = executar(ferramentaComClassificadorFalso(), Map.of("arquivo", csv.toString()));

		assertFalse(resultado.isError());
		assertEquals("Nenhum gasto encontrado no extrato.", texto(resultado));
	}

	@Test
	@DisplayName("Sem parâmetros: erro")
	void semParametros() {
		CallToolResult resultado = executar(ferramentaComClassificadorFalso(), Map.of());

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("Informe o caminho"));
	}

	@Test
	@DisplayName("Arquivo inexistente: erro")
	void arquivoInexistente() {
		CallToolResult resultado = executar(ferramentaComClassificadorFalso(),
				Map.of("arquivo", pasta.resolve("nao_existe.csv").toString()));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("Arquivo não encontrado"));
	}

	@Test
	@DisplayName("CSV sem coluna de valor: erro de formato")
	void formatoInvalido() throws IOException {
		Path csv = criar("extrato.csv", "Data;Descrição\n21/11/2026;UBER\n");

		CallToolResult resultado = executar(ferramentaComClassificadorFalso(), Map.of("arquivo", csv.toString()));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("formato de extrato reconhecido"));
	}

	@Test
	@DisplayName("IA fora do ar: erro explicando")
	void erroDaIa() {
		GastosPorCategoriaTool ferramenta = ferramenta(transacao -> {
			throw new IllegalStateException("sem internet");
		});

		CallToolResult resultado = executar(ferramenta, Map.of("conteudo", CSV));

		assertTrue(resultado.isError());
		assertTrue(texto(resultado).contains("A IA não conseguiu classificar"));
		assertTrue(texto(resultado).contains("sem internet"));
	}
}