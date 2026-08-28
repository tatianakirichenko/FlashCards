#!/usr/bin/env node
// flash_cards.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

const DATA_FILE = 'cards.json';

class Card {
    constructor(question, answer, set, correct = 0, wrong = 0) {
        this.question = question;
        this.answer = answer;
        this.set = set || 'Общее';
        this.correct = correct;
        this.wrong = wrong;
    }
}

class FlashCards {
    constructor() {
        this.cards = [];
        this.load();
    }

    load() {
        try {
            if (fs.existsSync(DATA_FILE)) {
                const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
                this.cards = data.map(item => new Card(item.question, item.answer, item.set, item.correct, item.wrong));
            }
        } catch (e) {
            this.cards = [];
        }
    }

    save() {
        fs.writeFileSync(DATA_FILE, JSON.stringify(this.cards, null, 2));
    }

    addCard(question, answer, set) {
        this.cards.push(new Card(question, answer, set));
        this.save();
        console.log(chalk.green(`Карточка добавлена (ID: ${this.cards.length - 1})`));
    }

    removeCard(idx) {
        if (idx < 0 || idx >= this.cards.length) {
            console.log(chalk.red('Неверный ID.'));
            return;
        }
        const removed = this.cards.splice(idx, 1)[0];
        this.save();
        console.log(chalk.yellow(`Карточка '${removed.question}' удалена.`));
    }

    listCards() {
        if (this.cards.length === 0) {
            console.log(chalk.yellow('Нет карточек.'));
            return;
        }
        console.log(chalk.cyan('📚 Список карточек:'));
        for (let i = 0; i < this.cards.length; i++) {
            const c = this.cards[i];
            console.log(`  ${i}. ${c.question} | ${c.answer} | набор: ${c.set} | правильные: ${c.correct}, ошибки: ${c.wrong}`);
        }
    }

    train(cardSet) {
        let cards = this.cards;
        if (cardSet) {
            cards = cards.filter(c => c.set === cardSet);
            if (cards.length === 0) {
                console.log(chalk.red(`Набор '${cardSet}' не найден.`));
                return;
            }
        }
        if (cards.length === 0) {
            console.log(chalk.yellow('Нет карточек для тренировки.'));
            return;
        }
        cards = cards.sort(() => Math.random() - 0.5);
        let correctTotal = 0, wrongTotal = 0;
        console.log(chalk.green(`Начинаем тренировку (набор: ${cardSet || 'все'})`));
        console.log('Введите ответ, или "q" для выхода, или "s" для пропуска.');
        const readline = require('readline').createInterface({
            input: process.stdin,
            output: process.stdout
        });
        const ask = (index) => {
            if (index >= cards.length) {
                readline.close();
                this._finishTraining(correctTotal, wrongTotal);
                return;
            }
            const card = cards[index];
            console.log(chalk.cyan(`Вопрос: ${card.question}`));
            readline.question(chalk.yellow('Ваш ответ: '), (answer) => {
                const userInput = answer.trim();
                if (userInput.toLowerCase() === 'q') {
                    console.log(chalk.magenta('Тренировка прервана.'));
                    readline.close();
                    return;
                }
                if (userInput.toLowerCase() === 's') {
                    console.log(chalk.blue(`Правильный ответ: ${card.answer}`));
                    ask(index + 1);
                    return;
                }
                if (userInput.toLowerCase() === card.answer.toLowerCase()) {
                    console.log(chalk.green('✅ Правильно!'));
                    card.correct++;
                    correctTotal++;
                } else {
                    console.log(chalk.red(`❌ Неправильно. Правильный ответ: ${card.answer}`));
                    card.wrong++;
                    wrongTotal++;
                }
                this.save();
                ask(index + 1);
            });
        };
        ask(0);
    }

    _finishTraining(correct, wrong) {
        const total = correct + wrong;
        if (total > 0) {
            console.log(chalk.magenta(`Тренировка завершена. Правильных: ${correct}, ошибок: ${wrong}, точность: ${(correct / total * 100).toFixed(1)}%`));
        }
    }

    stats() {
        if (this.cards.length === 0) {
            console.log(chalk.yellow('Нет данных.'));
            return;
        }
        const totalCorrect = this.cards.reduce((s, c) => s + c.correct, 0);
        const totalWrong = this.cards.reduce((s, c) => s + c.wrong, 0);
        const total = totalCorrect + totalWrong;
        console.log(chalk.cyan('📊 Статистика:'));
        console.log(`  Всего карточек: ${this.cards.length}`);
        console.log(`  Правильных ответов: ${totalCorrect}`);
        console.log(`  Неправильных ответов: ${totalWrong}`);
        if (total > 0) {
            console.log(`  Точность: ${(totalCorrect / total * 100).toFixed(1)}%`);
        }
        const weak = this.cards.filter(c => c.correct + c.wrong > 0 && (c.correct / (c.correct + c.wrong) < 0.5));
        if (weak.length > 0) {
            console.log(chalk.yellow('Слабые карточки (точность < 50%):'));
            for (const c of weak) {
                const acc = c.correct / (c.correct + c.wrong) * 100;
                console.log(`  ${c.question} | точность: ${acc.toFixed(1)}%`);
            }
        }
    }

    exportJson(filename) {
        fs.writeFileSync(filename, JSON.stringify(this.cards, null, 2));
        console.log(chalk.green(`Экспортировано в ${filename} (JSON)`));
    }

    importJson(filename) {
        try {
            const data = JSON.parse(fs.readFileSync(filename, 'utf8'));
            data.forEach(item => this.cards.push(new Card(item.question, item.answer, item.set, item.correct, item.wrong)));
            this.save();
            console.log(chalk.green(`Импортировано ${data.length} карточек из ${filename}`));
        } catch (e) {
            console.log(chalk.red(`Ошибка импорта: ${e.message}`));
        }
    }

    exportCsv(filename) {
        const header = 'question,answer,set,correct,wrong\n';
        const rows = this.cards.map(c => `${c.question},${c.answer},${c.set},${c.correct},${c.wrong}`).join('\n');
        fs.writeFileSync(filename, header + rows);
        console.log(chalk.green(`Экспортировано в ${filename} (CSV)`));
    }
}

program
    .option('--add <question>', 'Добавить карточку (вопрос)')
    .option('--answer <answer>', 'Ответ на карточку')
    .option('--set <set>', 'Набор (категория)', 'Общее')
    .option('--remove <id>', 'Удалить карточку по ID', parseInt)
    .option('--list', 'Показать все карточки')
    .option('--train [set]', 'Начать тренировку (опционально указать набор)')
    .option('--stats', 'Показать статистику')
    .option('--export-json <file>', 'Экспорт в JSON')
    .option('--import-json <file>', 'Импорт из JSON')
    .option('--export-csv <file>', 'Экспорт в CSV')
    .parse(process.argv);

const opts = program.opts();
const fc = new FlashCards();

if (opts.add) {
    if (!opts.answer) {
        console.error(chalk.red('Для добавления карточки требуется --answer'));
        process.exit(1);
    }
    fc.addCard(opts.add, opts.answer, opts.set);
} else if (opts.remove !== undefined) {
    fc.removeCard(opts.remove);
} else if (opts.list) {
    fc.listCards();
} else if (opts.train !== undefined) {
    fc.train(opts.train);
} else if (opts.stats) {
    fc.stats();
} else if (opts.exportJson) {
    fc.exportJson(opts.exportJson);
} else if (opts.importJson) {
    fc.importJson(opts.importJson);
} else if (opts.exportCsv) {
    fc.exportCsv(opts.exportCsv);
} else {
    program.help();
}
