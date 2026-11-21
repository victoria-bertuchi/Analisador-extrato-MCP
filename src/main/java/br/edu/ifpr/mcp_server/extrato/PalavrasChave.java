package br.edu.ifpr.mcp_server.extrato;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista de palavras-chave de cada categoria, usada pela classificação reserva
 * (quando a IA não está disponível).
 * As palavras ficam em minúsculo, porque o classificador converte a descrição para minúsculo.
 */
public final class PalavrasChave {

    //construtor privado: ninguém precisa criar um objeto desta classe, só usar o método padrao()
    private PalavrasChave() {
    }

    public static Map<Categoria, List<String>> padrao() {
        //linkedHashMap guarda as categorias NA ORDEM em que foram colocadas.
        //a ordem importa, o classificador devolve a primeira categoria que encontrar.
        //por isso FARMACIA e MERCADO vêm antes de DELIVERY: "Ifd*Drogaria" vira FARMACIA, e não DELIVERY.
        Map<Categoria, List<String>> palavras = new LinkedHashMap<>();
        palavras.put(Categoria.FARMACIA, List.of("drogasil", "droga raia", "drogaria", "farmacia", "pague menos", "panvel"));
        palavras.put(Categoria.MERCADO, List.of("supermercado", "mercado", "carrefour", "assai", "atacadao", "pao de acucar", "condor", "muffato"));
        palavras.put(Categoria.PET, List.of("petz", "cobasi", "petshop", "pet shop"));
        palavras.put(Categoria.SAUDE, List.of("hospital", "clinica", "laboratorio", "unimed", "odonto"));
        palavras.put(Categoria.STREAMING, List.of("netflix", "spotify", "disney", "hbo", "prime video", "globoplay", "youtube"));
        palavras.put(Categoria.EDUCACAO, List.of("udemy", "alura", "escola", "faculdade", "curso", "livraria"));
        palavras.put(Categoria.VESTUARIO, List.of("c&a", "renner", "riachuelo", "zara", "hering", "centauro", "shein"));
        palavras.put(Categoria.CONTAS_DA_CASA, List.of("copel", "sanepar", "enel", "sabesp", "vivo", "claro"));
        palavras.put(Categoria.DELIVERY, List.of("ifd*", "ifood", "rappi", "ze delivery", "aiqfome"));
        palavras.put(Categoria.TRANSPORTE, List.of("uber", "99*", "99 *", "posto", "combustivel", "estacionamento"));
        palavras.put(Categoria.ALIMENTACAO, List.of("restaurante", "lanchonete", "padaria", "pizzaria", "burger", "mcdonald", "subway", "cafe"));
        palavras.put(Categoria.LAZER, List.of("cinema", "cinemark", "ingresso", "steam", "playstation"));
        return palavras;
    }
}