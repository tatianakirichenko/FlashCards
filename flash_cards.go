// flash_cards.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"time"
)

const dataFile = "cards.json"

type Card struct {
	Question string `json:"question"`
	Answer   string `json:"answer"`
	Set      string `json:"set"`
	Correct  int    `json:"correct"`
	Wrong    int    `json:"wrong"`
}

type FlashCards struct {
	Cards []Card `json:"cards"`
}

func (fc *FlashCards) load() {
	data, err := os.ReadFile(dataFile)
	if err != nil {
		fc.Cards = []Card{}
		return
	}
	if err := json.Unmarshal(data, fc); err != nil {
		fc.Cards = []Card{}
	}
}

func (fc *FlashCards) save() {
	data, _ := json.MarshalIndent(fc, "", "  ")
	os.WriteFile(dataFile, data, 0644)
}

func (fc *FlashCards) addCard(question, answer, set string) {
	fc.Cards = append(fc.Cards, Card{Question: question, Answer: answer, Set: set})
	fc.save()
	fmt.Printf("\033[32mКарточка добавлена (ID: %d)\033[0m\n", len(fc.Cards)-1)
}

func (fc *FlashCards) removeCard(idx int) {
	if idx < 0 || idx >= len(fc.Cards) {
		fmt.Println("\033[31mНеверный ID.\033[0m")
		return
	}
	removed := fc.Cards[idx]
	fc.Cards = append(fc.Cards[:idx], fc.Cards[idx+1:]...)
	fc.save()
	fmt.Printf("\033[33mКарточка '%s' удалена.\033[0m\n", removed.Question)
}

func (fc *FlashCards) listCards() {
	if len(fc.Cards) == 0 {
		fmt.Println("\033[33mНет карточек.\033[0m")
		return
	}
	fmt.Println("\033[36m📚 Список карточек:\033[0m")
	for i, c := range fc.Cards {
		fmt.Printf("  %d. %s | %s | набор: %s | правильные: %d, ошибки: %d\n",
			i, c.Question, c.Answer, c.Set, c.Correct, c.Wrong)
	}
}

func (fc *FlashCards) train(set string) {
	cards := fc.Cards
	if set != "" {
		var filtered []Card
		for _, c := range cards {
			if c.Set == set {
				filtered = append(filtered, c)
			}
		}
		if len(filtered) == 0 {
			fmt.Printf("\033[31mНабор '%s' не найден.\033[0m\n", set)
			return
		}
		cards = filtered
	}
	if len(cards) == 0 {
		fmt.Println("\033[33mНет карточек для тренировки.\033[0m")
		return
	}
	rand.Shuffle(len(cards), func(i, j int) { cards[i], cards[j] = cards[j], cards[i] })
	correctTotal := 0
	wrongTotal := 0
	fmt.Printf("\033[32mНачинаем тренировку (набор: %s)\033[0m\n", set)
	fmt.Println("Введите ответ, или 'q' для выхода, или 's' для пропуска.")
	reader := bufio.NewReader(os.Stdin)
	for _, card := range cards {
		fmt.Printf("\033[36mВопрос: %s\033[0m\n", card.Question)
		fmt.Print("\033[33mВаш ответ: \033[0m")
		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)
		if input == "q" {
			fmt.Println("\033[35mТренировка прервана.\033[0m")
			break
		}
		if input == "s" {
			fmt.Printf("\033[34mПравильный ответ: %s\033[0m\n", card.Answer)
			continue
		}
		if strings.EqualFold(input, card.Answer) {
			fmt.Println("\033[32m✅ Правильно!\033[0m")
			card.Correct++
			correctTotal++
		} else {
			fmt.Printf("\033[31m❌ Неправильно. Правильный ответ: %s\033[0m\n", card.Answer)
			card.Wrong++
			wrongTotal++
		}
		fc.save()
	}
	total := correctTotal + wrongTotal
	if total > 0 {
		fmt.Printf("\033[35mТренировка завершена. Правильных: %d, ошибок: %d, точность: %.1f%%\033[0m\n",
			correctTotal, wrongTotal, float64(correctTotal)/float64(total)*100)
	}
}

func (fc *FlashCards) stats() {
	if len(fc.Cards) == 0 {
		fmt.Println("\033[33mНет данных.\033[0m")
		return
	}
	totalCorrect, totalWrong := 0, 0
	for _, c := range fc.Cards {
		totalCorrect += c.Correct
		totalWrong += c.Wrong
	}
	total := totalCorrect + totalWrong
	fmt.Println("\033[36m📊 Статистика:\033[0m")
	fmt.Printf("  Всего карточек: %d\n", len(fc.Cards))
	fmt.Printf("  Правильных ответов: %d\n", totalCorrect)
	fmt.Printf("  Неправильных ответов: %d\n", totalWrong)
	if total > 0 {
		fmt.Printf("  Точность: %.1f%%\n", float64(totalCorrect)/float64(total)*100)
	}
	var weak []Card
	for _, c := range fc.Cards {
		if c.Correct+c.Wrong > 0 && float64(c.Correct)/float64(c.Correct+c.Wrong) < 0.5 {
			weak = append(weak, c)
		}
	}
	if len(weak) > 0 {
		fmt.Println("\033[33mСлабые карточки (точность < 50%):\033[0m")
		for _, c := range weak {
			acc := float64(c.Correct) / float64(c.Correct+c.Wrong) * 100
			fmt.Printf("  %s | точность: %.1f%%\n", c.Question, acc)
		}
	}
}

func (fc *FlashCards) exportJSON(filename string) {
	data, _ := json.MarshalIndent(fc.Cards, "", "  ")
	os.WriteFile(filename, data, 0644)
	fmt.Printf("\033[32mЭкспортировано в %s (JSON)\033[0m\n", filename)
}

func (fc *FlashCards) importJSON(filename string) {
	data, err := os.ReadFile(filename)
	if err != nil {
		fmt.Printf("\033[31mОшибка импорта: %v\033[0m\n", err)
		return
	}
	var imported []Card
	if err := json.Unmarshal(data, &imported); err != nil {
		fmt.Printf("\033[31mОшибка импорта: %v\033[0m\n", err)
		return
	}
	fc.Cards = append(fc.Cards, imported...)
	fc.save()
	fmt.Printf("\033[32mИмпортировано %d карточек из %s\033[0m\n", len(imported), filename)
}

func (fc *FlashCards) exportCSV(filename string) {
	f, _ := os.Create(filename)
	defer f.Close()
	w := csv.NewWriter(f)
	defer w.Flush()
	w.Write([]string{"question", "answer", "set", "correct", "wrong"})
	for _, c := range fc.Cards {
		w.Write([]string{c.Question, c.Answer, c.Set, strconv.Itoa(c.Correct), strconv.Itoa(c.Wrong)})
	}
	fmt.Printf("\033[32mЭкспортировано в %s (CSV)\033[0m\n", filename)
}

func main() {
	var (
		add      string
		answer   string
		set      string
		remove   int
		list     bool
		train    string
		stats    bool
		expJson  string
		impJson  string
		expCsv   string
	)
	flag.StringVar(&add, "add", "", "Добавить карточку (вопрос)")
	flag.StringVar(&answer, "answer", "", "Ответ на карточку")
	flag.StringVar(&set, "set", "Общее", "Набор (категория)")
	flag.IntVar(&remove, "remove", -1, "Удалить карточку по ID")
	flag.BoolVar(&list, "list", false, "Показать все карточки")
	flag.StringVar(&train, "train", "", "Начать тренировку (указать набор, или пустую строку для всех)")
	flag.BoolVar(&stats, "stats", false, "Показать статистику")
	flag.StringVar(&expJson, "export-json", "", "Экспорт в JSON")
	flag.StringVar(&impJson, "import-json", "", "Импорт из JSON")
	flag.StringVar(&expCsv, "export-csv", "", "Экспорт в CSV")
	flag.Parse()

	fc := &FlashCards{}
	fc.load()

	if add != "" {
		if answer == "" {
			fmt.Println("\033[31mДля добавления карточки требуется --answer\033[0m")
			os.Exit(1)
		}
		fc.addCard(add, answer, set)
	} else if remove != -1 {
		fc.removeCard(remove)
	} else if list {
		fc.listCards()
	} else if train != "" || flag.Arg(0) == "train" {
		fc.train(train)
	} else if stats {
		fc.stats()
	} else if expJson != "" {
		fc.exportJSON(expJson)
	} else if impJson != "" {
		fc.importJSON(impJson)
	} else if expCsv != "" {
		fc.exportCSV(expCsv)
	} else {
		fmt.Println("Используйте --help для справки.")
	}
}
