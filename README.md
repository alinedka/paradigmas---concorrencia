# Concorrência em Kotlin — Paradigmas de Programação

Exemplos executáveis de como Kotlin lida com concorrência: threads, memória
compartilhada, sincronização, corrotinas, event loop, promises, atores e outros modelos.

## Estrutura

| Tópico | Arquivo |
|---|---|
| 1.1 Como gerar threads | [parte1/threads/ThreadsCriacao.kt](src/main/kotlin/concorrencia/parte1/threads/ThreadsCriacao.kt) |
| 1.2 Memória compartilhada | [parte1/memoria/MemoriaCompartilhada.kt](src/main/kotlin/concorrencia/parte1/memoria/MemoriaCompartilhada.kt) |
| 1.3 Sincronização | [parte1/sincronizacao/Sincronizacao.kt](src/main/kotlin/concorrencia/parte1/sincronizacao/Sincronizacao.kt) |
| 2.1 Corrotinas | [parte2/corrotinas/Corrotinas.kt](src/main/kotlin/concorrencia/parte2/corrotinas/Corrotinas.kt) |
| 2.2 Event loop | [parte2/eventloop/EventLoop.kt](src/main/kotlin/concorrencia/parte2/eventloop/EventLoop.kt) |
| 2.3 Promises | [parte2/promises/Promises.kt](src/main/kotlin/concorrencia/parte2/promises/Promises.kt) |
| 2.4 Modelo de Atores | [parte2/atores/Atores.kt](src/main/kotlin/concorrencia/parte2/atores/Atores.kt) |
| 2.5 Outras formas (Channels, select, Flow, Virtual Threads...) | [parte2/outras/OutrasFormas.kt](src/main/kotlin/concorrencia/parte2/outras/OutrasFormas.kt) |
| Roda tudo em sequência | [Main.kt](src/main/kotlin/concorrencia/Main.kt) |

## Requisitos

- JDK 21+ (virtual threads exigem Java 21)
- Dependência: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`

## Como rodar

**IntelliJ IDEA:** abra a pasta como projeto Gradle e clique no ▶ ao lado
de qualquer `fun main()`.

**Gradle:**
```bash
gradle run                                            # todos os exemplos
gradle run -Pexemplo=parte2.corrotinas.CorrotinasKt   # um exemplo específico
```

**kotlinc (sem Gradle):** baixe o jar `kotlinx-coroutines-core-jvm-1.10.2.jar` do Maven Central e:
```bash
kotlinc src/main/kotlin -cp kotlinx-coroutines-core-jvm-1.10.2.jar -d app.jar
kotlin -cp "app.jar;kotlinx-coroutines-core-jvm-1.10.2.jar" concorrencia.MainKt
```

## Resumo

- **Threads:** são as threads da JVM (`java.lang.Thread`, 1:1 com o SO); Kotlin oferece `thread { }`.
- **Memória:** heap compartilhado e pilha por thread; sem sincronização há *race conditions*. Os recursos são `@Volatile` e `ThreadLocal`.
- **Sincronização:** `synchronized {}`, `@Synchronized`, `ReentrantLock.withLock`, `Atomic*`, `CountDownLatch`, `Semaphore`, `wait/notify`; nas corrotinas, `Mutex`.
- **Corrotinas:** é o modelo principal da linguagem. `suspend` é palavra-chave, e `launch`/`async` vêm do kotlinx.coroutines. A concorrência é estruturada.
- **Event loop:** a linguagem não tem um event loop global, mas `runBlocking`, `Dispatchers.Main` e dispatchers de thread única funcionam como event loops.
- **Promises:** `Deferred` (`async`/`await`), `CompletableDeferred` e `CompletableFuture`.
- **Atores:** `actor { }` do kotlinx.coroutines (obsoleto, mas funcional), atores feitos com `Channel`, e Akka/Apache Pekko.
- **Outros:** Channels (CSP), `select`, `Flow`/`StateFlow`, Virtual Threads, `parallelStream`, `sequence { }`.
