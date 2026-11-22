package br.edu.ifpr.mcp_server.mcp.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Descobre qual arquivo CSV a ferramenta deve ler, a partir dos argumentos que o Claude envia:
 * o caminho informado em "arquivo" ou, quando o usuário anexa o CSV no chat, o texto em
 * "conteudo" (que é gravado num arquivo temporário).
 * Todas as ferramentas de extrato usam esta classe, então aceitam os mesmos parâmetros
 * e respondem os mesmos erros (DRY).
 */
public class ResolvedorDeArquivoDoExtrato {

   public static final String ATRIBUTO_ARQUIVO = "arquivo";
   public static final String ATRIBUTO_CONTEUDO = "conteudo";

   /**
    * Parâmetros das ferramentas de extrato, no formato do esquema JSON do MCP.
    * Fica aqui para o nome dos parâmetros nunca ficar diferente entre as ferramentas.
    */
   public static Map<String, Object> propriedadesDoEsquema() {
      return Map.<String, Object>of(
            ATRIBUTO_ARQUIVO, Map.of("type", "string",
                  "description", "Caminho completo do arquivo CSV do extrato no computador"),
            ATRIBUTO_CONTEUDO, Map.of("type", "string",
                  "description", "Texto completo do CSV, linha por linha, quando o usuário anexar o arquivo no chat"));
   }

   /**
    * Devolve o arquivo a ser lido: o texto do CSV (se veio) tem prioridade sobre o caminho.
    *
    * @throws IllegalArgumentException se nada foi informado, se o arquivo não existe ou se não é .csv
    * @throws IOException              se não foi possível gravar o arquivo temporário
    */
   public Path resolver(Map<String, Object> argumentos) throws IOException {
      if (argumentos == null) {
         argumentos = Map.of();
      }
      if (argumentos.get(ATRIBUTO_CONTEUDO) instanceof String conteudo && !conteudo.isBlank()) {
         return gravarTemporario(conteudo);
      }
      if (!(argumentos.get(ATRIBUTO_ARQUIVO) instanceof String caminho) || caminho.isBlank()) {
         throw new IllegalArgumentException("Informe o caminho do arquivo CSV ou anexe o arquivo no chat.");
      }
      return abrirCaminho(caminho);
   }

   private Path gravarTemporario(String conteudo) throws IOException {
      Path temporario = Files.createTempFile("extrato", ".csv");
      Files.writeString(temporario, conteudo, StandardCharsets.UTF_8);
      temporario.toFile().deleteOnExit();
      return temporario;
   }

   //tira as aspas que o Windows coloca em "Copiar como caminho" e confere se o arquivo serve
   private Path abrirCaminho(String caminho) {
      Path arquivo = Path.of(caminho.replace("\"", "").trim());
      if (!Files.exists(arquivo)) {
         throw new IllegalArgumentException("Arquivo não encontrado: " + caminho);
      }
      if (!arquivo.toString().toLowerCase().endsWith(".csv")) {
         throw new IllegalArgumentException("O arquivo precisa ser .csv: " + caminho);
      }
      return arquivo;
   }
}