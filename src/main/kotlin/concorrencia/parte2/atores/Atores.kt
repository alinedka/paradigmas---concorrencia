package concorrencia.parte2.atores

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

/**
 * PARTE 2.4 - MODELO DE ATORES
 *
 * Ator = entidade com estado PRIVADO que só se comunica por MENSAGENS.
 * Cada ator processa uma mensagem por vez (mailbox) -> sem locks, sem race condition.
 *
 * Bibliotecas em Kotlin/JVM:
 *  - kotlinx.coroutines: função actor { } (marcada @ObsoleteCoroutinesApi,
 *    mas funcional) e Channels, com os quais se monta um ator facilmente.
 *  - Akka (Lightbend) / Apache Pekko (fork open source do Akka): modelo de
 *    atores completo, distribuído, com supervisão. Usável em Kotlin.
 *  - Outras: kactor, Orbit (atores virtuais), Vert.x (verticles, parecido).
 */

// Mensagens do ator (sealed class = conjunto fechado de mensagens)
sealed class MsgConta
data class Depositar(val valor: Int) : MsgConta()
data class Sacar(val valor: Int, val resposta: CompletableDeferred<Boolean>) : MsgConta()
data class ConsultarSaldo(val resposta: CompletableDeferred<Int>) : MsgConta()

// 1) Ator usando o builder actor {} da kotlinx.coroutines
@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.contaBancariaAtor(): SendChannel<MsgConta> = actor {
    var saldo = 0                          // estado privado do ator
    for (msg in channel) {                 // mailbox: uma mensagem por vez
        when (msg) {
            is Depositar -> saldo += msg.valor
            is Sacar -> {
                val ok = saldo >= msg.valor
                if (ok) saldo -= msg.valor
                msg.resposta.complete(ok)
            }
            is ConsultarSaldo -> msg.resposta.complete(saldo)
        }
    }
}

// 2) Ator "feito à mão" com Channel (forma recomendada atualmente)
class ContadorAtor(scope: CoroutineScope) {
    private val mailbox = Channel<Pair<String, CompletableDeferred<Int>?>>(Channel.UNLIMITED)
    private var total = 0                  // só a corrotina do ator mexe aqui

    init {
        scope.launch {
            for ((cmd, resposta) in mailbox) {
                when (cmd) {
                    "inc" -> total++
                    "get" -> resposta?.complete(total)
                }
            }
        }
    }

    suspend fun inc() = mailbox.send("inc" to null)
    suspend fun get(): Int = CompletableDeferred<Int>().also { mailbox.send("get" to it) }.await()
    fun fechar() = mailbox.close()
}

fun main() = runBlocking {
    println("1) Ator de conta bancária (actor {})")
    val conta = contaBancariaAtor()

    // 1000 corrotinas em várias threads depositando ao mesmo tempo
    withContext(Dispatchers.Default) {
        List(1000) { launch { conta.send(Depositar(10)) } }.joinAll()
    }
    val saque = CompletableDeferred<Boolean>()
    conta.send(Sacar(3000, saque))
    println("   saque de 3000 aprovado? ${saque.await()}")

    val saldo = CompletableDeferred<Int>()
    conta.send(ConsultarSaldo(saldo))
    println("   saldo final: ${saldo.await()} (esperado 10000 - 3000 = 7000), sem nenhum lock!")
    conta.close()

    println("\n2) Ator manual com Channel")
    val contador = ContadorAtor(this)
    withContext(Dispatchers.Default) {
        List(100) { launch { repeat(100) { contador.inc() } } }.joinAll()
    }
    println("   total = ${contador.get()} (esperado 10000)")
    contador.fechar()

    println("\n3) Pipeline de atores: um ator envia mensagens para outro")
    val impressor = Channel<String>()
    val dobrador = Channel<Int>()
    launch { for (n in dobrador) impressor.send("recebi $n, dobro = ${n * 2}") }
    launch { for (texto in impressor) println("   impressor: $texto") }
    for (i in 1..3) dobrador.send(i)
    dobrador.close()
    delay(100)
    impressor.close()
}
