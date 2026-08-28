// FlashCards.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FlashCards {
    private static final String DATA_FILE = "cards.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Card>>(){}.getType();

    @Parameter(names = "--add")
    private String addQuestion;
    @Parameter(names = "--answer")
    private String answer;
    @Parameter(names = "--set")
    private String set = "Общее";
    @Parameter(names = "--remove")
    private Integer removeId;
    @Parameter(names = "--list")
    private boolean list;
    @Parameter(names = "--train")
    private String trainSet;
    @Parameter(names = "--stats")
    private boolean stats;
    @Parameter(names = "--export-json")
    private String exportJson;
    @Parameter(names = "--import-json")
    private String importJson;
    @Parameter(names = "--export-csv")
    private String exportCsv;

    static class Card {
        String question, answer, set;
        int correct, wrong;
        Card(String q, String a, String s, int c, int w) {
            question = q; answer = a; set = s; correct = c; wrong = w;
        }
    }

    private List<Card> cards = new ArrayList<>();
    private Scanner scanner = new Scanner(System.in);

    private void load() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
            cards = GSON.fromJson(json, LIST_TYPE);
        } catch (Exception e) {
            cards = new ArrayList<>();
        }
    }

    private void save() {
        try {
            Files.write(Paths.get(DATA_FILE), GSON.toJson(cards).getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private void addCard(String q, String a, String s) {
        cards.add(new Card(q, a, s, 0, 0));
        save();
        System.out.println("\u001B[32mКарточка добавлена (ID: " + (cards.size()-1) + ")\u001B[0m");
    }

    private void removeCard(int idx) {
        if (idx < 0 || idx >= cards.size()) {
            System.out.println("\u001B[31mНеверный ID.\u001B[0m");
            return;
        }
        Card removed = cards.remove(idx);
        save();
        System.out.println("\u001B[33mКарточка '" + removed.question + "' удалена.\u001B[0m");
    }

    private void listCards() {
        if (cards.isEmpty()) {
            System.out.println("\u001B[33mНет карточек.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36m📚 Список карточек:\u001B[0m");
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            System.out.printf("  %d. %s | %s | набор: %s | правильные: %d, ошибки: %d%n",
                i, c.question, c.answer, c.set, c.correct, c.wrong);
        }
    }

    private void train(String set) {
        List<Card> trainCards = cards;
        if (set != null && !set.isEmpty()) {
            trainCards = new ArrayList<>();
            for (Card c : cards) {
                if (c.set.equals(set)) trainCards.add(c);
            }
            if (trainCards.isEmpty()) {
                System.out.println("\u001B[31mНабор '" + set + "' не найден.\u001B[0m");
                return;
            }
        }
        if (trainCards.isEmpty()) {
            System.out.println("\u001B[33mНет карточек для тренировки.\u001B[0m");
            return;
        }
        Collections.shuffle(trainCards);
        int correctTotal = 0, wrongTotal = 0;
        System.out.println("\u001B[32mНачинаем тренировку (набор: " + (set != null ? set : "все") + ")\u001B[0m");
        System.out.println("Введите ответ, или 'q' для выхода, или 's' для пропуска.");
        for (Card c : trainCards) {
            System.out.println("\u001B[36mВопрос: " + c.question + "\u001B[0m");
            System.out.print("\u001B[33mВаш ответ: \u001B[0m");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("q")) {
                System.out.println("\u001B[35mТренировка прервана.\u001B[0m");
                break;
            }
            if (input.equalsIgnoreCase("s")) {
                System.out.println("\u001B[34mПравильный ответ: " + c.answer + "\u001B[0m");
                continue;
            }
            if (input.equalsIgnoreCase(c.answer)) {
                System.out.println("\u001B[32m✅ Правильно!\u001B[0m");
                c.correct++;
                correctTotal++;
            } else {
                System.out.println("\u001B[31m❌ Неправильно. Правильный ответ: " + c.answer + "\u001B[0m");
                c.wrong++;
                wrongTotal++;
            }
            save();
        }
        int total = correctTotal + wrongTotal;
        if (total > 0) {
            System.out.printf("\u001B[35mТренировка завершена. Правильных: %d, ошибок: %d, точность: %.1f%%\u001B[0m%n",
                correctTotal, wrongTotal, (double)correctTotal/total*100);
        }
    }

    private void stats() {
        if (cards.isEmpty()) {
            System.out.println("\u001B[33mНет данных.\u001B[0m");
            return;
        }
        int totalCorrect = 0, totalWrong = 0;
        for (Card c : cards) {
            totalCorrect += c.correct;
            totalWrong += c.wrong;
        }
        int total = totalCorrect + totalWrong;
        System.out.println("\u001B[36m📊 Статистика:\u001B[0m");
        System.out.println("  Всего карточек: " + cards.size());
        System.out.println("  Правильных ответов: " + totalCorrect);
        System.out.println("  Неправильных ответов: " + totalWrong);
        if (total > 0) {
            System.out.printf("  Точность: %.1f%%%n", (double)totalCorrect/total*100);
        }
        List<Card> weak = new ArrayList<>();
        for (Card c : cards) {
            if (c.correct + c.wrong > 0 && (double)c.correct/(c.correct+c.wrong) < 0.5) {
                weak.add(c);
            }
        }
        if (!weak.isEmpty()) {
            System.out.println("\u001B[33mСлабые карточки (точность < 50%):\u001B[0m");
            for (Card c : weak) {
                double acc = (double)c.correct / (c.correct + c.wrong) * 100;
                System.out.printf("  %s | точность: %.1f%%%n", c.question, acc);
            }
        }
    }

    private void exportJson(String filename) throws IOException {
        Files.write(Paths.get(filename), GSON.toJson(cards).getBytes());
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (JSON)\u001B[0m");
    }

    private void importJson(String filename) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(filename)));
        List<Card> imported = GSON.fromJson(json, LIST_TYPE);
        cards.addAll(imported);
        save();
        System.out.println("\u001B[32mИмпортировано " + imported.size() + " карточек из " + filename + "\u001B[0m");
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("question,answer,set,correct,wrong");
            for (Card c : cards) {
                pw.printf("%s,%s,%s,%d,%d%n", c.question, c.answer, c.set, c.correct, c.wrong);
            }
        }
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (CSV)\u001B[0m");
    }

    public void run() throws Exception {
        load();
        if (addQuestion != null) {
            if (answer == null) {
                System.err.println("\u001B[31mДля добавления карточки требуется --answer\u001B[0m");
                System.exit(1);
            }
            addCard(addQuestion, answer, set);
        } else if (removeId != null) {
            removeCard(removeId);
        } else if (list) {
            listCards();
        } else if (trainSet != null) {
            train(trainSet);
        } else if (stats) {
            stats();
        } else if (exportJson != null) {
            exportJson(exportJson);
        } else if (importJson != null) {
            importJson(importJson);
        } else if (exportCsv != null) {
            exportCsv(exportCsv);
        } else {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        FlashCards fc = new FlashCards();
        JCommander.newBuilder().addObject(fc).build().parse(args);
        fc.run();
    }
}
