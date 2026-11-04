package concorrencia.parte2.corrotinas

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

/**
 * PARTE 2.1 - CORROTINAS
 *
 * SIM, Kotlin aplica corrotinas - é o principal modelo de concorrência da linguagem.
 *  - A palavra-chave 'suspend' faz parte da LINGUAGEM: o compilador transforma
 *    funções suspend em máquinas de estado (CPS - Continuation Passing Style).
 *  - Os construtores (launch, async, runBlocking...) vêm da biblioteca
 *    kotlinx.coroutines (oficial da JetBrains).
 *  - Corrotinas são "threads leves": suspender NÃO bloqueia a thread,
 *    ela fica livre para executar outra corrotina.
 *  - Concorrência estruturada: toda corrotina vive dentro de um CoroutineScope;
 *    o pai espera os filhos, e cancelamento/erros se propagam na árvore.
 *  - Dispatchers decidem em qual thread a corrotina roda:
 *      Default (CPU), IO (bloqueante), Main (UI/Android), Unconfined.
 */

// Função suspensa: pode pausar sem bloquear a thread
suspend fun buscarUsuario(id: Int): String {
    delay(500)                       // suspende (≠ Thread.sleep, que bloqueia)
    return "Usuário#$id"
}

fun main() = runBlocking {           // ponte entre o mundo bloqueante e o de corrotinas
    // 1) launch: dispara uma corrotina "fire and forget" (retorna Job)
    println("1) launch")
    val job = launch {
        delay(200)
        println("   corrotina filha em ${Thread.currentThread().name}")
    }
    println("   main continua sem esperar...")
    job.join()

    // 2) Sequencial vs concorrente com funções suspend
    println("\n2) Sequencial vs concorrente")
    val tSeq = measureTimeMillis {
        buscarUsuario(1); buscarUsuario(2); buscarUsuario(3)
    }
    val tConc = measureTimeMillis {
        val a = async { buscarUsuario(1) }
        val b = async { buscarUsuario(2) }
        val c = async { buscarUsuario(3) }
        println("   ${a.await()}, ${b.await()}, ${c.await()}")
    }
    println("   sequencial: ${tSeq}ms | concorrente: ${tConc}ms")

    // 3) Corrotinas são leves: 100.000 corrotinas sem problema
    println("\n3) 100.000 corrotinas")
    val tempo = measureTimeMillis {
        val jobs = List(100_000) { launch { delay(1000) } }
        jobs.joinAll()
    }
    println("   100.000 corrotinas dormindo 1s terminaram em ${tempo}ms")
    println("   (com 100.000 threads do SO, isso estouraria memória ou levaria muito mais)")

    // 4) Dispatchers: escolhendo a thread
    println("\n4) Dispatchers")
    launch(Dispatchers.Default) { println("   Default -> ${Thread.currentThread().name}") }.join()
    launch(Dispatchers.IO) { println("   IO      -> ${Thread.currentThread().name}") }.join()
    withContext(Dispatchers.Default) {
        println("   withContext troca de thread e volta: ${Thread.currentThread().name}")
    }
    println("   de volta em: ${Thread.currentThread().name}")

    // 5) Suspensão libera a thread: 2 corrotinas alternam numa thread só
    println("\n5) Duas corrotinas, uma única thread (cooperativo)")
    val a = launch { repeat(3) { println("   A$it"); yield() } }
    val b = launch { repeat(3) { println("   B$it"); yield() } }
    joinAll(a, b)

    // 6) Cancelamento cooperativo
    println("\n6) Cancelamento")
    val longo = launch {
        try {
            repeat(1000) { i -> println("   trabalhando $i..."); delay(100) }
        } finally {
            println("   limpeza no finally (recursos liberados)")
        }
    }
    delay(350)
    longo.cancelAndJoin()
    println("   job cancelado: ${longo.isCancelled}")

    // 7) Timeout
    val r = withTimeoutOrNull(300) { buscarUsuario(9) }
    println("\n7) withTimeoutOrNull(300ms) -> $r (a busca leva 500ms)")

    // 8) Concorrência estruturada: o erro de um filho cancela os irmãos
    println("\n8) Concorrência estruturada")
    try {
        coroutineScope {
            launch { delay(1000); println("   nunca imprime: fui cancelado") }
            launch { delay(100); throw IllegalStateException("falha no filho 2") }
        }
    } catch (e: IllegalStateException) {
        println("   escopo falhou com '${e.message}' e cancelou o irmão automaticamente")
    }

    // 9) Sincronização entre corrotinas: Mutex (suspende em vez de bloquear)
    println("\n9) Mutex")
    val mutex = Mutex()
    var contador = 0
    withContext(Dispatchers.Default) {        // várias threads de verdade
        List(1000) {
            launch { repeat(100) { mutex.withLock { contador++ } } }
        }.joinAll()
    }
    println("   contador = $contador (esperado 100000)")
}
