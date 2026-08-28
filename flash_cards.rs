// flash_cards.rs
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::{self, Write, BufRead};
use rand::seq::SliceRandom;
use colored::*;

const DATA_FILE: &str = "cards.json";

#[derive(Serialize, Deserialize, Clone)]
struct Card {
    question: String,
    answer: String,
    set: String,
    correct: u32,
    wrong: u32,
}

struct FlashCards {
    cards: Vec<Card>,
}

impl FlashCards {
    fn new() -> Self {
        let mut fc = FlashCards { cards: Vec::new() };
        fc.load();
        fc
    }

    fn load(&mut self) {
        if let Ok(data) = fs::read_to_string(DATA_FILE) {
            if let Ok(cards) = serde_json::from_str(&data) {
                self.cards = cards;
                return;
            }
        }
        self.cards = Vec::new();
    }

    fn save(&self) {
        let json = serde_json::to_string_pretty(&self.cards).unwrap();
        fs::write(DATA_FILE, json).unwrap();
    }

    fn add_card(&mut self, question: &str, answer: &str, set: &str) {
        self.cards.push(Card {
            question: question.to_string(),
            answer: answer.to_string(),
            set: set.to_string(),
            correct: 0,
            wrong: 0,
        });
        self.save();
        println!("{}", format!("Карточка добавлена (ID: {})", self.cards.len() - 1).green());
    }

    fn remove_card(&mut self, idx: usize) {
        if idx >= self.cards.len() {
            println!("{}", "Неверный ID.".red());
            return;
        }
        let removed = self.cards.remove(idx);
        self.save();
        println!("{}", format!("Карточка '{}' удалена.", removed.question).yellow());
    }

    fn list_cards(&self) {
        if self.cards.is_empty() {
            println!("{}", "Нет карточек.".yellow());
            return;
        }
        println!("{}", "📚 Список карточек:".cyan());
        for (i, c) in self.cards.iter().enumerate() {
            println!("  {}. {} | {} | набор: {} | правильные: {}, ошибки: {}",
                i, c.question, c.answer, c.set, c.correct, c.wrong);
        }
    }

    fn train(&mut self, set: Option<&str>) {
        let mut cards = self.cards.clone();
        if let Some(s) = set {
            cards.retain(|c| c.set == s);
            if cards.is_empty() {
                println!("{}", format!("Набор '{}' не найден.", s).red());
                return;
            }
        }
        if cards.is_empty() {
            println!("{}", "Нет карточек для тренировки.".yellow());
            return;
        }
        let mut rng = rand::thread_rng();
        cards.shuffle(&mut rng);
        let mut correct_total = 0;
        let mut wrong_total = 0;
        println!("{}", format!("Начинаем тренировку (набор: {})", set.unwrap_or("все")).green());
        println!("Введите ответ, или 'q' для выхода, или 's' для пропуска.");
        let stdin = io::stdin();
        let mut stdout = io::stdout();
        for mut card in cards {
            println!("{}", format!("Вопрос: {}", card.question).cyan());
            print!("{}", "Ваш ответ: ".yellow());
            stdout.flush().unwrap();
            let mut input = String::new();
            stdin.read_line(&mut input).unwrap();
            let input = input.trim();
            if input == "q" {
                println!("{}", "Тренировка прервана.".magenta());
                break;
            }
            if input == "s" {
                println!("{}", format!("Правильный ответ: {}", card.answer).blue());
                continue;
            }
            if input.to_lowercase() == card.answer.to_lowercase() {
                println!("{}", "✅ Правильно!".green());
                card.correct += 1;
                correct_total += 1;
            } else {
                println!("{}", format!("❌ Неправильно. Правильный ответ: {}", card.answer).red());
                card.wrong += 1;
                wrong_total += 1;
            }
            // Обновляем карточку в основном списке
            if let Some(idx) = self.cards.iter().position(|c| c.question == card.question && c.answer == card.answer) {
                self.cards[idx] = card;
            }
            self.save();
        }
        let total = correct_total + wrong_total;
        if total > 0 {
            println!("{}", format!("Тренировка завершена. Правильных: {}, ошибок: {}, точность: {:.1}%",
                correct_total, wrong_total, (correct_total as f64 / total as f64) * 100.0).magenta());
        }
    }

    fn stats(&self) {
        if self.cards.is_empty() {
            println!("{}", "Нет данных.".yellow());
            return;
        }
        let total_correct: u32 = self.cards.iter().map(|c| c.correct).sum();
        let total_wrong: u32 = self.cards.iter().map(|c| c.wrong).sum();
        let total = total_correct + total_wrong;
        println!("{}", "📊 Статистика:".cyan());
        println!("  Всего карточек: {}", self.cards.len());
        println!("  Правильных ответов: {}", total_correct);
        println!("  Неправильных ответов: {}", total_wrong);
        if total > 0 {
            println!("  Точность: {:.1}%", (total_correct as f64 / total as f64) * 100.0);
        }
        let weak: Vec<&Card> = self.cards.iter()
            .filter(|c| c.correct + c.wrong > 0 && (c.correct as f64 / (c.correct + c.wrong) as f64) < 0.5)
            .collect();
        if !weak.is_empty() {
            println!("{}", "Слабые карточки (точность < 50%):".yellow());
            for c in weak {
                let acc = c.correct as f64 / (c.correct + c.wrong) as f64 * 100.0;
                println!("  {} | точность: {:.1}%", c.question, acc);
            }
        }
    }

    fn export_json(&self, filename: &str) {
        let json = serde_json::to_string_pretty(&self.cards).unwrap();
        fs::write(filename, json).unwrap();
        println!("{}", format!("Экспортировано в {} (JSON)", filename).green());
    }

    fn import_json(&mut self, filename: &str) {
        if let Ok(data) = fs::read_to_string(filename) {
            if let Ok(mut imported) = serde_json::from_str::<Vec<Card>>(&data) {
                self.cards.append(&mut imported);
                self.save();
                println!("{}", format!("Импортировано {} карточек из {}", imported.len(), filename).green());
                return;
            }
        }
        println!("{}", "Ошибка импорта.".red());
    }

    fn export_csv(&self, filename: &str) {
        let mut wtr = csv::Writer::from_path(filename).unwrap();
        wtr.write_record(&["question", "answer", "set", "correct", "wrong"]).unwrap();
        for c in &self.cards {
            wtr.write_record(&[&c.question, &c.answer, &c.set, &c.correct.to_string(), &c.wrong.to_string()]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Экспортировано в {} (CSV)", filename).green());
    }
}

fn main() {
    let matches = App::new("Flash Cards")
        .arg(Arg::with_name("add").long("add").takes_value(true).help("Добавить карточку (вопрос)"))
        .arg(Arg::with_name("answer").long("answer").takes_value(true).help("Ответ на карточку"))
        .arg(Arg::with_name("set").long("set").takes_value(true).default_value("Общее").help("Набор (категория)"))
        .arg(Arg::with_name("remove").long("remove").takes_value(true).help("Удалить карточку по ID"))
        .arg(Arg::with_name("list").long("list").help("Показать все карточки"))
        .arg(Arg::with_name("train").long("train").takes_value(true).help("Начать тренировку (указать набор)"))
        .arg(Arg::with_name("stats").long("stats").help("Показать статистику"))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true).help("Экспорт в JSON"))
        .arg(Arg::with_name("import-json").long("import-json").takes_value(true).help("Импорт из JSON"))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true).help("Экспорт в CSV"))
        .get_matches();

    let mut fc = FlashCards::new();

    if let Some(question) = matches.value_of("add") {
        let answer = matches.value_of("answer").expect("--answer required");
        let set = matches.value_of("set").unwrap();
        fc.add_card(question, answer, set);
    } else if let Some(idx_str) = matches.value_of("remove") {
        let idx = idx_str.parse::<usize>().expect("Неверный ID");
        fc.remove_card(idx);
    } else if matches.is_present("list") {
        fc.list_cards();
    } else if let Some(set) = matches.value_of("train") {
        fc.train(Some(set));
    } else if matches.is_present("stats") {
        fc.stats();
    } else if let Some(file) = matches.value_of("export-json") {
        fc.export_json(file);
    } else if let Some(file) = matches.value_of("import-json") {
        fc.import_json(file);
    } else if let Some(file) = matches.value_of("export-csv") {
        fc.export_csv(file);
    } else {
        println!("Используйте --help для справки.");
    }
}
