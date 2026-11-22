package br.edu.ifpr.mcp_server.extrato;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitor para extratos de conta de outros bancos (Banco do Brasil, Santander, Nubank conta...).
 * Em vez de depender da ordem das colunas, ele lê o cabeçalho
 * e procura pelo NOME as colunas de data, descrição e valor.
 */
public class LeitorExtratoCsvGenerico implements LeitorDeExtrato {

    // nomes que cada coluna pode ter, em minúsculo e sem acento
    private static final List<String> NOMES_DATA = List.of("data", "date", "data lancamento");
    private static final List<String> NOMES_DESCRICAO = List.of("lancamento", "descricao", "historico", "title", "detalhes");
    private static final List<String> NOMES_VALOR = List.of("valor", "amount", "valor (r$)");
    private static final List<String> NOMES_TIPO = List.of("tipo lancamento", "tipo");

    // formatos de data aceitos. "d" e "M" aceitam com ou sem zero na frente (5/11 ou 05/11)
    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),   // 05/11/2026 (dia, mês, ano)
            DateTimeFormatter.ofPattern("d-M-yyyy"),   // 05-11-2026
            DateTimeFormatter.ofPattern("yyyy-M-d"),   // 2026-11-05 (ano, mês, dia)
            DateTimeFormatter.ofPattern("yyyy/M/d"));  // 2026/11/05

    @Override
    public List<Transacao> ler(Path arquivo) throws IOException {
        List<String> linhas = juntarLinhasQuebradas(lerLinhas(arquivo));
        if (linhas.isEmpty()) {
            return List.of();
        }

        // alguns bancos colocam linhas antes do cabeçalho (nome, agência, conta...): procura onde ele está
        int linhaCabecalho = acharLinhaDoCabecalho(linhas);

        // descobre o separador e em que posição está cada coluna, olhando o cabeçalho
        String cabecalho = linhas.get(linhaCabecalho);
        String separador = cabecalho.contains(";") ? ";" : ",";
        List<String> colunas = separar(cabecalho, separador);
        int posicaoData = acharColuna(colunas, NOMES_DATA, true);
        int posicaoValor = acharColuna(colunas, NOMES_VALOR, true);
        int posicaoTipo = acharColuna(colunas, NOMES_TIPO, false);
        List<Integer> posicoesDescricao = acharColunasDescricao(colunas);
        // a linha precisa ter pelo menos até a coluna de data e a de valor
        int colunasNecessarias = Math.max(posicaoData, posicaoValor) + 1;

        List<Transacao> transacoes = new ArrayList<>();
        for (String linha : linhas.subList(linhaCabecalho + 1, linhas.size())) {
            if (linha.isBlank()) {
                continue;
            }
            List<String> partes = separar(linha, separador);
            // linhas sem data ou com colunas faltando (rodapé, totais, avisos do banco) não são lançamentos
            if (partes.size() < colunasNecessarias || partes.get(posicaoData).isBlank()) {
                continue;
            }
            String descricao = montarDescricao(partes, posicoesDescricao);
            // linhas de saldo ("Saldo Anterior", "Saldo do dia", "S A L D O") não são gastos.
            // replace(" ", "") tira os espaços, para "S A L D O" virar "saldo"
            if (normalizar(descricao).replace(" ", "").startsWith("saldo")) {
                continue;
            }
            LocalDate data = converterData(partes.get(posicaoData));
            BigDecimal valor = converterValor(partes.get(posicaoValor));

            if (posicaoTipo >= 0) {
                // se o extrato tem uma coluna de tipo (ex.: "Tipo Lançamento" do BB), ela diz se é Entrada ou Saída
                boolean saida = normalizar(partes.get(posicaoTipo)).startsWith("saida");
                valor = saida ? valor.abs() : valor.abs().negate();
            } else {
                // sem coluna de tipo, o gasto vem negativo: inverte o sinal para o gasto ficar positivo
                valor = valor.negate();
            }
            transacoes.add(new Transacao(data, valor, descricao));
        }
        return transacoes;
    }

    // tenta ler como UTF-8; se o banco salvou com acentuação do Windows (ISO-8859-1), lê assim
    private List<String> lerLinhas(Path arquivo) throws IOException {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readAllLines(arquivo, StandardCharsets.ISO_8859_1);
        }
    }

    // uma descrição entre aspas pode ter quebra de linha dentro; aqui a linha quebrada é juntada de volta.
    // enquanto o número de aspas estiver ímpar, a linha ainda não terminou
    private List<String> juntarLinhasQuebradas(List<String> linhas) {
        List<String> juntas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String linha : linhas) {
            if (!atual.isEmpty()) {
                atual.append(' ');
            }
            atual.append(linha);
            long aspas = atual.chars().filter(c -> c == '"').count();
            if (aspas % 2 == 0) {
                juntas.add(atual.toString());
                atual.setLength(0);
            }
        }
        if (!atual.isEmpty()) {
            juntas.add(atual.toString());
        }
        return juntas;
    }

    // o cabeçalho é a primeira linha que tem uma coluna de data e uma de valor.
    // se nenhuma linha tiver, devolve 0 e o erro de "coluna não encontrada" aparece depois
    private int acharLinhaDoCabecalho(List<String> linhas) {
        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            List<String> colunas = separar(linha, linha.contains(";") ? ";" : ",");
            if (acharColuna(colunas, NOMES_DATA, false) >= 0 && acharColuna(colunas, NOMES_VALOR, false) >= 0) {
                return i;
            }
        }
        return 0;
    }

    // divide a linha pelo separador, mas ignora o separador que está dentro de aspas ("1.234,56")
    private List<String> separar(String linha, String separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (char c : linha.toCharArray()) {
            if (c == '"') {
                dentroDeAspas = !dentroDeAspas;
            } else if (!dentroDeAspas && separador.charAt(0) == c) {
                partes.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        partes.add(atual.toString().trim());
        return partes;
    }

    // procura no cabeçalho uma coluna com um dos nomes aceitos; devolve -1 se não for obrigatória e não existir
    private int acharColuna(List<String> colunas, List<String> nomesAceitos, boolean obrigatoria) {
        for (int i = 0; i < colunas.size(); i++) {
            if (nomesAceitos.contains(normalizar(colunas.get(i)))) {
                return i;
            }
        }
        if (obrigatoria) {
            throw new IllegalArgumentException("Coluna não encontrada no cabeçalho. Esperado um destes nomes: " + nomesAceitos);
        }
        return -1;
    }

    // a descrição pode estar em mais de uma coluna (ex.: "Lançamento" e "Detalhes" no BB); quando isso acontece, o leitor junta todas
    private List<Integer> acharColunasDescricao(List<String> colunas) {
        List<Integer> posicoes = new ArrayList<>();
        for (int i = 0; i < colunas.size(); i++) {
            if (NOMES_DESCRICAO.contains(normalizar(colunas.get(i)))) {
                posicoes.add(i);
            }
        }
        if (posicoes.isEmpty()) {
            throw new IllegalArgumentException("Coluna não encontrada no cabeçalho. Esperado um destes nomes: " + NOMES_DESCRICAO);
        }
        return posicoes;
    }

    private String montarDescricao(List<String> partes, List<Integer> posicoes) {
        List<String> pedacos = new ArrayList<>();
        for (int posicao : posicoes) {
            if (posicao < partes.size() && !partes.get(posicao).isBlank()) {
                pedacos.add(partes.get(posicao));
            }
        }
        return String.join(" - ", pedacos);
    }

    // deixa em minúsculo e sem acento, para "Descrição" e "descricao" serem iguais
    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase();
    }

    // tenta cada formato da lista; o primeiro que funcionar é o usado
    private LocalDate converterData(String texto) {
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException e) {
                // não era este formato, tenta o próximo
            }
        }
        throw new IllegalArgumentException("Data em formato não reconhecido: " + texto);
    }

    private BigDecimal converterValor(String texto) {
        String numero = texto.replace("R$", "").replace(" ", "");
        if (numero.contains(",")) {
            // formato brasileiro: 1.234,56 -> tira o ponto do milhar e troca a vírgula por ponto
            numero = numero.replace(".", "").replace(",", ".");
        }
        return new BigDecimal(numero);
    }
}