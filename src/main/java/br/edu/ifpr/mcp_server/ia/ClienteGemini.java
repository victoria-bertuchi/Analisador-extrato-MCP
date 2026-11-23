package br.edu.ifpr.mcp_server.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Fala com o Google Gemini pela API compatível com o formato da OpenAI.
 */
public class ClienteGemini implements ClienteIA {

	private static final Logger LOG = LoggerFactory.getLogger(ClienteGemini.class);

	// endereço da internet para onde mandamos as perguntas
	private static final String URL_PADRAO = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
	// modelo do Gemini usado
	private static final String MODELO_PADRAO = "gemini-flash-lite-latest";
	private static final int MAXIMO_DE_TENTATIVAS = 3;
	private static final Duration TEMPO_LIMITE = Duration.ofSeconds(30);

	private final String chave;
	private final String url;
	private final String modelo;

	// ferramenta que envia pedidos pela internet
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	// ferramenta que monta e lê textos no formato JSON
	private final ObjectMapper json = JsonMapper.builder().build();

	/**
	 * Usa a URL e o modelo padrão, ou os das variáveis de ambiente GEMINI_URL e
	 * GEMINI_MODEL, se existirem.
	 */
	public ClienteGemini(String chave) {
		this(chave, valorOuPadrao("GEMINI_URL", URL_PADRAO), valorOuPadrao("GEMINI_MODEL", MODELO_PADRAO));
	}

	/**
	 * Permite apontar para outro endereço, por exemplo um servidor falso nos
	 * testes.
	 */
	public ClienteGemini(String chave, String url, String modelo) {
		this.chave = chave;
		this.url = url;
		this.modelo = modelo;
	}

	/**
	 * Manda as mensagens system e user para o Gemini e devolve o texto da resposta.
	 */
	@Override
	public String perguntar(String instrucoesDoSistema, String mensagemDoUsuario) throws Exception {
		HttpRequest pedido = montarPedido(instrucoesDoSistema, mensagemDoUsuario);
		HttpResponse<String> resposta = enviarComTentativas(pedido);

		// 200 = deu certo; qualquer outra coisa vira um erro
		if (resposta.statusCode() != 200) {
			throw new IllegalStateException("Erro da IA " + resposta.statusCode() + ": " + resposta.body());
		}
		return extrairTexto(resposta.body());
	}

	// monta o pedido: para onde vai, a chave e o JSON
	private HttpRequest montarPedido(String instrucoes, String mensagem) {
		ObjectNode corpo = json.createObjectNode(); // cria um JSON vazio: { }
		corpo.put("model", modelo); // diz qual modelo usar
		ArrayNode mensagens = corpo.putArray("messages"); // cria a lista de mensagens
		mensagens.addObject().put("role", "system").put("content", instrucoes); // as regras
		mensagens.addObject().put("role", "user").put("content", mensagem); // a pergunta

		return HttpRequest.newBuilder(URI.create(url)).timeout(TEMPO_LIMITE).header("Authorization", "Bearer " + chave)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo))).build();
	}

	// 503 = servidor lotado e 429 = pedidos demais merecem outra tentativa;
	// qualquer outro código não
	private HttpResponse<String> enviarComTentativas(HttpRequest pedido) throws IOException, InterruptedException {
		HttpResponse<String> resposta = null;
		for (int tentativa = 1; tentativa <= MAXIMO_DE_TENTATIVAS; tentativa++) {
			resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
			boolean ocupado = resposta.statusCode() == 503 || resposta.statusCode() == 429;
			if (!ocupado || tentativa == MAXIMO_DE_TENTATIVAS) {
				return resposta;
			}
			long espera = tentativa * 2L; // espera 2s, 4s...
			LOG.warn("IA ocupada (HTTP {}). Nova tentativa em {} s...", resposta.statusCode(), espera);
			Thread.sleep(espera * 1000);
		}
		return resposta;
	}

	// tira só o texto da resposta, que fica em choices[0].message.content
	private String extrairTexto(String corpoDaResposta) {
		JsonNode raiz = json.readTree(corpoDaResposta);
		return raiz.path("choices").path(0).path("message").path("content").asString();
	}

	private static String valorOuPadrao(String variavel, String padrao) {
		String valor = System.getenv(variavel);
		return (valor == null || valor.isBlank()) ? padrao : valor;
	}
}