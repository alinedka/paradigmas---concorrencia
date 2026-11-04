package concorrencia.parte2.outras

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 * PARTE 2.5 - OUTRAS FORMAS DE CONCORRÊNCIA
 *
 *  a) Channels  - comunicação entre corrotinas no estilo CSP (como Go):
 *                 "não comunique compartilhando memória, compartilhe memória comunicando".
 *  b) select    - espera em vários canais ao mesmo tempo (como select do Go).
 *  c) Flow      - streams assíncronos e reativos (similar a RxJava/Reactive Streams).
 *  d) StateFlow - estado observável compartilhado.
 *  e) Virtual Threads (Java 21+, Projeto Loom) - threads leves geridas pela JVM.
 *  f) Parallel streams / ExecutorService (Java) - paralelismo de dados.
 *  g) sequence { } - geradores (corrotinas síncronas, sem concorrência real).
 */

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    // a) CHANNELS - produtor/consumidor estilo CSP
    println("a) Channel produtor -> consumidor")
    val canal = Channel<Int>(capacity = 2)
    launch {
        for (i in 1..5) { canal.send(i); println("   enviou $i") }
        canal.close()
    }
    for (v in canal) { delay(50); println("   recebeu $v") }

    // Fan-out: vários trabalhadores consumindo o mesmo canal
    println("\n   fan-out: 3 workers dividindo 6 tarefas")
    val tarefas = produce { for (t in 1..6) send(t) }
    List(3) { w ->
        launch(Dispatchers.Default) {
            for (t in tarefas) println("   worker $w processou tarefa $t")
        }
    }.joinAll()

    // b) SELECT - pega o que chegar primeiro
    println("\nb) select em dois canais")
    val rapido = produce { repeat(3) { delay(60); send("rápido-$it") } }
    val lento = produce { repeat(3) { delay(150); send("lento-$it") } }
    repeat(4) {
        val msg = select {
            rapido.onReceiveCatching { it.getOrNull() ?: "rápido fechou" }
            lento.onReceiveCatching { it.getOrNull() ?: "lento fechou" }
        }
        println("   $msg")
    }
    rapido.cancel(); lento.cancel()

    // c) FLOW - stream assíncrono "frio" com operadores
    println("\nc) Flow")
    val temperaturas = flow {
        for (t in listOf(18, 22, 35, 27, 40)) { delay(50); emit(t) }
    }
    temperaturas
        .filter { it > 25 }
        .map { "${it}°C - alerta!" }
        .collect { println("   $it") }

    // d) STATEFLOW - estado compartilhado e observável
    println("\nd) StateFlow")
    val estado = MutableStateFlow("desconectado")
    val observador = launch { estado.collect { println("   estado mudou: $it") } }
    delay(50); estado.value = "conectando"
    delay(50); estado.value = "conectado"
    delay(50); observador.cancel()

    // e) VIRTUAL THREADS (Java 21+) - funciona direto em Kotlin
    println("\ne) Virtual Threads (Java 21+)")
    val t = measureTimeMillis {
        Executors.newVirtualThreadPerTaskExecutor().use { exec ->
            repeat(10_000) { exec.submit { Thread.sleep(500) } }
        }
    }
    println("   10.000 virtual threads dormindo 500ms: ${t}ms")
    val vt = Thread.ofVirtual().start { println("   sou virtual? ${Thread.currentThread().isVirtual}") }
    vt.join()

    // Virtual threads como Dispatcher de corrotinas
    val vtDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
    withContext(vtDispatcher) { println("   corrotina rodando em virtual thread: ${Thread.currentThread().isVirtual}") }
    vtDispatcher.close()

    // f) PARALELISMO DE DADOS - parallelStream do Java
    println("\nf) parallelStream")
    val soma = (1..1_000_000L).toList().parallelStream().mapToLong { it * 2 }.sum()
    println("   soma paralela = $soma")

    // g) SEQUENCE - gerador (corrotina, mas sem concorrência)
    println("\ng) sequence {} (gerador com yield)")
    val fibonacci = sequence {
        var a = 0; var b = 1
        while (true) { yield(a); val tmp = a + b; a = b; b = tmp }
    }
    println("   ${fibonacci.take(10).toList()}")
}
