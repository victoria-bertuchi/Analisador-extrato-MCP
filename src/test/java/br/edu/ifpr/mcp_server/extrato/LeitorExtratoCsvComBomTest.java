package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testa CSV salvo pelo Excel como "CSV UTF-8", que começa com o BOM (3 bytes
 * invisíveis).
 */
class LeitorExtratoCsvComBomTest {

	private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	@TempDir
	Path pasta;

	private Path criarComBom(String conteudo) throws IOException {
		byte[] texto = conteudo.getBytes(StandardCharsets.UTF_8);
		byte[] tudo = new byte[BOM.length + texto.length];
		System.arraycopy(BOM, 0, tudo, 0, BOM.length);
		System.arraycopy(texto, 0, tudo, BOM.length, texto.length);

		Path arquivo = pasta.resolve("extrato.csv");
		Files.write(arquivo, tudo);
		return arquivo;
	}

	@Test
	@DisplayName("Leitor genérico ignora o BOM no começo do arquivo")
	void leitorGenericoIgnoraBom() throws IOException {
		Path arquivo = criarComBom("Data;Descrição;Valor\n21/11/2026;Uber;-18,50");

		List<Transacao> transacoes = new LeitorExtratoCsvGenerico().ler(arquivo);

		assertEquals(1, transacoes.size());
		assertEquals(new BigDecimal("18.50"), transacoes.get(0).valor());
	}

	@Test
	@DisplayName("Fatura Nubank com BOM: a factory ainda escolhe o leitor Nubank")
	void factoryReconheceNubankComBom() throws IOException {
		Path arquivo = criarComBom("date,title,amount\n2026-11-01,Sushi,45.90");

		LeitorDeExtrato leitor = LeitorDeExtratoFactory.para(arquivo);

		assertInstanceOf(LeitorExtratoCsvNubank.class, leitor);
		assertEquals(new BigDecimal("45.90"), leitor.ler(arquivo).get(0).valor());
	}
}