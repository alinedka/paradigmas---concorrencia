package concorrencia.parte1.threads

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * PARTE 1.1 - COMO GERO THREADS?
 *
 * Kotlin (na JVM) usa as threads da própria JVM: java.lang.Thread.
 * Cada Thread da JVM é (por padrão) mapeada 1:1 para uma thread do
 * sistema operacional. O Kotlin adiciona açúcar sintático (função thread {}).
 * A partir do Java 21, existem também as Virtual Threads (ver parte2/OutrasFormas.kt).
 */

// Forma 1: herdando de Thread
class MinhaThread(private val nome: String) : Thread() {
    override fun run() {
        println("[Forma 1 - herança]  $nome rodando em ${currentThread().name}")
    }
}

// Forma 2: implementando Runnable
class MinhaTarefa : Runnable {
    override fun run() {
        println("[Forma 2 - Runnable] rodando em ${Thread.currentThread().name}")
    }
}

fun main() {
    // Forma 1
    val t1 = MinhaThread("T1")
    t1.start()          // start() cria a thread do SO; run() direto NÃO criaria
    t1.join()           // espera terminar

    // Forma 2
    val t2 = Thread(MinhaTarefa(), "Thread-Runnable")
    t2.start()
    t2.join()

    // Forma 3: lambda passada ao construtor de Thread
    val t3 = Thread { println("[Forma 3 - lambda]   rodando em ${Thread.currentThread().name}") }
    t3.start()
    t3.join()

    // Forma 4: função idiomática do Kotlin (kotlin.concurrent.thread)
    // Já cria E inicia a thread. Permite configurar nome, daemon, prioridade.
    val t4 = thread(name = "Thread-Kotlin", isDaemon = false) {
        println("[Forma 4 - thread{}] rodando em ${Thread.currentThread().name}")
    }
    t4.join()

    // Forma 5: pool de threads (ExecutorService) - reaproveita threads
    val pool = Executors.newFixedThreadPool(3)
    repeat(6) { i ->
        pool.submit {
            println("[Forma 5 - pool]     tarefa $i em ${Thread.currentThread().name}")
        }
    }
    pool.shutdown()
    pool.awaitTermination(5, TimeUnit.SECONDS)

    // Várias threads em paralelo: a ordem de saída NÃO é determinística
    println("\n--- 5 threads em paralelo (ordem varia a cada execução) ---")
    val threads = (1..5).map { i ->
        thread { println("Olá da thread $i (${Thread.currentThread().name})") }
    }
    threads.forEach { it.join() }

    println("\nThread principal: ${Thread.currentThread().name}. Fim.")
}
