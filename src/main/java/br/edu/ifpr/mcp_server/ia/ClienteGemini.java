package br.edu.ifpr.mcp_server.ia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;
import tools.jackson.databind.JsonNode;

public class ClienteGemini implements ClienteIA {
	
	// endereço da internet pra onde mandamos as perguntas
	private static final String URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
	//versão do gpt usada
	private static final String MODELO = "gemini-flash-lite-latest";
	
	// a chave, api-key
	private final String chave;
	
	//ferramenta que envia pedidos pela internet
	private final HttpClient http = HttpClient.newHttpClient();
	
	//ferramenta wur monta e lê textos no formato JSON
	private final ObjectMapper json = JsonMapper.builder().build();
			
	//recebe a chave de fora e guarda na gaveta (construtor)
	public ClienteGemini (String chave) {
		this.chave = chave;
	}
	
    /**
     * Manda as mensagens system e user para o ChatGPT e devolve o texto da resposta.
     */
    @Override
    public String perguntar(String instrucoesDoSistema, String mensagemDoUsuario) throws Exception {

        //Monta a pergunta no formato JSON que a OpenAI entende
        ObjectNode corpo = json.createObjectNode();         // cria um JSON vazio: { }
        corpo.put("model", MODELO);                        // diz qual modelo usar
        ArrayNode mensagens = corpo.putArray("messages"); // cria a lista de mensagens
        mensagens.addObject().put("role", "system").put("content", instrucoesDoSistema); // as regras
        mensagens.addObject().put("role", "user").put("content", mensagemDoUsuario);    // a pergunta

        //Monta o pedido(para onde vai, a chave e o JSON)
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(URL))
                .header("Authorization", "Bearer " + chave)       // a chave vai aqui
                .header("Content-Type", "application/json")      // avisa que é JSON
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                .build();

        //Envia pela internet e tenta de novo se o servidor estiver ocupado (até 4 vezes)
        HttpResponse<String> resposta = null;
        for (int tentativa = 1; tentativa <=4; tentativa++) {
        	resposta = http.send(pedido,HttpResponse.BodyHandlers.ofString());
        

	        //caso seja 503= servidor lotado, 429= pedidos demais, ele tenta mais algumas vezes; qualquer outro código ele para de tentar 
	        if (resposta.statusCode() != 503 && resposta.statusCode() !=429) {
	            break;
	        }
	        
	        System.err.println("Servidor ocupado, tentando denovo em " + (tentativa * 5) + " segundos...");
	        Thread.sleep(tentativa * 5000L); //espera 5s, 10s, 15s...
        }
        //200= deu certo; qualquer outra coisa vira um erro
        if (resposta.statusCode() != 200) {
        	throw new IllegalStateException ("Erro da IA" + resposta.statusCode() + ": " + resposta.body());
        }

        //Tira só o texto da resposta, que fica em choices[0].message.content
        JsonNode raiz = json.readTree(resposta.body());
        return raiz.path("choices").path(0).path("message").path("content").asString();
    }
}


