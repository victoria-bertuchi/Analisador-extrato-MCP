package br.edu.ifpr.mcp_server.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResolvedorDeArquivoDoExtratoTest {

	@TempDir
	Path pasta;

	private final ResolvedorDeArquivoDoExtrato resolvedor = new ResolvedorDeArquivoDoExtrato();

	private Path criar(String nome, String conteudo) throws IOException {
		return Files.writeString(pasta.resolve(nome), conteudo);
	}

	@Test
	@DisplayName("Argumentos nulos: erro pedindo o arquivo")
	void argumentosNulos() {
		IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> resolvedor.resolver(null));

		assertTrue(erro.getMessage().contains("Informe o caminho"));
	}

	@Test
	@DisplayName("Nenhum parâmetro: erro pedindo o arquivo")
	void semParametros() {
		assertThrows(IllegalArgumentException.class, () -> resolvedor.resolver(Map.of()));
	}

	@Test
	@DisplayName("Parâmetros em branco: erro pedindo o arquivo")
	void parametrosEmBranco() {
		assertThrows(IllegalArgumentException.class,
				() -> resolvedor.resolver(Map.of("arquivo", "  ", "conteudo", " ")));
	}

	@Test
	@DisplayName("Conteúdo do CSV vira um arquivo temporário com o mesmo texto")
	void conteudoViraArquivoTemporario() throws IOException {
		Path arquivo = resolvedor.resolver(Map.of("conteudo", "date,title,amount\n2026-11-01,Uber,18.50"));

		assertEquals("date,title,amount\n2026-11-01,Uber,18.50", Files.readString(arquivo));
	}

	@Test
	@DisplayName("Se vierem os dois, o conteúdo tem prioridade")
	void conteudoTemPrioridade() throws IOException {
		Path csv = criar("extrato.csv", "do arquivo");

		Path arquivo = resolvedor.resolver(Map.of("arquivo", csv.toString(), "conteudo", "do conteudo"));

		assertEquals("do conteudo", Files.readString(arquivo));
	}

	@Test
	@DisplayName("Caminho válido é devolvido")
	void caminhoValido() throws IOException {
		Path csv = criar("extrato.csv", "x");

		assertEquals(csv, resolvedor.resolver(Map.of("arquivo", csv.toString())));
	}

	@Test
	@DisplayName("Caminho entre aspas (Copiar como caminho do Windows) é aceito")
	void caminhoEntreAspas() throws IOException {
		Path csv = criar("extrato.csv", "x");

		assertEquals(csv, resolvedor.resolver(Map.of("arquivo", "\"" + csv + "\"")));
	}

	@Test
	@DisplayName("Arquivo inexistente: erro")
	void arquivoInexistente() {
		IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
				() -> resolvedor.resolver(Map.of("arquivo", pasta.resolve("nao_existe.csv").toString())));

		assertTrue(erro.getMessage().contains("Arquivo não encontrado"));
	}

	@Test
	@DisplayName("Arquivo que não é .csv: erro")
	void naoCsv() throws IOException {
		Path txt = criar("extrato.txt", "x");

		IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
				() -> resolvedor.resolver(Map.of("arquivo", txt.toString())));

		assertTrue(erro.getMessage().contains(".csv"));
	}

	@Test
	@DisplayName("O esquema declara os parâmetros arquivo e conteudo")
	void esquemaDosParametros() {
		Map<String, Object> propriedades = ResolvedorDeArquivoDoExtrato.propriedadesDoEsquema();

		assertTrue(propriedades.containsKey("arquivo"));
		assertTrue(propriedades.containsKey("conteudo"));
	}
}