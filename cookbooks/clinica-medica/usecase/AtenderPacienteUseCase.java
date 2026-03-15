package cookbooks.clinicamedica.usecase;

import com.paragon.agents.Interactable;
import com.paragon.agents.StructuredAgentResult;

/**
 * Caso de uso de atendimento de paciente.
 *
 * <p>Demonstra como encapsular a lógica de negócio em um use case que delega
 * ao agente roteador, sem conhecer qual especialista será escolhido.
 *
 * <p>O uso do genérico {@code T} permite reutilizar este use case com qualquer
 * tipo de saída estruturada, não apenas {@code ResultadoConsulta}.
 *
 * @param <T> tipo de saída estruturada do agente
 */
public class AtenderPacienteUseCase<T> {

  private final Interactable.Structured<T> roteador;

  /**
   * Cria o caso de uso com o roteador responsável por despachar para o especialista correto.
   *
   * @param roteador o agente roteador com saída estruturada
   */
  public AtenderPacienteUseCase(Interactable.Structured<T> roteador) {
    this.roteador = roteador;
  }

  /**
   * Executa o atendimento do paciente.
   *
   * <p>Formata a mensagem com dados do paciente e delega ao roteador, que classifica
   * a queixa e encaminha para o especialista adequado.
   *
   * @param input dados do paciente
   * @return resultado estruturado produzido pelo especialista
   */
  public StructuredAgentResult<T> executa(AtenderPacienteInput input) {
    String mensagem =
        "Paciente: %s (%d anos)\nQueixa: %s"
            .formatted(input.nomePaciente(), input.idadePaciente(), input.queixa());
    return roteador.interact(mensagem);
  }
}
