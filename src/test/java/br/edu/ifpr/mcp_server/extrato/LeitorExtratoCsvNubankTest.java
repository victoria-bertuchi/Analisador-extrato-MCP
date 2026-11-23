package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorExtratoCsvNubankTest {

    @TempDir
    Path pasta;

    //no CSV do Nubank (formato date,title,amount), o gasto já vem positivo, então o leitor mantém o valor como está
    @Test
    @DisplayName("Lê a fatura Nubank mantendo o sinal do valor")
    void lerFaturaNubank() throws IOException {
        Path arquivo = pasta.resolve("fatura.csv");
        Files.writeString(arquivo, "date,title,amount\n"
                                 + "2026-11-01,Ifd*Saiko Sushi,45.90\n"
                                 + "2026-11-05,Pagamento recebido,-200.00\n");

        List<Transacao> transacoes = new LeitorExtratoCsvNubank().ler(arquivo);

        assertEquals(2, transacoes.size());
        assertEquals(LocalDate.of(2026, 11, 1), transacoes.get(0).data());
        assertEquals("Ifd*Saiko Sushi", transacoes.get(0).descricao());
        assertEquals(new BigDecimal("45.90"), transacoes.get(0).valor());
        assertEquals(new BigDecimal("-200.00"), transacoes.get(1).valor());
    }
}