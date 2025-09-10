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

See the full text in [LICENSE](LICENSE).

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
- **Сканеры штрих-кода:** любой USB/HID, настроенный на **суффикс Enter (CR/LF)**. Tab поддерживается опционально.
- **Чековый принтер:** 80 мм, **ESC/POS**-совместимый. Печать идёт на **системный принтер по умолчанию**.
- **OS:** Windows 10/11 (x64). Linux/macOS — экспериментально.

## ⚙️ Requirements
- PostgreSQL 15+ (рекомендовано 16/17), заранее установленный.
- Java 21/22 (JDK) при сборке из исходников.

## 🚦 Quick Start (Windows)
1) Установите PostgreSQL и создайте БД/пользователя (см. ниже).
2) Установите чековый принтер и сделайте его **принтером по умолчанию** в Windows.
3) Подключите сканер штрих-кода в режиме **HID Keyboard** с суффиксом **Enter**.
4) Отредактируйте `application.yml` (раздел *Database*).
5) Запустите приложение (готовый `.exe` или `java -jar`).

📚 Подробнее: [docs/INSTALL_WINDOWS.md](docs/INSTALL_WINDOWS.md), [docs/PRINTING.md](docs/PRINTING.md), [docs/BARCODE_SCANNER.md](docs/BARCODE_SCANNER.md), [docs/DB_SETUP.md](docs/DB_SETUP.md), [docs/PACKAGING_WINDOWS.md](docs/PACKAGING_WINDOWS.md)
---

© 2025 Yestay Yeleup. All rights reserved.
