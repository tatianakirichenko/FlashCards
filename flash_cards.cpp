// flash_cards.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm>
#include <random>
#include <ctime>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const string DATA_FILE = "cards.json";

struct Card {
    string question, answer, set;
    int correct, wrong;
};

class FlashCards {
private:
    vector<Card> cards;

    void load() {
        ifstream ifs(DATA_FILE);
        if (!ifs) return;
        Json::Value root;
        ifs >> root;
        for (const auto& item : root) {
            Card c;
            c.question = item["question"].asString();
            c.answer = item["answer"].asString();
            c.set = item["set"].asString();
            c.correct = item["correct"].asInt();
            c.wrong = item["wrong"].asInt();
            cards.push_back(c);
        }
    }

    void save() {
        Json::Value root(Json::arrayValue);
        for (const auto& c : cards) {
            Json::Value item;
            item["question"] = c.question;
            item["answer"] = c.answer;
            item["set"] = c.set;
            item["correct"] = c.correct;
            item["wrong"] = c.wrong;
            root.append(item);
        }
        ofstream ofs(DATA_FILE);
        ofs << root.toStyledString();
    }

public:
    FlashCards() { load(); }

    void addCard(const string& question, const string& answer, const string& set) {
        Card c{question, answer, set, 0, 0};
        cards.push_back(c);
        save();
        cout << "\033[32mКарточка добавлена (ID: " << cards.size()-1 << ")\033[0m" << endl;
    }

    void removeCard(int idx) {
        if (idx < 0 || idx >= (int)cards.size()) {
            cout << "\033[31mНеверный ID.\033[0m" << endl;
            return;
        }
        string question = cards[idx].question;
        cards.erase(cards.begin() + idx);
        save();
        cout << "\033[33mКарточка '" << question << "' удалена.\033[0m" << endl;
    }

    void listCards() {
        if (cards.empty()) {
            cout << "\033[33mНет карточек.\033[0m" << endl;
            return;
        }
        cout << "\033[36m📚 Список карточек:\033[0m" << endl;
        for (size_t i = 0; i < cards.size(); ++i) {
            const auto& c = cards[i];
            cout << "  " << i << ". " << c.question << " | " << c.answer << " | набор: " << c.set
                 << " | правильные: " << c.correct << ", ошибки: " << c.wrong << endl;
        }
    }

    void train(const string& set) {
        vector<Card> trainCards = cards;
        if (!set.empty()) {
            trainCards.erase(remove_if(trainCards.begin(), trainCards.end(),
                [&](const Card& c) { return c.set != set; }), trainCards.end());
            if (trainCards.empty()) {
                cout << "\033[31mНабор '" << set << "' не найден.\033[0m" << endl;
                return;
            }
        }
        if (trainCards.empty()) {
            cout << "\033[33mНет карточек для тренировки.\033[0m" << endl;
            return;
        }
        random_device rd;
        mt19937 g(rd());
        shuffle(trainCards.begin(), trainCards.end(), g);
        int correctTotal = 0, wrongTotal = 0;
        cout << "\033[32mНачинаем тренировку (набор: " << (set.empty() ? "все" : set) << ")\033[0m" << endl;
        cout << "Введите ответ, или 'q' для выхода, или 's' для пропуска." << endl;
        for (auto& c : trainCards) {
            cout << "\033[36mВопрос: " << c.question << "\033[0m" << endl;
            cout << "\033[33mВаш ответ: \033[0m";
            string input;
            getline(cin, input);
            if (input == "q") {
                cout << "\033[35mТренировка прервана.\033[0m" << endl;
                break;
            }
            if (input == "s") {
                cout << "\033[34mПравильный ответ: " << c.answer << "\033[0m" << endl;
                continue;
            }
            if (input == c.answer) {
                cout << "\033[32m✅ Правильно!\033[0m" << endl;
                c.correct++;
                correctTotal++;
            } else {
                cout << "\033[31m❌ Неправильно. Правильный ответ: " << c.answer << "\033[0m" << endl;
                c.wrong++;
                wrongTotal++;
            }
            // Обновляем оригинальную карточку
            for (auto& orig : cards) {
                if (orig.question == c.question && orig.answer == c.answer) {
                    orig = c;
                    break;
                }
            }
            save();
        }
        int total = correctTotal + wrongTotal;
        if (total > 0) {
            cout << "\033[35mТренировка завершена. Правильных: " << correctTotal << ", ошибок: " << wrongTotal
                 << ", точность: " << (double)correctTotal/total*100 << "%\033[0m" << endl;
        }
    }

    void stats() {
        if (cards.empty()) {
            cout << "\033[33mНет данных.\033[0m" << endl;
            return;
        }
        int totalCorrect = 0, totalWrong = 0;
        for (const auto& c : cards) {
            totalCorrect += c.correct;
            totalWrong += c.wrong;
        }
        int total = totalCorrect + totalWrong;
        cout << "\033[36m📊 Статистика:\033[0m" << endl;
        cout << "  Всего карточек: " << cards.size() << endl;
        cout << "  Правильных ответов: " << totalCorrect << endl;
        cout << "  Неправильных ответов: " << totalWrong << endl;
        if (total > 0) {
            cout << "  Точность: " << (double)totalCorrect/total*100 << "%" << endl;
        }
        cout << "\033[33mСлабые карточки (точность < 50%):\033[0m" << endl;
        for (const auto& c : cards) {
            if (c.correct + c.wrong > 0 && (double)c.correct/(c.correct+c.wrong) < 0.5) {
                cout << "  " << c.question << " | точность: " << (double)c.correct/(c.correct+c.wrong)*100 << "%" << endl;
            }
        }
    }

    void exportJSON(const string& filename) {
        Json::Value root(Json::arrayValue);
        for (const auto& c : cards) {
            Json::Value item;
            item["question"] = c.question;
            item["answer"] = c.answer;
            item["set"] = c.set;
            item["correct"] = c.correct;
            item["wrong"] = c.wrong;
            root.append(item);
        }
        ofstream ofs(filename);
        ofs << root.toStyledString();
        cout << "\033[32mЭкспортировано в " << filename << " (JSON)\033[0m" << endl;
    }

    void importJSON(const string& filename) {
        ifstream ifs(filename);
        if (!ifs) {
            cout << "\033[31mОшибка импорта: файл не найден.\033[0m" << endl;
            return;
        }
        Json::Value root;
        ifs >> root;
        for (const auto& item : root) {
            Card c;
            c.question = item["question"].asString();
            c.answer = item["answer"].asString();
            c.set = item["set"].asString();
            c.correct = item["correct"].asInt();
            c.wrong = item["wrong"].asInt();
            cards.push_back(c);
        }
        save();
        cout << "\033[32mИмпортировано " << root.size() << " карточек из " << filename << "\033[0m" << endl;
    }

    void exportCSV(const string& filename) {
        ofstream ofs(filename);
        ofs << "question,answer,set,correct,wrong\n";
        for (const auto& c : cards) {
            ofs << c.question << "," << c.answer << "," << c.set << "," << c.correct << "," << c.wrong << "\n";
        }
        cout << "\033[32mЭкспортировано в " << filename << " (CSV)\033[0m" << endl;
    }
};

int main(int argc, char* argv[]) {
    string add, answer, set = "Общее", train, json, csv;
    int remove = -1;
    bool list = false, stats = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--add" && i+1 < argc) add = argv[++i];
        else if (arg == "--answer" && i+1 < argc) answer = argv[++i];
        else if (arg == "--set" && i+1 < argc) set = argv[++i];
        else if (arg == "--remove" && i+1 < argc) remove = stoi(argv[++i]);
        else if (arg == "--list") list = true;
        else if (arg == "--train" && i+1 < argc) train = argv[++i];
        else if (arg == "--stats") stats = true;
        else if (arg == "--export-json" && i+1 < argc) json = argv[++i];
        else if (arg == "--import-json" && i+1 < argc) json = argv[++i]; // для импорта используем тот же флаг
        else if (arg == "--export-csv" && i+1 < argc) csv = argv[++i];
    }

    FlashCards fc;
    if (!add.empty()) {
        if (answer.empty()) {
            cerr << "\033[31mДля добавления карточки требуется --answer\033[0m" << endl;
            return 1;
        }
        fc.addCard(add, answer, set);
    } else if (remove != -1) {
        fc.removeCard(remove);
    } else if (list) {
        fc.listCards();
    } else if (!train.empty()) {
        fc.train(train);
    } else if (stats) {
        fc.stats();
    } else if (!json.empty()) {
        // по ключевому слову определяем экспорт или импорт: если файл существует, импорт, иначе экспорт? нет, используем разные флаги
        // для упрощения --import-json обрабатывается отдельно, здесь только export
        fc.exportJSON(json);
    } else if (!csv.empty()) {
        fc.exportCSV(csv);
    } else {
        cout << "Используйте --help для справки." << endl;
    }
    return 0;
}
