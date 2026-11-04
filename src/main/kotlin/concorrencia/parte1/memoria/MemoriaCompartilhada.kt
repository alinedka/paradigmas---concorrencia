package concorrencia.parte1.memoria

import kotlin.concurrent.thread

/**
 * PARTE 1.2 - COMO É O COMPARTILHAMENTO DE MEMÓRIA ENTRE AS THREADS?
 *
 * Modelo de memória compartilhada (Java Memory Model):
 *  - Todas as threads do processo enxergam o MESMO heap (objetos, campos, arrays).
 *  - Cada thread tem a SUA pilha (variáveis locais e parâmetros são privados).
 *  - Sem sincronização, não há garantia de ATOMICIDADE nem de VISIBILIDADE:
 *      * condição de corrida (race condition) em operações como contador++
 *      * uma thread pode não "ver" o valor escrito por outra (cache de CPU)
 *  - @Volatile garante visibilidade (mas não atomicidade).
 *  - ThreadLocal dá a cada thread sua própria cópia de uma variável.
 */

// Objeto no heap -> compartilhado por todas as threads
var contadorCompartilhado = 0

// @Volatile: escritas ficam visíveis imediatamente para as outras threads
@Volatile
var executando = true

// Cada thread terá a sua cópia
val idPorThread: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

fun main() {
    // 1) CONDIÇÃO DE CORRIDA
    // contador++ é na verdade 3 operações: ler, somar, escrever.
    val threads = (1..4).map {
        thread {
            repeat(100_000) { contadorCompartilhado++ }
        }
    }
    threads.forEach { it.join() }
    println("1) Esperado: 400000 | Obtido: $contadorCompartilhado  <- valores se perderam (race condition)")

    // 2) VARIÁVEIS LOCAIS SÃO PRIVADAS (ficam na pilha de cada thread)
    val t = (1..3).map { n ->
        thread {
            var local = 0                // cada thread tem o seu 'local'
            repeat(1000) { local++ }
            println("2) Thread $n: variável local = $local (sempre 1000, sem interferência)")
        }
    }
    t.forEach { it.join() }

    // 3) VISIBILIDADE COM @Volatile
    // Sem @Volatile a thread 'trabalhadora' poderia nunca enxergar executando = false.
    val trabalhadora = thread {
        var voltas = 0L
        while (executando) voltas++
        println("3) Trabalhadora viu executando=false e parou após $voltas voltas")
    }
    Thread.sleep(100)
    executando = false
    trabalhadora.join()

    // 4) ThreadLocal: mesma variável, valor diferente por thread
    val tl = (1..3).map { n ->
        thread {
            idPorThread.set(n * 10)
            Thread.sleep(50)
            println("4) Thread $n vê idPorThread = ${idPorThread.get()}")
        }
    }
    tl.forEach { it.join() }
    println("4) Thread main vê idPorThread = ${idPorThread.get()} (valor inicial, não foi afetada)")

    // 5) Objetos imutáveis (val + data class) são seguros para compartilhar
    data class Config(val url: String, val tentativas: Int)
    val config = Config("https://exemplo.com", 3)
    (1..2).map { n -> thread { println("5) Thread $n lê $config com segurança (imutável)") } }
        .forEach { it.join() }
}
