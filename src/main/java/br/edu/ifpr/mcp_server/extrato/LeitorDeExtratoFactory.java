package br.edu.ifpr.mcp_server.extrato;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Escolhe o leitor certo para cada arquivo, olhando o cabeçalho (padrão
 * Factory). Quem usa a factory não precisa saber de qual banco é o extrato.
 */
public final class LeitorDeExtratoFactory {

	// cabeçalho da fatura do cartão Nubank, lida pelo LeitorExtratoCsvNubank
	private static final String CABECALHO_NUBANK = "date,title,amount";

	// BOM: caracteres invisíveis que o Excel coloca no começo de um CSV UTF-8.
	// Lido como ISO-8859-1 eles aparecem como "ï»¿"; lido como UTF-8, como \uFEFF
	private static final String BOM_LIDO_COMO_ISO = "\u00EF\u00BB\u00BF";
	private static final String BOM_LIDO_COMO_UTF8 = "\uFEFF";

	// construtor privado, ninguém precisa criar um objeto desta classe, só chamar
	// LeitorDeExtratoFactory.para(arquivo)
	private LeitorDeExtratoFactory() {
	}

	public static LeitorDeExtrato para(Path arquivo) throws IOException {
		// lê só a primeira linha do arquivo
		String cabecalho;
		try (BufferedReader leitor = Files.newBufferedReader(arquivo, StandardCharsets.ISO_8859_1)) {
			String primeiraLinha = leitor.readLine();
			cabecalho = primeiraLinha == null ? "" : normalizarCabecalho(primeiraLinha);
		}

		if (cabecalho.equals(CABECALHO_NUBANK)) {
			return new LeitorExtratoCsvNubank();
		}
		return new LeitorExtratoCsvGenerico();
	}

	private static String normalizarCabecalho(String linha) {
		return linha.replace("\"", "").replace(BOM_LIDO_COMO_ISO, "").replace(BOM_LIDO_COMO_UTF8, "").trim()
				.toLowerCase();
	}
}