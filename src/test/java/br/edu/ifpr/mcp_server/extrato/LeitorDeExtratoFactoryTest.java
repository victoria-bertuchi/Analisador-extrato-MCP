package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorDeExtratoFactoryTest {

    @TempDir
    Path pasta;

    private Path criarCsv(String conteudo) throws IOException {
        Path arquivo = pasta.resolve("extrato.csv");
        Files.writeString(arquivo, conteudo);
        return arquivo;
    }

    //o cabeçalho da fatura Nubank escolhe o leitor do Nubank
    @Test
    @DisplayName("Cabeçalho date,title,amount: escolhe o leitor Nubank")
    void escolherLeitorNubank() throws IOException {
        Path arquivo = criarCsv("date,title,amount\n2026-11-01,Uber,18.50");

        assertInstanceOf(LeitorExtratoCsvNubank.class, LeitorDeExtratoFactory.para(arquivo));
    }

    //qualquer outro cabeçalho escolhe o leitor genérico
    @Test
    @DisplayName("Outro cabeçalho: escolhe o leitor genérico")
    void escolherLeitorGenerico() throws IOException {
        Path arquivo = criarCsv("Data;Descrição;Valor\n21/11/2026;Uber;-18,50");

        assertInstanceOf(LeitorExtratoCsvGenerico.class, LeitorDeExtratoFactory.para(arquivo));
    }

    //arquivo vazio não quebra a factory, ela devolve o genérico
    @Test
    @DisplayName("Arquivo vazio: escolhe o leitor genérico")
    void escolherLeitorParaArquivoVazio() throws IOException {
        Path arquivo = criarCsv("");

        assertInstanceOf(LeitorExtratoCsvGenerico.class, LeitorDeExtratoFactory.para(arquivo));
    }
}
