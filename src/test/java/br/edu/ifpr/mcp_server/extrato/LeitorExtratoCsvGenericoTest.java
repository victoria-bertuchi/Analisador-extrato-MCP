package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorExtratoCsvGenericoTest {

    //o JUnit cria uma pasta temporária para cada teste e apaga no final
    @TempDir
    Path pasta;

    private final LeitorExtratoCsvGenerico leitor = new LeitorExtratoCsvGenerico();

    //cria um arquivo CSV na pasta temporária com as linhas informadas
    private Path criarCsv(String... linhas) throws IOException {
        Path arquivo = pasta.resolve("extrato.csv");
        Files.writeString(arquivo, String.join("\n", linhas), StandardCharsets.UTF_8);
        return arquivo;
    }

    //quando há coluna de tipo (por exemplo, no Banco do Brasil), só a Saída vira gasto (positivo) e a Entrada fica negativa
    @Test
    @DisplayName("Formato Banco do Brasil: usa a coluna Tipo Lançamento")
    void lerFormatoBancoDoBrasil() throws IOException {
        Path arquivo = criarCsv(
                "Data,\"Lançamento\",\"Detalhes\",\"N° documento\",\"Valor\",\"Tipo Lançamento\"",
                "10/11/2026,\"Compra com Cartão\",\"10/11 11:02 LOJA X\",\"1\",\"-10,00\",\"Saída\"",
                "11/11/2026,\"Pix - Recebido\",\"Fulano\",\"2\",\"700,00\",\"Entrada\"");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals(2, transacoes.size());
        assertEquals(new BigDecimal("10.00"), transacoes.get(0).valor());
        assertEquals(new BigDecimal("-700.00"), transacoes.get(1).valor());
    }

    // quando há duas colunas de descrição (um exemplo é no banco  BB), elas são juntadas numa só
    @Test
    @DisplayName("Junta as colunas Lançamento e Detalhes na descrição")
    void juntarColunasDeDescricao() throws IOException {
        Path arquivo = criarCsv(
                "Data,\"Lançamento\",\"Detalhes\",\"Valor\",\"Tipo Lançamento\"",
                "10/11/2026,\"Compra com Cartão\",\"LOJA X\",\"-10,00\",\"Saída\"");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals("Compra com Cartão - LOJA X", transacoes.get(0).descricao());
    }

    //linhas de saldo, inclusive "S A L D O" com espaços, não viram transação
    @Test
    @DisplayName("Ignora as linhas de saldo")
    void ignorarLinhasDeSaldo() throws IOException {
        Path arquivo = criarCsv(
                "Data,\"Lançamento\",\"Detalhes\",\"Valor\",\"Tipo Lançamento\"",
                "13/10/2026,\"Saldo Anterior\",\"\",\"0,00\",\"\"",
                "10/11/2026,\"Compra com Cartão\",\"LOJA X\",\"-10,00\",\"Saída\"",
                "10/11/2026,\"Saldo do dia\",\"\",\"924,33\",\"\"",
                "23/11/2026,\"S A L D O\",\"\",\"560,00\",\"\"");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals(1, transacoes.size());
    }

    //sem coluna de tipo, o gasto vem negativo e o leitor inverte para positivo
    @Test
    @DisplayName("Separador ponto e vírgula e valor brasileiro: inverte o sinal")
    void lerComPontoEVirgula() throws IOException {
        Path arquivo = criarCsv(
                "Data;Descrição;Valor",
                "21/11/2026;UBER *TRIP;-18,50",
                "19/11/2026;PIX RECEBIDO;1.200,00");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals(new BigDecimal("18.50"), transacoes.get(0).valor());
        assertEquals(new BigDecimal("-1200.00"), transacoes.get(1).valor());
        assertEquals("UBER *TRIP", transacoes.get(0).descricao());
    }

    // as colunas são achadas pelo nome, então a ordem delas não importa (formato da conta Nubank)
    @Test
    @DisplayName("Acha as colunas pelo nome, em qualquer ordem")
    void lerColunasEmOutraOrdem() throws IOException {
        Path arquivo = criarCsv(
                "Data,Valor,Identificador,Descrição",
                "21/11/2026,-18.50,abc,Uber *Trip");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals(new BigDecimal("18.50"), transacoes.get(0).valor());
        assertEquals("Uber *Trip", transacoes.get(0).descricao());
    }

    //linhas antes do cabeçalho (nome, agência, conta) são puladas
    @Test
    @DisplayName("Pula linhas que vêm antes do cabeçalho")
    void pularLinhasAntesDoCabecalho() throws IOException {
        Path arquivo = criarCsv(
                "Extrato Conta Corrente",
                "Agencia: 1234 Conta: 5678-9",
                "",
                "Data;Histórico;Valor",
                "21/11/2026;UBER *TRIP;-18,50");

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals(1, transacoes.size());
        assertEquals("UBER *TRIP", transacoes.get(0).descricao());
    }

    //os quatro formatos de data aceitos viram a mesma data
    @Test
    @DisplayName("Aceita os quatro formatos de data")
    void aceitarFormatosDeData() throws IOException {
        Path arquivo = criarCsv(
                "Data;Descrição;Valor",
                "05/11/2026;A;-1,00",
                "05-11-2026;B;-1,00",
                "2026-11-05;C;-1,00",
                "2026/11/05;D;-1,00");

        List<Transacao> transacoes = leitor.ler(arquivo);

        LocalDate esperada = LocalDate.of(2026, 11, 5);
        for (Transacao transacao : transacoes) {
            assertEquals(esperada, transacao.data());
        }
    }

    //arquivo salvo com acentuação do Windows (ISO-8859-1) também é lido
    @Test
    @DisplayName("Lê arquivo com acentuação do Windows")
    void lerArquivoComAcentuacaoDoWindows() throws IOException {
        Path arquivo = pasta.resolve("extrato.csv");
        Files.writeString(arquivo, "Data;Descrição;Valor\n21/11/2026;Farmácia;-10,00", StandardCharsets.ISO_8859_1);

        List<Transacao> transacoes = leitor.ler(arquivo);

        assertEquals("Farmácia", transacoes.get(0).descricao());
    }

    //arquivo vazio devolve lista vazia, sem erro
    @Test
    @DisplayName("Arquivo vazio devolve lista vazia")
    void lerArquivoVazio() throws IOException {
        Path arquivo = criarCsv("");

        assertTrue(leitor.ler(arquivo).isEmpty());
    }

    // sem coluna de valor no cabeçalho, o leitor avisa com erro
    @Test
    @DisplayName("Sem coluna de valor: lança erro")
    void erroSemColunaDeValor() throws IOException {
        Path arquivo = criarCsv("Data;Descrição", "21/11/2026;UBER");

        assertThrows(IllegalArgumentException.class, () -> leitor.ler(arquivo));
    }

    //data num formato que não está na lista gera erro
    @Test
    @DisplayName("Data em formato desconhecido: lança erro")
    void erroDataDesconhecida() throws IOException {
        Path arquivo = criarCsv("Data;Descrição;Valor", "2026.11.05;UBER;-1,00");

        assertThrows(IllegalArgumentException.class, () -> leitor.ler(arquivo));
    }
}