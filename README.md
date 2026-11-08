<div align="center">

<img src="src/main/resources/assets/sandevistan/icon_sandevistan.png" width="140" alt="Sandevistan">

# Sandevistan

**Киберустановка Militech «Апогей» из Cyberpunk: Edgerunners для Minecraft 1.20.1 (Fabric)**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.15.11+-DBB69B?style=for-the-badge&logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-17-E76F00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Discord](https://img.shields.io/badge/Discord-Сообщество-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/ghxC6H2htE)

[**🇷🇺 Русский**](README.md) · [🇬🇧 English](README.en.md) · [🇯🇵 日本語](README.ja.md)

</div>

---

> **«Время — это роскошь. Сандевистан делает её доступной.»**
>
> Установите киберустановку в позвоночник, нажмите **V** — и мир вокруг замедлится,
> оставив за вами шлейф послеобразов. Но помните: частое использование ведёт к
> **киберпсихозу**.

---

## ✨ Возможности

| | |
|---|---|
| ⏳ **Замедление времени** | Мир вокруг игрока замедляется до **15%** скорости в радиусе **64 блоков**. Активация длится до **9 секунд**, перезарядка — до **15 секунд** и зависит от потраченного заряда. |
| 👤 **Только окружение** | Владелец установки продолжает двигаться с обычной скоростью. Других игроков можно замедлять (геймрул). |
| 🌀 **Визуальные эффекты** | Ghost trail, шоковые кольца, частицы, постобработка шейдерами `sandy` и `psycho`, лёгкое дрожание камеры. |
| 🧠 **Киберпсихоз** | Перегрузка вызывает искажения экрана, шум, звон в ушах, дебаффы и урон. Доведите шкалу до предела — и всё закончится плохо. |
| 🖥️ **HUD** | Полоса заряда, индикатор перезарядки и состояние установки. |
| 🔊 **Звук** | Собственные звуки активации, работы, предупреждения и «плоской линии». |
| 🌐 **Локализация** | Русский, английский и японский языки. |
| 🔌 **Совместимость** | Мягкая интеграция с модом **Capri** — при его отсутствии ничего не ломается. |

## 📦 Установка

### Требования

| Компонент | Версия |
|---|---|
| Minecraft | **1.20.1** |
| Fabric Loader | **0.15.11+** |
| Fabric API | **0.92.2+1.20.1** |
| Java | **17+** |

### Способ 1 — готовый JAR

1. Скачайте последний `sandevistan-*.jar` со страницы [Releases](https://github.com/watsbunny/sandevistan/releases).
2. Поместите его в папку `mods` вашего профиля Minecraft.
3. Убедитесь, что установлены **Fabric Loader** и **Fabric API**.
4. Запустите игру.

### Способ 2 — сборка из исходников

```bash
git clone https://github.com/watsbunny/sandevistan.git
cd sandevistan
./gradlew build
```

Готовый файл появится в `build/libs/sandevistan-1.0.0+1.20.1.jar`.

## 🎮 Управление

| Клавиша | Действие |
|---|---|
| **ПКМ** по предмету | Установить Сандевистан в позвоночник |
| **V** | Активировать / деактивировать замедление времени |

Клавишу можно переназначить в **Настройки → Управление → Сандевистан**.

## 💬 Команды

Для команд нужен уровень прав оператора (**2**).

| Команда | Описание |
|---|---|
| `/sandevistan install [игроки]` | Выдать и установить Сандевистан |
| `/sandevistan remove [игроки]` | Удалить Сандевистан |
| `/sandevistan toggle` | Переключить замедление времени |
| `/sandevistan psycho <игроки> <0–1000>` | Задать уровень киберпсихоза (для тестов) |

## ⚙️ Геймрулы

| Геймрул | По умолчанию | Описание |
|---|---|---|
| `sandevistanSlowsPlayers` | `true` | Замедляет других игроков рядом с активной установкой |
| `sandevistanCyberpsychosis` | `true` | Включает накопление киберпсихоза от перегрузки |

Изменить можно командой:

```mcfunction
/gamerule sandevistanCyberpsychosis false
```

## 🧩 Совместимость

- **Fabric API** — обязателен.
- **Capri** — опционально. Мод определяет его наличие через `Class.forName`; если Capri не установлен, интеграция просто отключается.
- **Satin** — библиотека шейдеров, поставляется внутри JAR (JiJ), отдельно устанавливать не нужно.

## 🗂️ Структура проекта

```
sandevistan/
├─ src/main/java/com/watsbunny/sandy/
│  ├─ Sandevistan.java          точка входа, предмет, геймрулы
│  ├─ SandyServer.java          серверная логика замедления
│  ├─ SlowClock.java            расчёт скорости времени
│  ├─ SandyCommand.java         команды /sandevistan
│  ├─ cyber/                    предмет, позвоночник, киберпсихоз
│  └─ client/                   HUD, шейдеры, следы, эффекты, миксины
├─ src/main/resources/
│  ├─ assets/sandevistan/       текстуры, звуки, шейдеры, языки
│  ├─ data/sandevistan/         рецепты, теги, типы урона
│  └─ *.mixins.json             конфигурации миксинов
├─ build.gradle
└─ gradle.properties
```

## 🛠️ Сборка

Проект собран на **JDK 21** с таргетом **Java 17** и **Gradle 8.9**.

```bash
./gradlew build       # собрать мод
./gradlew runClient   # запустить клиент для отладки
```

## 💬 Сообщество

Официальный Discord разработчика — там можно задать вопрос, сообщить об ошибке
и первым узнать о новых версиях:

<div align="center">

[![Присоединиться к Discord](https://img.shields.io/badge/Присоединиться_к_Discord-официальный_сервер-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/ghxC6H2htE)

**https://discord.gg/ghxC6H2htE**

</div>

## 📜 Лицензия

**All Rights Reserved.** Исходный код предоставлен для личного изучения.
Публикация, перепубликация и распространение изменённых сборок без разрешения
автора запрещены.

---

<div align="center">

Сделано с ❤️ **WatsBunny**

[Discord](https://discord.gg/ghxC6H2htE) · [GitHub](https://github.com/watsbunny)

<sub>Неофициальный фанатский проект по вселенной Cyberpunk: Edgerunners. Minecraft — товарный знак Mojang Studios.</sub>

</div>
