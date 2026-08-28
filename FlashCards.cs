// FlashCards.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace FlashCards
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var fc = new FlashCards();
            if (opts.Add != null)
            {
                if (opts.Answer == null)
                {
                    Console.Error.WriteLine("\u001B[31mДля добавления карточки требуется --answer\u001B[0m");
                    return;
                }
                fc.AddCard(opts.Add, opts.Answer, opts.Set);
            }
            else if (opts.Remove.HasValue)
            {
                fc.RemoveCard(opts.Remove.Value);
            }
            else if (opts.List)
            {
                fc.ListCards();
            }
            else if (opts.Train != null)
            {
                fc.Train(opts.Train);
            }
            else if (opts.Stats)
            {
                fc.Stats();
            }
            else if (opts.ExportJson != null)
            {
                fc.ExportJson(opts.ExportJson);
            }
            else if (opts.ImportJson != null)
            {
                fc.ImportJson(opts.ImportJson);
            }
            else if (opts.ExportCsv != null)
            {
                fc.ExportCsv(opts.ExportCsv);
            }
            else
            {
                Console.WriteLine("Используйте --help для справки.");
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--add": opts.Add = args[++i]; break;
                    case "--answer": opts.Answer = args[++i]; break;
                    case "--set": opts.Set = args[++i]; break;
                    case "--remove": opts.Remove = int.Parse(args[++i]); break;
                    case "--list": opts.List = true; break;
                    case "--train": opts.Train = args[++i]; break;
                    case "--stats": opts.Stats = true; break;
                    case "--export-json": opts.ExportJson = args[++i]; break;
                    case "--import-json": opts.ImportJson = args[++i]; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public string Add { get; set; }
            public string Answer { get; set; }
            public string Set { get; set; } = "Общее";
            public int? Remove { get; set; }
            public bool List { get; set; }
            public string Train { get; set; }
            public bool Stats { get; set; }
            public string ExportJson { get; set; }
            public string ImportJson { get; set; }
            public string ExportCsv { get; set; }
        }

        class Card
        {
            public string Question { get; set; }
            public string Answer { get; set; }
            public string Set { get; set; }
            public int Correct { get; set; }
            public int Wrong { get; set; }
        }

        class FlashCards
        {
            private const string DataFile = "cards.json";
            private List<Card> cards = new List<Card>();

            public FlashCards() => Load();

            private void Load()
            {
                try
                {
                    if (File.Exists(DataFile))
                    {
                        string json = File.ReadAllText(DataFile);
                        cards = JsonSerializer.Deserialize<List<Card>>(json) ?? new List<Card>();
                    }
                }
                catch { cards = new List<Card>(); }
            }

            private void Save()
            {
                string json = JsonSerializer.Serialize(cards, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(DataFile, json);
            }

            public void AddCard(string question, string answer, string set)
            {
                cards.Add(new Card { Question = question, Answer = answer, Set = set });
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Карточка добавлена (ID: {cards.Count - 1})");
                Console.ResetColor();
            }

            public void RemoveCard(int idx)
            {
                if (idx < 0 || idx >= cards.Count)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine("Неверный ID.");
                    Console.ResetColor();
                    return;
                }
                var removed = cards[idx];
                cards.RemoveAt(idx);
                Save();
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"Карточка '{removed.Question}' удалена.");
                Console.ResetColor();
            }

            public void ListCards()
            {
                if (cards.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет карточек.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("📚 Список карточек:");
                Console.ResetColor();
                for (int i = 0; i < cards.Count; i++)
                {
                    var c = cards[i];
                    Console.WriteLine($"  {i}. {c.Question} | {c.Answer} | набор: {c.Set} | правильные: {c.Correct}, ошибки: {c.Wrong}");
                }
            }

            public void Train(string set)
            {
                var trainCards = cards.AsEnumerable();
                if (!string.IsNullOrEmpty(set))
                {
                    trainCards = trainCards.Where(c => c.Set == set);
                    if (!trainCards.Any())
                    {
                        Console.ForegroundColor = ConsoleColor.Red;
                        Console.WriteLine($"Набор '{set}' не найден.");
                        Console.ResetColor();
                        return;
                    }
                }
                if (!trainCards.Any())
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет карточек для тренировки.");
                    Console.ResetColor();
                    return;
                }
                trainCards = trainCards.OrderBy(_ => Guid.NewGuid()).ToList();
                int correctTotal = 0, wrongTotal = 0;
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Начинаем тренировку (набор: {set ?? "все"})");
                Console.ResetColor();
                Console.WriteLine("Введите ответ, или 'q' для выхода, или 's' для пропуска.");
                foreach (var c in trainCards)
                {
                    Console.ForegroundColor = ConsoleColor.Cyan;
                    Console.WriteLine($"Вопрос: {c.Question}");
                    Console.ResetColor();
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.Write("Ваш ответ: ");
                    Console.ResetColor();
                    string input = Console.ReadLine().Trim();
                    if (input.Equals("q", StringComparison.OrdinalIgnoreCase))
                    {
                        Console.ForegroundColor = ConsoleColor.Magenta;
                        Console.WriteLine("Тренировка прервана.");
                        Console.ResetColor();
                        break;
                    }
                    if (input.Equals("s", StringComparison.OrdinalIgnoreCase))
                    {
                        Console.ForegroundColor = ConsoleColor.Blue;
                        Console.WriteLine($"Правильный ответ: {c.Answer}");
                        Console.ResetColor();
                        continue;
                    }
                    if (input.Equals(c.Answer, StringComparison.OrdinalIgnoreCase))
                    {
                        Console.ForegroundColor = ConsoleColor.Green;
                        Console.WriteLine("✅ Правильно!");
                        Console.ResetColor();
                        c.Correct++;
                        correctTotal++;
                    }
                    else
                    {
                        Console.ForegroundColor = ConsoleColor.Red;
                        Console.WriteLine($"❌ Неправильно. Правильный ответ: {c.Answer}");
                        Console.ResetColor();
                        c.Wrong++;
                        wrongTotal++;
                    }
                    Save();
                }
                int total = correctTotal + wrongTotal;
                if (total > 0)
                {
                    Console.ForegroundColor = ConsoleColor.Magenta;
                    Console.WriteLine($"Тренировка завершена. Правильных: {correctTotal}, ошибок: {wrongTotal}, точность: {(double)correctTotal / total * 100:F1}%");
                    Console.ResetColor();
                }
            }

            public void Stats()
            {
                if (cards.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет данных.");
                    Console.ResetColor();
                    return;
                }
                int totalCorrect = cards.Sum(c => c.Correct);
                int totalWrong = cards.Sum(c => c.Wrong);
                int total = totalCorrect + totalWrong;
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("📊 Статистика:");
                Console.ResetColor();
                Console.WriteLine($"  Всего карточек: {cards.Count}");
                Console.WriteLine($"  Правильных ответов: {totalCorrect}");
                Console.WriteLine($"  Неправильных ответов: {totalWrong}");
                if (total > 0)
                {
                    Console.WriteLine($"  Точность: {(double)totalCorrect / total * 100:F1}%");
                }
                var weak = cards.Where(c => c.Correct + c.Wrong > 0 && (double)c.Correct / (c.Correct + c.Wrong) < 0.5);
                if (weak.Any())
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Слабые карточки (точность < 50%):");
                    Console.ResetColor();
                    foreach (var c in weak)
                    {
                        double acc = (double)c.Correct / (c.Correct + c.Wrong) * 100;
                        Console.WriteLine($"  {c.Question} | точность: {acc:F1}%");
                    }
                }
            }

            public void ExportJson(string filename)
            {
                string json = JsonSerializer.Serialize(cards, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(filename, json);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (JSON)");
                Console.ResetColor();
            }

            public void ImportJson(string filename)
            {
                try
                {
                    string json = File.ReadAllText(filename);
                    var imported = JsonSerializer.Deserialize<List<Card>>(json) ?? new List<Card>();
                    cards.AddRange(imported);
                    Save();
                    Console.ForegroundColor = ConsoleColor.Green;
                    Console.WriteLine($"Импортировано {imported.Count} карточек из {filename}");
                    Console.ResetColor();
                }
                catch (Exception e)
                {
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine($"Ошибка импорта: {e.Message}");
                    Console.ResetColor();
                }
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine("question,answer,set,correct,wrong");
                foreach (var c in cards)
                    sw.WriteLine($"{c.Question},{c.Answer},{c.Set},{c.Correct},{c.Wrong}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (CSV)");
                Console.ResetColor();
            }
        }
    }
}
