package cookbooks.clinicamedica.models;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Resultado estruturado de uma consulta médica.
 *
 * <p>Este record é a saída comum de todos os agentes especialistas. O RouterAgent.Structured
 * usa este tipo para garantir que qualquer agente roteado produza um resultado tipado.
 *
 * @param diagnostico diagnóstico formulado pelo especialista
 * @param tratamentoRecomendado tratamento recomendado para o paciente
 * @param examesSolicitados lista de exames solicitados (pode ser vazia)
 * @param medicoResponsavel nome do especialista responsável
 * @param precisaRetorno indica se o paciente precisa de retorno
 * @param observacoes observações adicionais (pode ser null)
 */
public record ResultadoConsulta(
    String diagnostico,
    String tratamentoRecomendado,
    @Nullable List<String> examesSolicitados,
    String medicoResponsavel,
    boolean precisaRetorno,
    @Nullable String observacoes) {}
