# Reisebroker – Verteilte Systeme Projekt

Hier findet ihr die Lösung von Verteilte Systeme.

## Inhalte
- ZeroMQ Microservices
- Buchungsanfragen-Simulation
- SAGA Pattern Implementierung
- Logging mit Logback
- Konfigurierbare Properties

## Starten
```bash
mvn clean package
java -jar target/reisebroker-1.0-SNAPSHOT-jar-with-dependencies.jar
```

🔗 Projekt klonen & einrichten
Folge diesen Schritten, um das Projekt lokal bei dir auszuführen:

## 1. Git installieren
Falls du Git noch nicht installiert hast, lade es herunter und installiere es

## 2. Repository klonen
Öffne dein Terminal und führe folgenden Befehl aus:

```bash
git clone https://github.com/nadl172/reisebroker.git
```

## 3. Wechsle ins Projektverzeichnis

```bash
cd reisebroker
```

## 4. Maven installieren (falls noch nicht installiert)
Falls du Maven noch nicht installiert hast, kannst du es mit Homebrew installieren:

```bash

/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
````

Dann installiere Maven mit:

```bash
brew install maven
```

## 5. Abhängigkeiten mit Maven installieren
Nachdem Maven installiert ist, führe folgenden Befehl aus, um die Projektabhängigkeiten zu installieren:

```bash
mvn clean install
```

## 7. (Optional) Änderungen vom Remote holen
Wenn du sicherstellen willst, dass du die neuesten Änderungen hast:

```bash
git pull origin main
```

✅ Voraussetzungen
- Git installiert
- Visual Studio Code installiert 
- Maven installiert
- Homebrew installiert (nur für macOS, Download hier)
- Zugang zum privaten Repository (du musst als Mitarbeiter:in eingeladen sein)
