

#!/usr/bin/env python3
# flash_cards.py
import argparse
import json
import csv
import os
import sys
import random
from datetime import datetime
from colorama import init, Fore, Style

init(autoreset=True)

DATA_FILE = "cards.json"

class Card:
    def __init__(self, question, answer, card_set="Общее", correct=0, wrong=0):
        self.question = question
        self.answer = answer
        self.set = card_set
        self.correct = correct
        self.wrong = wrong

    def to_dict(self):
        return {
            "question": self.question,
            "answer": self.answer,
            "set": self.set,
            "correct": self.correct,
            "wrong": self.wrong
        }

    @classmethod
    def from_dict(cls, data):
        return cls(data["question"], data["answer"], data.get("set", "Общее"),
                   data.get("correct", 0), data.get("wrong", 0))

    def get_id(self, all_cards):
        # Для удобства используем индекс в списке
        pass

class FlashCards:
    def __init__(self):
        self.cards = []
        self.load()

    def load(self):
        if not os.path.exists(DATA_FILE):
            return
        try:
            with open(DATA_FILE, 'r') as f:
                data = json.load(f)
                self.cards = [Card.from_dict(item) for item in data]
        except:
            self.cards = []

    def save(self):
        with open(DATA_FILE, 'w') as f:
            json.dump([c.to_dict() for c in self.cards], f, indent=2)

    def add_card(self, question, answer, card_set="Общее"):
        self.cards.append(Card(question, answer, card_set))
        self.save()
        print(Fore.GREEN + f"Карточка добавлена (ID: {len(self.cards)-1})")

    def remove_card(self, idx):
        if idx < 0 or idx >= len(self.cards):
            print(Fore.RED + "Неверный ID.")
            return
        removed = self.cards.pop(idx)
        self.save()
        print(Fore.YELLOW + f"Карточка '{removed.question}' удалена.")

    def list_cards(self):
        if not self.cards:
            print(Fore.YELLOW + "Нет карточек.")
            return
        print(Fore.CYAN + "📚 Список карточек:")
        for i, c in enumerate(self.cards):
            print(f"  {i}. {c.question} | {c.answer} | набор: {c.set} | правильные: {c.correct}, ошибки: {c.wrong}")

    def train(self, card_set=None):
        cards = self.cards
        if card_set:
            cards = [c for c in cards if c.set == card_set]
            if not cards:
                print(Fore.RED + f"Набор '{card_set}' не найден.")
                return
        if not cards:
            print(Fore.YELLOW + "Нет карточек для тренировки.")
            return
        random.shuffle(cards)
        correct_total = 0
        wrong_total = 0
        print(Fore.GREEN + f"Начинаем тренировку (набор: {card_set if card_set else 'все'})")
        print("Введите ответ, или 'q' для выхода, или 's' для пропуска.")
        for card in cards:
            print(Fore.CYAN + f"Вопрос: {card.question}")
            user_input = input(Fore.YELLOW + "Ваш ответ: " + Style.RESET_ALL).strip()
            if user_input.lower() == 'q':
                print(Fore.MAGENTA + "Тренировка прервана.")
                break
            if user_input.lower() == 's':
                print(Fore.BLUE + f"Правильный ответ: {card.answer}")
                continue
            if user_input.lower() == card.answer.lower():
                print(Fore.GREEN + "✅ Правильно!")
                card.correct += 1
                correct_total += 1
            else:
                print(Fore.RED + f"❌ Неправильно. Правильный ответ: {card.answer}")
                card.wrong += 1
                wrong_total += 1
            self.save()
        total = correct_total + wrong_total
        if total > 0:
            print(Fore.MAGENTA + f"Тренировка завершена. Правильных: {correct_total}, ошибок: {wrong_total}, точность: {correct_total/total*100:.1f}%")

    def stats(self):
        if not self.cards:
            print(Fore.YELLOW + "Нет данных.")
            return
        total_correct = sum(c.correct for c in self.cards)
        total_wrong = sum(c.wrong for c in self.cards)
        total = total_correct + total_wrong
        print(Fore.CYAN + "📊 Статистика:")
        print(f"  Всего карточек: {len(self.cards)}")
        print(f"  Правильных ответов: {total_correct}")
        print(f"  Неправильных ответов: {total_wrong}")
        if total > 0:
            print(f"  Точность: {total_correct/total*100:.1f}%")
        # Карточки с низкой точностью
        weak = [c for c in self.cards if c.correct + c.wrong > 0 and (c.correct / (c.correct + c.wrong) < 0.5)]
        if weak:
            print(Fore.YELLOW + "Слабые карточки (точность < 50%):")
            for c in weak:
                acc = c.correct / (c.correct + c.wrong) * 100
                print(f"  {c.question} | точность: {acc:.1f}%")

    def export_json(self, filename):
        with open(filename, 'w') as f:
            json.dump([c.to_dict() for c in self.cards], f, indent=2)
        print(Fore.GREEN + f"Экспортировано в {filename} (JSON)")

    def import_json(self, filename):
        try:
            with open(filename, 'r') as f:
                data = json.load(f)
                for item in data:
                    self.cards.append(Card.from_dict(item))
            self.save()
            print(Fore.GREEN + f"Импортировано {len(data)} карточек из {filename}")
        except Exception as e:
            print(Fore.RED + f"Ошибка импорта: {e}")

    def export_csv(self, filename):
        with open(filename, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["question", "answer", "set", "correct", "wrong"])
            for c in self.cards:
                writer.writerow([c.question, c.answer, c.set, c.correct, c.wrong])
        print(Fore.GREEN + f"Экспортировано в {filename} (CSV)")

def main():
    parser = argparse.ArgumentParser(description="Тренер памяти (карточки)")
    parser.add_argument("--add", help="Добавить карточку (вопрос)")
    parser.add_argument("--answer", help="Ответ на карточку")
    parser.add_argument("--set", default="Общее", help="Набор (категория)")
    parser.add_argument("--remove", type=int, help="Удалить карточку по ID")
    parser.add_argument("--list", action="store_true", help="Показать все карточки")
    parser.add_argument("--train", help="Начать тренировку (указать набор, или без аргумента - все)")
    parser.add_argument("--stats", action="store_true", help="Показать статистику")
    parser.add_argument("--export-json", help="Экспорт в JSON")
    parser.add_argument("--import-json", help="Импорт из JSON")
    parser.add_argument("--export-csv", help="Экспорт в CSV")
    args = parser.parse_args()

    fc = FlashCards()

    if args.add:
        if not args.answer:
            print(Fore.RED + "Для добавления карточки требуется --answer")
            sys.exit(1)
        fc.add_card(args.add, args.answer, args.set)
    elif args.remove is not None:
        fc.remove_card(args.remove)
    elif args.list:
        fc.list_cards()
    elif args.train is not None:
        fc.train(args.train if args.train != "" else None)
    elif args.stats:
        fc.stats()
    elif args.export_json:
        fc.export_json(args.export_json)
    elif args.import_json:
        fc.import_json(args.import_json)
    elif args.export_csv:
        fc.export_csv(args.export_csv)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
