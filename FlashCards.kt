// FlashCards.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

class FlashCards {
    @Parameter(names = ["--add"])
    private var addQuestion: String? = null

    @Parameter(names = ["--answer"])
    private var answer: String? = null

    @Parameter(names = ["--set"])
    private var set: String = "Общее"

    @Parameter(names = ["--remove"])
    private var removeId: Int? = null

    @Parameter(names = ["--list"])
    private var list: Boolean = false

    @Parameter(names = ["--train"])
    private var trainSet: String? = null

    @Parameter(names = ["--stats"])
    private var stats: Boolean = false

    @Parameter(names = ["--export-json"])
    private var exportJson: String? = null

    @Parameter(names = ["--import-json"])
    private var importJson: String? = null

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    data class Card(val question: String, val answer: String, val set: String, var correct: Int = 0, var wrong: Int = 0)

    private val dataFile = "cards.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<MutableList<Card>>() {}.type
    private val cards = mutableListOf<Card>()
    private val scanner = Scanner(System.`in`)

    private fun load() {
        val f = File(dataFile)
        if (!f.exists()) return
        try {
            val json = f.readText()
            val list = gson.fromJson<MutableList<Card>>(json, type)
            cards.addAll(list)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun save() {
        val json = gson.toJson(cards)
        File(dataFile).writeText(json)
    }

    private fun addCard(question: String, answer: String, set: String) {
        cards.add(Card(question, answer, set))
        save()
        println("\u001B[32mКарточка добавлена (ID: ${cards.size - 1})\u001B[0m")
    }

    private fun removeCard(idx: Int) {
        if (idx !in cards.indices) {
            println("\u001B[31mНеверный ID.\u001B[0m")
            return
        }
        val removed = cards.removeAt(idx)
        save()
        println("\u001B[33mКарточка '${removed.question}' удалена.\u001B[0m")
    }

    private fun listCards() {
        if (cards.isEmpty()) {
            println("\u001B[33mНет карточек.\u001B[0m")
            return
        }
        println("\u001B[36m📚 Список карточек:\u001B[0m")
        cards.forEachIndexed { i, c ->
            println("  $i. ${c.question} | ${c.answer} | набор: ${c.set} | правильные: ${c.correct}, ошибки: ${c.wrong}")
        }
    }

    private fun train(set: String?) {
        var trainCards = cards
        if (set != null && set.isNotEmpty()) {
            trainCards = cards.filter { it.set == set }.toMutableList()
            if (trainCards.isEmpty()) {
                println("\u001B[31mНабор '$set' не найден.\u001B[0m")
                return
            }
        }
        if (trainCards.isEmpty()) {
            println("\u001B[33mНет карточек для тренировки.\u001B[0m")
            return
        }
        trainCards.shuffle()
        var correctTotal = 0
        var wrongTotal = 0
        println("\u001B[32mНачинаем тренировку (набор: ${set ?: "все"})\u001B[0m")
        println("Введите ответ, или 'q' для выхода, или 's' для пропуска.")
        for (c in trainCards) {
            println("\u001B[36mВопрос: ${c.question}\u001B[0m")
            print("\u001B[33mВаш ответ: \u001B[0m")
            val input = scanner.nextLine().trim()
            if (input.equals("q", ignoreCase = true)) {
                println("\u001B[35mТренировка прервана.\u001B[0m")
                break
            }
            if (input.equals("s", ignoreCase = true)) {
                println("\u001B[34mПравильный ответ: ${c.answer}\u001B[0m")
                continue
            }
            if (input.equals(c.answer, ignoreCase = true)) {
                println("\u001B[32m✅ Правильно!\u001B[0m")
                c.correct++
                correctTotal++
            } else {
                println("\u001B[31m❌ Неправильно. Правильный ответ: ${c.answer}\u001B[0m")
                c.wrong++
                wrongTotal++
            }
            // Обновляем в основном списке
            val idx = cards.indexOfFirst { it.question == c.question && it.answer == c.answer }
            if (idx != -1) cards[idx] = c
            save()
        }
        val total = correctTotal + wrongTotal
        if (total > 0) {
            println("\u001B[35mТренировка завершена. Правильных: $correctTotal, ошибок: $wrongTotal, точность: ${"%.1f".format(correctTotal.toDouble() / total * 100)}%\u001B[0m")
        }
    }

    private fun stats() {
        if (cards.isEmpty()) {
            println("\u001B[33mНет данных.\u001B[0m")
            return
        }
        val totalCorrect = cards.sumOf { it.correct }
        val totalWrong = cards.sumOf { it.wrong }
        val total = totalCorrect + totalWrong
        println("\u001B[36m📊 Статистика:\u001B[0m")
        println("  Всего карточек: ${cards.size}")
        println("  Правильных ответов: $totalCorrect")
        println("  Неправильных ответов: $totalWrong")
        if (total > 0) {
            println("  Точность: ${"%.1f".format(totalCorrect.toDouble() / total * 100)}%")
        }
        val weak = cards.filter { it.correct + it.wrong > 0 && it.correct.toDouble() / (it.correct + it.wrong) < 0.5 }
        if (weak.isNotEmpty()) {
            println("\u001B[33mСлабые карточки (точность < 50%):\u001B[0m")
            weak.forEach {
                val acc = it.correct.toDouble() / (it.correct + it.wrong) * 100
                println("  ${it.question} | точность: ${"%.1f".format(acc)}%")
            }
        }
    }

    private fun exportJson(filename: String) {
        val json = gson.toJson(cards)
        File(filename).writeText(json)
        println("\u001B[32mЭкспортировано в $filename (JSON)\u001B[0m")
    }

    private fun importJson(filename: String) {
        try {
            val json = File(filename).readText()
            val imported = gson.fromJson<MutableList<Card>>(json, type) ?: mutableListOf()
            cards.addAll(imported)
            save()
            println("\u001B[32mИмпортировано ${imported.size} карточек из $filename\u001B[0m")
        } catch (e: Exception) {
            println("\u001B[31mОшибка импорта: ${e.message}\u001B[0m")
        }
    }

    private fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("question,answer,set,correct,wrong")
            cards.forEach { pw.println("${it.question},${it.answer},${it.set},${it.correct},${it.wrong}") }
        }
        println("\u001B[32mЭкспортировано в $filename (CSV)\u001B[0m")
    }

    fun run() {
        load()
        when {
            addQuestion != null -> {
                if (answer == null) {
                    System.err.println("\u001B[31mДля добавления карточки требуется --answer\u001B[0m")
                    System.exit(1)
                }
                addCard(addQuestion!!, answer!!, set)
            }
            removeId != null -> removeCard(removeId!!)
            list -> listCards()
            trainSet != null -> train(trainSet)
            stats -> stats()
            exportJson != null -> exportJson(exportJson!!)
            importJson != null -> importJson(importJson!!)
            exportCsv != null -> exportCsv(exportCsv!!)
            else -> println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val fc = FlashCards()
    JCommander.newBuilder().addObject(fc).build().parse(*args)
    fc.run()
}
