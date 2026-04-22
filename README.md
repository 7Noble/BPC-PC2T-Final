# Databázový systém zaměstnanců

Semestrální projekt z předmětu **BPC-PC2T** – konzolová Java aplikace pro správu databáze zaměstnanců technologické firmy s podporou skupinových dovedností, souborové serializace a SQLite zálohy.

---

## Obsah

1. [Požadavky](#požadavky)
2. [Sestavení a spuštění](#sestavení-a-spuštění)
3. [Architektura projektu](#architektura-projektu)
4. [Datový model](#datový-model)
5. [Skupiny zaměstnanců a jejich dovednosti](#skupiny-zaměstnanců-a-jejich-dovednosti)
6. [Popis funkcionalit menu](#popis-funkcionalit-menu)
7. [Souborová serializace](#souborová-serializace)
8. [SQLite databáze](#sqlite-databáze)
9. [OOP principy](#oop-principy)
10. [Závislosti](#závislosti)

---

## Požadavky

| Nástroj | Minimální verze |
|---------|----------------|
| JDK     | 17             |
| Maven   | 3.8+           |

> Projekt byl testován s **OpenJDK 25** a **Maven 3.9** dodaným s IntelliJ IDEA.

---

## Sestavení a spuštění

### Sestavení (JAR s závislostmi)

```bash
mvn package
```

Výstupní JAR se vytvoří jako `target/employee-database-1.0-SNAPSHOT.jar`.

> Standardní JAR neobsahuje SQLite JDBC knihovnu. Pro spouštění mimo IDE přidejte JAR na classpath nebo použijte `maven-shade-plugin` / `maven-assembly-plugin` pro vytvoření fat-JAR.

### Spuštění v IntelliJ IDEA

Otevřete projekt, nastavte JDK 17+ a spusťte třídu `cz.vutbr.bpcpc2t.Main`.

### Spuštění z příkazové řádky

```bash
# Předpoklad: sqlite-jdbc JAR je na classpath
java -cp "target/employee-database-1.0-SNAPSHOT.jar;path/to/sqlite-jdbc.jar" cz.vutbr.bpcpc2t.Main
```

---

## Architektura projektu

```
src/main/java/cz/vutbr/bpcpc2t/
│
├── Main.java                          # Vstupní bod, konzolové menu
│
├── model/
│   ├── CooperationLevel.java          # Enum: BAD | AVERAGE | GOOD
│   ├── Cooperation.java               # Vazba zaměstnanec–kolega s úrovní
│   ├── SkillExecutor.java             # Interface: executeSkill(), getSkillDescription()
│   ├── EmployeeRepository.java        # Interface: findById(), getAllEmployees()
│   ├── Employee.java                  # Abstraktní třída (implements SkillExecutor)
│   ├── DataAnalyst.java               # Konkrétní skupina – Datový analytik
│   └── SecuritySpecialist.java        # Konkrétní skupina – Bezpečnostní specialista
│
├── service/
│   └── EmployeeDatabase.java          # Správa databáze (implements EmployeeRepository)
│
└── storage/
    ├── FileManager.java               # Uložení/načtení zaměstnance do/ze souboru
    └── SqliteManager.java             # Záloha a obnova dat přes JDBC/SQLite
```

### Diagram závislostí

```
SkillExecutor  EmployeeRepository
     ▲                ▲
     │                │
  Employee      EmployeeDatabase
     ▲
     ├── DataAnalyst
     └── SecuritySpecialist

Employee  ←uses─  EmployeeRepository   (při executeSkill)
EmployeeDatabase  ─implements─  EmployeeRepository
```

Rozhraní `EmployeeRepository` záměrně zamezuje cyklické závislosti mezi vrstvou `model` a `service` – metoda `executeSkill` přijímá abstrakci, nikoli konkrétní `EmployeeDatabase`.

---

## Datový model

### Zaměstnanec (`Employee`)

| Atribut        | Typ      | Popis                              |
|----------------|----------|------------------------------------|
| `id`           | `int`    | Unikátní ID, přidělováno automaticky |
| `name`         | `String` | Jméno                              |
| `surname`      | `String` | Příjmení                           |
| `birthYear`    | `int`    | Rok narození                       |
| `cooperations` | `List<Cooperation>` | Dynamický seznam vazeb  |

### Spolupráce (`Cooperation`)

| Atribut       | Typ                | Popis                    |
|---------------|--------------------|--------------------------|
| `colleagueId` | `int`              | ID kolegy                |
| `level`       | `CooperationLevel` | Úroveň kvality spolupráce |

### Úroveň spolupráce (`CooperationLevel`)

| Hodnota   | Zobrazení   | Skóre |
|-----------|-------------|-------|
| `BAD`     | špatná      | 1     |
| `AVERAGE` | průměrná    | 2     |
| `GOOD`    | dobrá       | 3     |

---

## Skupiny zaměstnanců a jejich dovednosti

### Datový analytik (`DataAnalyst`)

**Dovednost:** Nalezení spolupracovníka s nejvíce společnými kolegy.

**Algoritmus:**

1. Sestaví množinu vlastních spolupracovníků `M`.
2. Pro každého spolupracovníka `K` ze seznamu:
   - Sestaví množinu kolegů `K` (s vyloučením sebe samého).
   - Spočítá průnik `|kolegové(K) ∩ M|`.
3. Vypíše spolupracovníka `K` s nejvyšším průnikem.

**Příklad:**

```
Analytik A má kolegy: B, C, D
  B má kolegy: A, C, E  →  průnik s M = {C}       = 1
  C má kolegy: A, B, D  →  průnik s M = {B, D}    = 2  ← vítěz
  D má kolegy: A, B     →  průnik s M = {B}        = 1

Výsledek: C – 2 společní spolupracovníci
```

---

### Bezpečnostní specialista (`SecuritySpecialist`)

**Dovednost:** Výpočet rizikového skóre spolupráce.

**Algoritmus:**

Každá spolupráce je ohodnocena rizikovou váhou:

| Úroveň  | Riziková váha |
|---------|---------------|
| Dobrá   | 1             |
| Průměrná| 2             |
| Špatná  | 3             |

Vzorec:

```
průměrná váha  = (Σ váha_i) / počet
faktor expozice = 1 + ln(počet + 1)
rizikové skóre  = průměrná váha × faktor expozice × (10 / 3)
```

Výsledné skóre je na škále **0 – ~100**. Faktor expozice penalizuje zaměstnance s velkým počtem kontaktů, i kdyby byly všechny průměrné.

**Hodnocení skóre:**

| Skóre       | Hodnocení                                   |
|-------------|---------------------------------------------|
| < 15        | NÍZKÉ – minimální bezpečnostní hrozba       |
| 15 – 29     | STŘEDNÍ – sledovat vývoj spolupráce         |
| 30 – 49     | ZVÝŠENÉ – doporučena revize kontaktů        |
| ≥ 50        | VYSOKÉ – okamžitá bezpečnostní kontrola     |

---

## Popis funkcionalit menu

Po spuštění se zobrazí konzolové menu s volbami `0`–`10`:

```
 1. Přidání zaměstnance
 2. Přidání spolupráce
 3. Odebrání zaměstnance
 4. Vyhledání zaměstnance dle ID
 5. Spuštění dovednosti zaměstnance
 6. Abecední výpis zaměstnanců dle skupin
 7. Statistiky
 8. Počet zaměstnanců ve skupinách
 9. Uložení zaměstnance do souboru
10. Načtení zaměstnance ze souboru
 0. Uložit a ukončit
```

### 1 – Přidání zaměstnance

Uživatel vybere skupinu (1 = Datový analytik, 2 = Bezpečnostní specialista), zadá jméno, příjmení a rok narození. ID je přiděleno automaticky jako inkrementální číslo.

### 2 – Přidání spolupráce

Uživatel zadá ID zaměstnance, ID kolegy a úroveň spolupráce (1=špatná, 2=průměrná, 3=dobrá). Pokud spolupráce se stejným kolegou již existuje, je přepsána. Nelze přidat spolupráci zaměstnance se sebou samým.

### 3 – Odebrání zaměstnance

Zaměstnanec je odstraněn z databáze **včetně všech vazeb**, které na něj odkazují od ostatních zaměstnanců.

### 4 – Vyhledání zaměstnance dle ID

Vypíše detailní informace: jméno, rok narození, skupina, dovednost, počet spolupracovníků, převažující kvalita spolupráce, průměrné skóre a seznam všech vazeb.

### 5 – Spuštění dovednosti

Dle skupiny zaměstnance spustí příslušný algoritmus (viz sekce [Skupiny](#skupiny-zaměstnanců-a-jejich-dovednosti)).

### 6 – Abecední výpis dle skupin

Vypíše všechny zaměstnance seřazené podle příjmení (sekundárně podle jména), seskupené podle skupin.

### 7 – Statistiky

- Převažující úroveň spolupráce napříč celou databází.
- Zaměstnanec s nejvíce vazbami.
- Celkový počet zaměstnanců a vazeb.

### 8 – Počet zaměstnanců ve skupinách

Přehledná tabulka počtu zaměstnanců v každé skupině + celkový součet.

### 9 – Uložení zaměstnance do souboru

Uloží jednoho zaměstnance do textového `.emp` souboru (viz [Souborová serializace](#souborová-serializace)). Výchozí název souboru je `employee_<ID>.emp`.

### 10 – Načtení zaměstnance ze souboru

Načte zaměstnance z `.emp` souboru. Pokud zaměstnanec se stejným ID již v databázi existuje, je přepsán.

### 0 – Uložit a ukončit

Uloží kompletní databázi do SQLite zálohy (`employees.db`) a ukončí program.

---

## Souborová serializace

Každý zaměstnanec se ukládá jako prostý textový soubor v kódování **UTF-8** s příponou `.emp`.

### Formát souboru

```
TYPE:DataAnalyst
ID:1
NAME:Jan
SURNAME:Novák
BIRTHYEAR:1990
COOPERATION:2:GOOD
COOPERATION:5:BAD
COOPERATION:7:AVERAGE
```

| Klíč          | Hodnota                                  |
|---------------|------------------------------------------|
| `TYPE`        | `DataAnalyst` nebo `SecuritySpecialist`  |
| `ID`          | Číslo (kladné celé číslo)                |
| `NAME`        | Jméno (libovolný řetězec)                |
| `SURNAME`     | Příjmení (libovolný řetězec)             |
| `BIRTHYEAR`   | Rok narození (číslo)                     |
| `COOPERATION` | `<ID_kolegy>:<BAD\|AVERAGE\|GOOD>`       |

Řádky `COOPERATION` se opakují pro každou vazbu. Pořadí ostatních řádků není závazné – parser rozlišuje klíče přes `switch`.

---

## SQLite databáze

Program automaticky pracuje se souborem **`employees.db`** v pracovním adresáři:

- **Při spuštění** – tabulky jsou vytvořeny (pokud neexistují) a všechna data jsou načtena.
- **Při ukončení** (volba `0`) – celá databáze je přepsána aktuálním stavem paměti.

Program funguje plně i bez SQLite – při nedostupnosti ovladače zobrazí varování a pokračuje bez zálohy.

### Schéma tabulek

```sql
CREATE TABLE IF NOT EXISTS employees (
    id         INTEGER PRIMARY KEY,
    name       TEXT    NOT NULL,
    surname    TEXT    NOT NULL,
    birth_year INTEGER NOT NULL,
    type       TEXT    NOT NULL   -- 'DataAnalyst' | 'SecuritySpecialist'
);

CREATE TABLE IF NOT EXISTS cooperations (
    employee_id  INTEGER NOT NULL,
    colleague_id INTEGER NOT NULL,
    level        TEXT    NOT NULL,  -- 'BAD' | 'AVERAGE' | 'GOOD'
    PRIMARY KEY (employee_id, colleague_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);
```

Ukládání probíhá v jedné **JDBC transakci** (DELETE + batch INSERT) – při chybě se neuloží nekonzistentní data.

---

## OOP principy

### Abstraktní třída

**`Employee`** (`model/Employee.java`) je abstraktní třída, která:
- definuje společné atributy všech zaměstnanců (ID, jméno, příjmení, rok narození, seznam spolupráce),
- poskytuje hotovou implementaci statistických metod (`getDominantCooperationLevel`, `getAverageCooperationScore`),
- deklaruje abstraktní metody `getGroupName()`, `getTypeName()` a metody z rozhraní `SkillExecutor`, které musí každá podtřída implementovat.

### Rozhraní

| Rozhraní              | Implementující třída     | Účel                                                               |
|-----------------------|--------------------------|--------------------------------------------------------------------|
| `SkillExecutor`       | `Employee` (→ podtřídy)  | Kontrakt pro skupinovou dovednost a její popis                     |
| `EmployeeRepository`  | `EmployeeDatabase`       | Abstrakce přístupu k datům; zamezuje cyklické závislosti model↔service |

### Dědičnost a polymorfismus

```
Employee  (abstraktní)
├── DataAnalyst          – executeSkill() = průnik spolupracovníků
└── SecuritySpecialist   – executeSkill() = výpočet rizikového skóre
```

Volání `employee.executeSkill(db)` v `Main.java` využívá polymorfismus – konkrétní algoritmus se vybere za běhu podle skutečného typu objektu.

### Dynamická datová struktura

`EmployeeDatabase` používá `LinkedList<Employee>` jako interní úložiště. `LinkedList` je vhodná pro časté vkládání a odebírání prvků (O(1) při odebírání s iterátorem), což odpovídá hlavním operacím databáze.

---

## Závislosti

| Artefakt                     | Verze    | Účel                       |
|------------------------------|----------|----------------------------|
| `org.xerial:sqlite-jdbc`     | 3.45.1.0 | JDBC ovladač pro SQLite     |
| `org.apache.maven.plugins:maven-jar-plugin` | 3.3.0 | Sestavení JAR s hlavní třídou |

Java standardní knihovna (JDK 17+) – žádné další závislosti.
