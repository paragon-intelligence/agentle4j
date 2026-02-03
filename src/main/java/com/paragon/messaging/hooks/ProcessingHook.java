package com.paragon.messaging.hooks;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Hook executado antes ou depois do processamento de lote de mensagens.
 *
 * <p>Hooks recebem {@link HookContext} e podem realizar ações como:</p>
 * <ul>
 *   <li>Logging e métricas</li>
 *   <li>Validação e moderação de conteúdo</li>
 *   <li>Enriquecimento de dados</li>
 *   <li>Auditoria</li>
 * </ul>
 *
 * <p><b>Pre-hooks</b> podem interromper processamento lançando
 * {@link HookInterruptedException}:</p>
 * <pre>{@code
 * ProcessingHook moderationHook = context -> {
 *     if (hasInappropriateContent(context.messages())) {
 *         throw new HookInterruptedException(
 *             "Conteúdo inapropriado detectado",
 *             "CONTENT_MODERATION"
 *         );
 *     }
 * };
 * }</pre>
 *
 * <p><b>Compartilhamento de dados:</b></p>
 * <pre>{@code
 * ProcessingHook preHook = context -> {
 *     context.putMetadata("startTime", Instant.now());
 * };
 *
 * ProcessingHook postHook = context -> {
 *     Instant start = context.getMetadata("startTime", Instant.class).orElseThrow();
 *     Duration elapsed = Duration.between(start, Instant.now());
 *     metrics.record(context.userId(), elapsed);
 * };
 * }</pre>
 *
 * @author Agentle Team
 * @see HookContext
 * @see HookInterruptedException
 * @since 1.0
 */
@FunctionalInterface
public interface ProcessingHook {

  /**
   * Hook no-op que não faz nada.
   *
   * @return hook vazio
   */
  static ProcessingHook noOp() {
    return context -> {
    };
  }

  /**
   * Hook de logging básico.
   *
   * @param logger consumidor da mensagem de log
   * @return hook de logging
   */
  static ProcessingHook logging(Consumer<String> logger) {
    return context -> {
      String msg = String.format(
              "Processing batch for user %s: %d messages (retry: %s)",
              context.userId(),
              context.batchSize(),
              context.isRetry()
      );
      logger.accept(msg);
    };
  }

  /**
   * Hook para medir tempo de processamento.
   *
   * <p>Retorna par de hooks (pre, post) que medem duração.</p>
   *
   * @param recorder consumidor que recebe userId e duração
   * @return par de hooks (pre, post)
   */
  static HookPair timing(BiConsumer<String, Duration> recorder) {
    ProcessingHook preHook = context -> {
      context.putMetadata("_timing_start", Instant.now());
    };

    ProcessingHook postHook = context -> {
      Instant start = context.getMetadata("_timing_start", Instant.class)
              .orElse(context.batchStartTime());
      Duration duration = Duration.between(start, Instant.now());
      recorder.accept(context.userId(), duration);
    };

    return new HookPair(preHook, postHook);
  }

  /**
   * Combina múltiplos hooks em um único.
   *
   * @param hooks hooks a combinar
   * @return hook composto
   */
  static ProcessingHook compose(ProcessingHook... hooks) {
    return context -> {
      for (ProcessingHook hook : hooks) {
        hook.execute(context);
      }
    };
  }

  /**
   * Executa lógica do hook.
   *
   * @param context contexto com informações do lote
   * @throws HookInterruptedException para interromper processamento (pre-hooks)
   * @throws Exception                para erros (será logado e pode triggerar retry)
   */
  void execute(HookContext context) throws Exception;

  /**
   * Par de pre-hook e post-hook.
   *
   * @param preHook  hook executado antes
   * @param postHook hook executado depois
   */
  record HookPair(ProcessingHook preHook, ProcessingHook postHook) {
  }
}