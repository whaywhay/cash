package kz.store.cash.fx.component;

import java.util.ArrayList;
import java.util.List;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import kz.store.cash.config.ProductProperties;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.repository.SalesRepository;
import kz.store.cash.util.UtilAlert;
import kz.store.cash.util.UtilNumbers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptPrintService {

  private final SalesRepository salesRepository;
  private final ProductProperties productProperties;
  private final UtilAlert utilAlert;
  // Константы ширины строки (для 80мм ленты ≈ 42 символа) - Но я добавил два символа,
  // вроде печатается без проблем
  private static final int LINE_WIDTH = 44;
  private final UtilNumbers utilNumbers;

  /**
   * Основной метод печати чека в ESC/POS
   */
  public void printReceiptRawWithLine(PaymentReceipt currentReceipt) {
    if (currentReceipt == null) {
      return;
    }
    try {
      StringBuilder sb = new StringBuilder();

      // --- Шапка чека ---
      sb.append(centerText("КАССИР: " + productProperties.organizationName()))
          .append("\n");
      sb.append(centerText(productProperties.cashierName())).append("\n");
      sb.append(centerText("ЧЕК #" + currentReceipt.getId())).append("\n");
      sb.append(centerText(currentReceipt.getCreated().toString())).append("\n");
      sb.append(centerText(currentReceipt.getPaymentType().getDisplayName())).append("\n");
      sb.append(repeatLineChar()).append("\n");

      // --- Заголовки таблицы ---
      sb.append(formatRow("Товар", "Кол-во", "Цена", "Сумма")).append("\n");
      sb.append(repeatLineChar()).append("\n");

      // --- Список товаров ---
      List<SalesWithProductName> sales = salesRepository.findSalesWithProductNames(
          currentReceipt.getId());
      for (SalesWithProductName sale : sales) {
        String productName = sale.productName();
        // Разбиваем название на части по ширине колонки (21 символов)
        List<String> nameParts = splitByLength(productName);
        // Первая строка: название + колонки
        sb.append(formatRow(
            nameParts.getFirst(),
            sale.quantity() + "шт",
            utilNumbers.formatPrice(sale.soldPrice()),
            utilNumbers.formatPrice(sale.total())
        )).append("\n");
        // Остальные строки: только название, остальные колонки пустые
        for (int i = 1; i < nameParts.size(); i++) {
          sb.append(padRight(nameParts.get(i), 21))   // Товар
              .append(padLeft("", 7))                   // Кол-во
              .append(padLeft("", 7))                   // Цена
              .append(padLeft("", 9))                   // Сумма
              .append("\n");
        }
      }
      sb.append(repeatLineChar()).append("\n");
      // --- Итого ---
      sb.append("ИТОГО: ").append(String.format("%.2f тг", currentReceipt.getTotal()))
          .append("\n");
      sb.append(currentReceipt.getPaymentType().getDisplayName()).append(": ")
          .append(String.format("%.2f тг", currentReceipt.getReceivedPayment())).append("\n");

      if (currentReceipt.getPaymentType() == PaymentType.MIXED) {
        sb.append("Наличные: ").append(String.format("%.2f тг", currentReceipt.getCashPayment()))
            .append("\n");
        sb.append("Карта: ").append(String.format("%.2f тг", currentReceipt.getCardPayment()))
            .append("\n");
      }

      sb.append("СДАЧА: ").append(currentReceipt.getChangeMoney()).append("\n");
      // --- Протяжка бумаги и обрезка ---
      feed(sb);
      cutPaper(sb, false); // true = полная, false = частичная

      // --- Отправляем в принтер ---
      sendToPrinter(sb.toString().getBytes("CP1251"));

    } catch (Exception e) {
      log.error("Ошибка печати чека", e);
      utilAlert.showError("Ошибка печати",
          "Не удалось отправить чек в принтер.\n" + e.getMessage());
    }
  }

  /**
   * Отправка данных в принтер по умолчанию
   */
  private void sendToPrinter(byte[] data) throws PrintException {
    DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
    PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
    if (printService == null) {
      utilAlert.showError("Ошибка печати", "Не найден принтер по умолчанию!");
      return;
    }
    DocPrintJob job = printService.createPrintJob();
    Doc doc = new SimpleDoc(data, flavor, null);
    PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
    job.print(doc, attr);
  }
  // ===================== ESC/POS команды =====================

  /**
   * Протяжка бумаги (n строк)
   */
  private void feed(StringBuilder sb) {
    sb.append("\n".repeat(4));
  }

  /**
   * Обрезка бумаги: fullCut = true → полная, false → частичная
   */
  private void cutPaper(StringBuilder sb, boolean fullCut) {
    sb.append((char) 0x1D)          // GS
        .append('V')                // команда обрезки бумаги
        .append((char) (fullCut ? 65 : 66)) // A=полная, B=частичная
        .append((char) 0);          // обрезка сразу
  }

  // ===================== Форматирование текста =====================
  private String repeatLineChar() {
    return ("-").repeat(LINE_WIDTH);
  }

  private String centerText(String text) {
    if (text.length() >= LINE_WIDTH) {
      return text;
    }
    int left = (LINE_WIDTH - text.length()) / 2;
    return " ".repeat(left) + text;
  }

  /**
   * Форматируем строку в таблицу: Товар - Кол-во - Цена - Сумма
   */
  private String formatRow(String col1, String col2, String col3, String col4) {
    // ширины колонок: 20 + 6 + 7 + 9 = 42
    int w1 = 21, w2 = 5, w3 = 7, w4 = 9;

    return padRight(col1, w1)
        + padLeft(col2, w2)
        + padLeft(col3, w3)
        + padLeft(col4, w4);
  }

  private String padRight(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    return text + " ".repeat(width - text.length());
  }

  private String padLeft(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    return " ".repeat(width - text.length()) + text;
  }

  /**
   * Разбиваем длинные строки на куски по length символов = 21 символов название продукта, если
   * больше переносим на следующию строку
   */
  private List<String> splitByLength(String text) {
    int splitLength = 21;
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < text.length(); i += splitLength) {
      parts.add(text.substring(i, Math.min(i + splitLength, text.length())));
    }
    return parts;
  }
}
