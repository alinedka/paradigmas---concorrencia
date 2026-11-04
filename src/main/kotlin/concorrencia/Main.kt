package concorrencia

import concorrencia.parte1.threads.main as threads
import concorrencia.parte1.memoria.main as memoria
import concorrencia.parte1.sincronizacao.main as sincronizacao
import concorrencia.parte2.corrotinas.main as corrotinas
import concorrencia.parte2.eventloop.main as eventLoop
import concorrencia.parte2.promises.main as promises
import concorrencia.parte2.atores.main as atores
import concorrencia.parte2.outras.main as outras

/** Roda todos os exemplos em sequência. */
fun main() {
    val exemplos: List<Pair<String, () -> Unit>> = listOf(
        "PARTE 1.1 - Criando threads" to ::threads,
        "PARTE 1.2 - Memória compartilhada" to ::memoria,
        "PARTE 1.3 - Sincronização" to ::sincronizacao,
        "PARTE 2.1 - Corrotinas" to { corrotinas() },
        "PARTE 2.2 - Event loop" to ::eventLoop,
        "PARTE 2.3 - Promises" to { promises() },
        "PARTE 2.4 - Atores" to { atores() },
        "PARTE 2.5 - Outras formas" to { outras() },
    )
    for ((titulo, executar) in exemplos) {
        println("\n" + "=".repeat(60) + "\n$titulo\n" + "=".repeat(60))
        executar()
    }
}
