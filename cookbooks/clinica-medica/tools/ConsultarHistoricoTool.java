package cookbooks.clinicamedica.tools;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Ferramenta para consultar o histórico médico de um paciente.
 *
 * <p>Usa construtor sem argumentos, podendo ser carregada via YAML com {@code toolClassNames}.
 * Em produção, esta ferramenta consultaria um banco de dados real.
 */
@FunctionMetadata(
    name = "consultar_historico",
    description = "Consulta o histórico médico de um paciente pelo CPF.")
public class ConsultarHistoricoTool extends FunctionTool<ConsultarHistoricoTool.Params> {

  /** Parâmetros para consulta de histórico. */
  public record Params(@NonNull String cpf) {}

  public ConsultarHistoricoTool() {
    super();
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable Params params) {
    if (params == null) {
      return FunctionToolCallOutput.error("CPF não informado");
    }

    // Histórico simulado — em produção consultaria o banco de dados
    String historico =
        """
        Histórico do paciente (CPF: %s):
        - 2023-03: Hipertensão leve (controlada com losartana 50mg)
        - 2023-08: Gripe com complicação bronquítica (antibiótico 7 dias)
        - 2024-01: Exame de rotina — colesterol LDL levemente elevado (142 mg/dL)
        - Alergias conhecidas: dipirona, penicilina
        - Tipo sanguíneo: O+
        """
            .formatted(params.cpf());

    return FunctionToolCallOutput.success(historico);
  }
}
