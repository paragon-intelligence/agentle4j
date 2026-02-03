package com.paragon.messaging.batching;

/**
 * Estratégia para lidar com backpressure quando buffer do usuário está cheio.
 *
 * <p>Backpressure ocorre quando mensagens chegam mais rápido que podem ser
 * processadas, causando enchimento do buffer até capacidade máxima.</p>
 *
 * <p><b>Exemplo:</b></p>
 * <pre>{@code
 * BatchingConfig config = BatchingConfig.builder()
 *     .maxBufferSize(50)
 *     .backpressureStrategy(BackpressureStrategy.DROP_OLDEST)
 *     .build();
 * }</pre>
 *
 * @author Agentle Team
 * @since 1.0
 */
public enum BackpressureStrategy {

  /**
   * Descarta mensagem nova (buffer inalterado).
   *
   * <p><b>Use quando:</b> Mensagens antigas têm mais contexto.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Nova mensagem é descartada silenciosamente</li>
   *   <li>Buffer preserva mensagens mais antigas</li>
   *   <li>Sem notificação ao usuário</li>
   * </ul>
   */
  DROP_NEW,

  /**
   * Descarta mensagem mais antiga para dar espaço à nova.
   *
   * <p><b>Use quando:</b> Mensagens recentes são mais relevantes.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Mensagem mais antiga é removida</li>
   *   <li>Nova mensagem é adicionada</li>
   *   <li>Sem notificação ao usuário</li>
   * </ul>
   */
  DROP_OLDEST,

  /**
   * Rejeita mensagem nova e opcionalmente notifica usuário.
   *
   * <p><b>Use quando:</b> Quer informar que está enviando rápido demais.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Nova mensagem é rejeitada</li>
   *   <li>Notificação opcional enviada ao usuário</li>
   *   <li>Buffer inalterado</li>
   * </ul>
   */
  REJECT_WITH_NOTIFICATION,

  /**
   * Bloqueia até espaço ficar disponível (espera síncrona).
   *
   * <p><b>Use quando:</b> Perda de mensagem é inaceitável.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Webhook bloqueia (200 OK atrasado)</li>
   *   <li>Mensagem é enfileirada quando espaço disponível</li>
   *   <li><b>Risco:</b> Pode causar timeout do webhook</li>
   * </ul>
   *
   * <p><b>Aviso:</b> Webhooks da Meta timeout após 20 segundos!</p>
   */
  BLOCK_UNTIL_SPACE,

  /**
   * Processa buffer imediatamente, depois aceita nova mensagem.
   *
   * <p><b>Use quando:</b> Quer processar acumuladas quando buffer enche.</p>
   *
   * <p><b>Comportamento:</b></p>
   * <ul>
   *   <li>Buffer atual é enviado ao processor imediatamente</li>
   *   <li>Nova mensagem inicia buffer fresco</li>
   *   <li>Sem perda de mensagens</li>
   * </ul>
   */
  FLUSH_AND_ACCEPT;

  /**
   * Verifica se estratégia pode causar perda de mensagens.
   *
   * @return true se pode perder mensagens
   */
  public boolean canLoseMessages() {
    return this == DROP_NEW || this == DROP_OLDEST || this == REJECT_WITH_NOTIFICATION;
  }

  /**
   * Verifica se estratégia pode bloquear webhook.
   *
   * @return true se pode bloquear
   */
  public boolean canBlock() {
    return this == BLOCK_UNTIL_SPACE;
  }

  /**
   * Retorna descrição legível da estratégia.
   *
   * @return descrição
   */
  public String description() {
    return switch (this) {
      case DROP_NEW -> "Descarta mensagens novas quando buffer cheio";
      case DROP_OLDEST -> "Descarta mensagens antigas para dar espaço às novas";
      case REJECT_WITH_NOTIFICATION -> "Rejeita mensagens novas e notifica usuário";
      case BLOCK_UNTIL_SPACE -> "Bloqueia webhook até buffer ter espaço";
      case FLUSH_AND_ACCEPT -> "Processa buffer imediatamente, depois aceita nova mensagem";
    };
  }
}
