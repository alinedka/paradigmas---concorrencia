package concorrencia.parte2.promises

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

/**
 * PARTE 2.3 - MODELO DE PROMISES
 *
 * Kotlin não tem uma classe chamada "Promise" na JVM, mas tem equivalentes:
 *
 *  1) Deferred<T> (kotlinx.coroutines) - criado por async { }.
 *     É uma "promessa" de um valor futuro; await() suspende até ficar pronto.
 *     Estados: ativo -> completo (valor) | falhou (exceção) | cancelado.
 *  2) CompletableDeferred<T> - promise que você resolve "na mão"
 *     (equivale a new Promise((resolve, reject) => ...) do JS).
 *  3) CompletableFuture<T> (Java) - promise com encadeamento
 *     thenApply / thenCompose / exceptionally (estilo .then().catch()).
 *  4) Kotlin/JS tem kotlin.js.Promise de verdade.
 *
 *  Interoperabilidade: future.await() e scope.future { } convertem entre eles.
 */

suspend fun buscarPreco(produto: String): Double {
    delay(300)
    if (produto == "erro") throw IllegalArgumentException("produto inválido")
    return produto.length * 10.0
}

fun main() = runBlocking {
    // 1) async/await com Deferred
    println("1) async / await (Deferred)")
    val promessa: Deferred<Double> = async { buscarPreco("notebook") }
    println("   promessa criada, completa? ${promessa.isCompleted}")
    val preco = promessa.await()
    println("   valor resolvido: $preco, completa? ${promessa.isCompleted}")

    // 2) Equivalente ao Promise.all: várias promessas em paralelo
    println("\n2) awaitAll (≈ Promise.all)")
    val precos = listOf("mouse", "teclado", "monitor")
        .map { p -> async { p to buscarPreco(p) } }
        .awaitAll()
    println("   $precos")

    // 3) Rejeição: a exceção é relançada no await (≈ .catch)
    println("\n3) Promessa rejeitada")
    val falha = async(SupervisorJob()) { buscarPreco("erro") }
    try {
        falha.await()
    } catch (e: IllegalArgumentException) {
        println("   capturado no await: ${e.message}")
    }

    // 4) Equivalente ao Promise.race: select o primeiro que terminar
    println("\n4) Corrida (≈ Promise.race)")
    val rapido = async { delay(100); "servidor rápido" }
    val lento = async { delay(500); "servidor lento" }
    val vencedor = kotlinx.coroutines.selects.select {
        rapido.onAwait { it }
        lento.onAwait { it }
    }
    lento.cancel()
    println("   vencedor: $vencedor")

    // 5) CompletableDeferred: resolve/reject manual (≈ new Promise(...))
    println("\n5) CompletableDeferred (resolve manual)")
    val manual = CompletableDeferred<String>()
    launch {
        delay(200)
        manual.complete("resolvido por outra corrotina")   // resolve(...)
        // manual.completeExceptionally(Exception())        // reject(...)
    }
    println("   ${manual.await()}")

    // 6) CompletableFuture (Java) - encadeamento estilo .then()
    println("\n6) CompletableFuture com thenApply/thenCompose/exceptionally")
    val futuro = CompletableFuture
        .supplyAsync { Thread.sleep(200); 21 }
        .thenApply { it * 2 }                                     // .then(x => x*2)
        .thenCompose { v -> CompletableFuture.supplyAsync { "resposta = $v" } }
        .exceptionally { e -> "erro: ${e.message}" }              // .catch(...)
    println("   ${futuro.get()}")

    // 7) Interop: CompletableFuture -> suspend (await) e corrotina -> CompletableFuture
    println("\n7) Interoperabilidade")
    val cf = CompletableFuture.supplyAsync { "valor vindo do Java" }
    println("   await() de CompletableFuture: ${cf.await()}")
    // Atenção: rodar no Dispatchers.Default. Se a corrotina fosse agendada no event loop
    // do runBlocking, o get() (bloqueante) travaria a própria thread do loop -> DEADLOCK.
    val cfDeCorrotina: CompletableFuture<Double> = future(Dispatchers.Default) { buscarPreco("tablet") }
    println("   future {} expõe corrotina como CompletableFuture: ${cfDeCorrotina.get()}")
}
