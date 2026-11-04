package concorrencia.parte1.sincronizacao

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * PARTE 1.3 - COMO SE FAZ SINCRONIZAÇÃO DE THREADS?
 *
 * Kotlin não tem as palavras-chave 'synchronized'/'volatile' do Java;
 * no lugar usa a função synchronized(lock) {} e as anotações
 * @Synchronized / @Volatile. Também usa tudo de java.util.concurrent.
 */

const val N_THREADS = 4
const val INCREMENTOS = 100_000

// Executa 'acao' INCREMENTOS vezes em N_THREADS threads e espera todas.
fun rodarEmParalelo(acao: () -> Unit) {
    (1..N_THREADS).map { thread { repeat(INCREMENTOS) { acao() } } }.forEach { it.join() }
}

// a) bloco synchronized (monitor / lock intrínseco)
object ContadorSync {
    private val lock = Any()
    var valor = 0
        private set
    fun inc() = synchronized(lock) { valor++ }
}

// b) método anotado com @Synchronized (lock no próprio objeto)
class ContadorAnotado {
    var valor = 0
        private set
    @Synchronized fun inc() { valor++ }
}

// c) ReentrantLock explícito (permite tryLock, timeout, fairness)
class ContadorLock {
    private val lock = ReentrantLock()
    var valor = 0
        private set
    fun inc() = lock.withLock { valor++ }   // withLock = lock() + try/finally unlock()
}

// g) Produtor-consumidor com wait/notify (monitor clássico)
class Buffer(private val capacidade: Int) {
    private val fila = ArrayDeque<Int>()
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val lock = Object()   // java.lang.Object: necessário para wait()/notifyAll()

    fun produzir(item: Int) = synchronized(lock) {
        while (fila.size == capacidade) lock.wait()   // espera haver espaço
        fila.addLast(item)
        lock.notifyAll()
    }

    fun consumir(): Int = synchronized(lock) {
        while (fila.isEmpty()) lock.wait()            // espera haver item
        val item = fila.removeFirst()
        lock.notifyAll()
        item
    }
}

fun main() {
    val esperado = N_THREADS * INCREMENTOS
    println("Valor esperado em cada contador: $esperado\n")

    rodarEmParalelo { ContadorSync.inc() }
    println("a) synchronized(lock) {}  -> ${ContadorSync.valor}")

    val c2 = ContadorAnotado()
    rodarEmParalelo { c2.inc() }
    println("b) @Synchronized          -> ${c2.valor}")

    val c3 = ContadorLock()
    rodarEmParalelo { c3.inc() }
    println("c) ReentrantLock          -> ${c3.valor}")

    // d) Variáveis atômicas (CAS - compare-and-swap, sem lock)
    val atomico = AtomicInteger(0)
    rodarEmParalelo { atomico.incrementAndGet() }
    println("d) AtomicInteger          -> ${atomico.get()}")

    // e) Coleções concorrentes
    val mapa = ConcurrentHashMap<String, Int>()
    rodarEmParalelo { mapa.merge("chave", 1, Int::plus) }
    println("e) ConcurrentHashMap      -> ${mapa["chave"]}")

    // f) CountDownLatch: a main espera 3 threads "ficarem prontas"
    println("\nf) CountDownLatch")
    val latch = CountDownLatch(3)
    repeat(3) { i ->
        thread {
            Thread.sleep(100L * (i + 1))
            println("   serviço $i pronto")
            latch.countDown()
        }
    }
    latch.await()
    println("   todos os serviços prontos, main continua")

    // Semaphore: no máximo 2 threads na "região crítica" ao mesmo tempo
    println("\nSemaphore (máx. 2 simultâneas)")
    val semaforo = Semaphore(2)
    val ativos = AtomicInteger(0)
    (1..6).map { i ->
        thread {
            semaforo.acquire()
            try {
                println("   thread $i entrou (ativas agora: ${ativos.incrementAndGet()})")
                Thread.sleep(150)
                ativos.decrementAndGet()
            } finally {
                semaforo.release()
            }
        }
    }.forEach { it.join() }

    // g) Produtor-consumidor com wait/notify
    println("\ng) Produtor-Consumidor com wait()/notifyAll()")
    val buffer = Buffer(capacidade = 2)
    val produtor = thread {
        for (i in 1..5) {
            buffer.produzir(i)
            println("   produziu $i")
        }
    }
    val consumidor = thread {
        repeat(5) {
            Thread.sleep(80)
            println("   consumiu ${buffer.consumir()}")
        }
    }
    produtor.join(); consumidor.join()
}
