# Cash POS System

**Cash POS** is a cross-platform Point of Sale system built with **JavaFX + Spring Boot + PostgreSQL**.  
It supports cash register hardware, multiple payment types (cash, card, mixed), returns, sales history, and integrations with external systems.

---

## 📜 License

This project is offered under a **dual licensing model**:

### 1. Open Source License (Community Edition)
- Licensed under the **GNU AGPLv3**.  
- You are free to use, modify, and distribute the code, provided you comply with the AGPLv3 requirements:
  - Retain copyright and license notices;
  - Disclose source code of derivative works;
  - Disclose source code when used as SaaS (web-based services).

### 2. Commercial License (Enterprise Edition)
- Available under a separate commercial agreement.  
- Intended for organizations that:
  - Do not wish to disclose their modifications or derivative works;
  - Require enterprise integrations (ERP, 1C, banking APIs, etc.);
  - Need professional support, updates, and SLA guarantees.

📧 To obtain a **commercial license**, contact us at:  
**estai86@gmail.com**

---

## 🚀 Features
- Java 22, JavaFx 24 and Spring Boot 3.x support  
- PostgreSQL as the main database (Oracle optional)  
- POS hardware integration  
- Sales, returns, shift management, sales history  
- Asynchronous product search (barcode & name)  
- Multiple payment types: Cash, Card, Mixed  

## 🧰 Hardware & OS
- **Barcode scanners:** any USB/HID device configured to send **Enter (CR/LF)** after the code. Tab is optionally supported.
- **Receipt printer:** 80 mm, **ESC/POS** compatible. Prints to the **system default printer**.
- **OS:** Windows 10/11 (x64). Linux/macOS — experimental.

## ⚙️ Requirements
- PostgreSQL 15+ (recommended: 16/17), pre-installed.
- JavaFX 24 if building from source.
- Java 21/22 (JDK) if building from source.

## 🚦 Quick Start (Windows)
1) Install PostgreSQL and create DB/user (see below).
2) Install the receipt printer and set it as **Default** in Windows.
3) Connect a barcode scanner in **USB HID Keyboard** mode with **Enter** suffix.
4) Configure `application.yml` (Database section).
5) Run the app (bundled `.exe` or `java -jar`). Compile exe executing command in project '.\mvnw -DskipTests clean package' and '.\mvnw -DskipTests jpackage:jpackage -X'
6) Install Cash-Store.exe in any cash computer


📚 Подробнее: [docs/INSTALL_WINDOWS.md](docs/INSTALL_WINDOWS.md), [docs/PRINTING.md](docs/PRINTING.md), [docs/BARCODE_SCANNER.md](docs/BARCODE_SCANNER.md), [docs/DB_SETUP.md](docs/DB_SETUP.md), [docs/PACKAGING_WINDOWS.md](docs/PACKAGING_WINDOWS.md)
---

© 2025 Yestay Yeleup. All rights reserved.
