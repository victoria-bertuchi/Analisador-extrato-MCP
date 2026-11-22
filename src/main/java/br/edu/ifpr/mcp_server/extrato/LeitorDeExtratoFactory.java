package br.edu.ifpr.mcp_server.extrato;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Escolhe o leitor certo para cada arquivo, olhando o cabeçalho (padrão Factory).
 * Quem usa a factory não precisa saber de qual banco é o extrato.
 */
public final class LeitorDeExtratoFactory {

    //cabeçalho da fatura do cartão Nubank, lida pelo LeitorExtratoCsvNubank
    private static final String CABECALHO_NUBANK = "date,title,amount";

    //construtor privado, ninguém precisa criar um objeto desta classe, só chamar LeitorDeExtratoFactory.para(arquivo)
    private LeitorDeExtratoFactory() {
    }

    public static LeitorDeExtrato para(Path arquivo) throws IOException {
        //lê só a primeira linha do arquivo
        String cabecalho;
        try (BufferedReader leitor = Files.newBufferedReader(arquivo, StandardCharsets.ISO_8859_1)) {
            String primeiraLinha = leitor.readLine();
            cabecalho = primeiraLinha == null ? "" : primeiraLinha.replace("\"", "").trim().toLowerCase();
        }

        if (cabecalho.equals(CABECALHO_NUBANK)) {
            return new LeitorExtratoCsvNubank();
        }
        return new LeitorExtratoCsvGenerico();
    }
}