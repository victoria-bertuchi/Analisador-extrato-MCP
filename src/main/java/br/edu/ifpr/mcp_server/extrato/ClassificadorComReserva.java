package br.edu.ifpr.mcp_server.extrato;

/**
 * Classificador que tenta o principal (a IA) e, se ele falhar, passa a usar a reserva
 * (a palavra-chave). É o padrão de projeto Decorator: ele "embrulha" dois classificadores
 * e continua assinando o mesmo contrato ClassificadorDeCategoria.
 */
public class ClassificadorComReserva implements ClassificadorDeCategoria {

    private final ClassificadorDeCategoria principal;
    private final ClassificadorDeCategoria reserva;

    //começa falso. vira verdadeiro na primeira falha da IA e não volta mais,
    //para não esperar a IA falhar de novo em cada compra
    private boolean usandoReserva = false;

    public ClassificadorComReserva(ClassificadorDeCategoria principal, ClassificadorDeCategoria reserva) {
        this.principal = principal;
        this.reserva = reserva;
    }

    @Override
    public Categoria classificar(Transacao transacao) {
        if (!usandoReserva) {
            try {
                return principal.classificar(transacao);
            } catch (IllegalStateException e) {
                //a IA não respondeu (sem internet, servidor fora do ar, chave errada): troca para a reserva
                usandoReserva = true;
            }
        }
        return reserva.classificar(transacao);
    }

    //a ferramenta usa este método para avisar, na resposta, que a reserva foi usada
    public boolean usouReserva() {
        return usandoReserva;
    }
}