package concorrencia.parte2.eventloop

import kotlinx.coroutines.*
import java.util.concurrent.Executors

/**
 * PARTE 2.2 - EVENT LOOP
 *
 * Kotlin NÃO tem um event loop global embutido na linguagem (como JavaScript/Node).
 * Porém a biblioteca kotlinx.coroutines USA event loops internamente:
 *
 *  - runBlocking { } cria um EVENT LOOP na thread atual (classe interna
 *    BlockingEventLoop): mantém uma fila de continuações e as executa
 *    uma a uma na mesma thread, até que todas as corrotinas terminem.
 *  - Dispatchers.Main (Android/JavaFX/Swing) despacha para o event loop da UI
 *    (no Android, o Looper/Handler da main thread).
 *  - Um dispatcher de thread única (newSingleThreadContext /
 *    Executors.newSingleThreadExecutor().asCoroutineDispatcher()) se comporta
 *    como um event loop: uma thread, uma fila de tarefas.
 *  - Frameworks como Ktor/Vert.x/Netty usam event loops de I/O não bloqueante.
 *  - Kotlin/JS roda sobre o event loop do JavaScript.
 */

fun main() {
    // 1) runBlocking = event loop numa única thread
    println("1) Event loop do runBlocking (tudo na mesma thread)")
    runBlocking {
        launch { log("tarefa A - início"); delay(100); log("tarefa A - fim") }
        launch { log("tarefa B - início"); delay(50);  log("tarefa B - fim") }
        launch { log("tarefa C - sem suspensão") }
        log("corpo do runBlocking terminou de agendar as tarefas")
    }
    // Repare: B termina antes de A (o timer de B dispara primeiro)
    // e todas rodaram na thread "main" -> é o loop processando a fila.

    // 2) Bloquear o event loop trava TODAS as tarefas (igual ao JS)
    println("\n2) Bloquear o loop é ruim")
    runBlocking {
        launch {
            delay(10)
            log("timer de 10ms só disparou agora, depois do bloqueio!")
        }
        launch {
            log("bloqueando a thread do loop com Thread.sleep(500)...")
            Thread.sleep(500)   // bloqueia a thread -> nenhuma outra tarefa roda
            log("fim do bloqueio")
        }
    }

    // 3) Event loop próprio: dispatcher de thread única
    println("\n3) Dispatcher de thread única como event loop")
    val loop = Executors.newSingleThreadExecutor { r -> Thread(r, "meu-event-loop") }
        .asCoroutineDispatcher()
    runBlocking {
        val eventos = listOf("clique", "tecla", "rede", "timer")
        eventos.mapIndexed { i, evento ->
            launch(loop) {
                delay((4 - i) * 30L)  // eventos "chegam" em tempos diferentes
                log("tratando evento '$evento'")
            }
        }.joinAll()
    }
    loop.close()

    // 4) Demonstração conceitual: um mini event loop escrito à mão
    println("\n4) Mini event loop manual (fila + laço)")
    val fila = ArrayDeque<() -> Unit>()
    fun agendar(tarefa: () -> Unit) = fila.addLast(tarefa)
    agendar { println("   evento 1"); agendar { println("   evento 3 (agendado pelo 1)") } }
    agendar { println("   evento 2") }
    while (fila.isNotEmpty()) fila.removeFirst()()   // o "loop"
}

private val inicio = System.currentTimeMillis()
fun log(msg: String) =
    println("   [%4dms][%s] %s".format(System.currentTimeMillis() - inicio, Thread.currentThread().name, msg))
