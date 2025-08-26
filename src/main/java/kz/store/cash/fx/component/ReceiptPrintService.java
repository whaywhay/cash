package kz.store.cash.fx.component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.service.SalesService;
import kz.store.cash.util.UtilNumbers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptPrintService {

  private final SalesService salesService;
  private final AppSettingService appSettingService;
  private final UiNotificationService uiNotificationService;
  private final UtilNumbers utilNumbers;

  /**
   * Для 80мм ленты ≈ 42 символа. Добавлен небольшой запас для CP1251.
   */
  private static final int LINE_WIDTH = 44;
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private String organizationName = "";
  private String cashierName = "";
  private String bin = "";

  // -------------------- Публичные методы печати --------------------

  /**
   * Печать сменного чека (закрытие смены). Заголовки по центру, данные — «лейбл слева / значение
   * справа».
   */
  public void printCashShift(CashShift cashShift,
      BigDecimal depositedFunds,
      BigDecimal withdrawalFunds) {
    if (cashShift == null) {
      uiNotificationService.showError("Сменный чек для распечатки пустой");
      return;
    }

    refreshAppSettings();

    try {
      // ---- Предварительные расчёты
      BigDecimal cashSum = zeroIfNull(cashShift.getSumCash());
      BigDecimal cardSum = zeroIfNull(cashShift.getSumCard());
      // Возвраты в исходном коде показывались через negate()
      BigDecimal returnCashSum = zeroIfNull(cashShift.getSumReturnCash()).negate();
      BigDecimal returnCardSum = zeroIfNull(cashShift.getSumReturnCard()).negate();
      BigDecimal leftSum = zeroIfNull(cashShift.getLeftInDrawer());
      BigDecimal openingSum = zeroIfNull(cashShift.getCashDuringOpening());

      BigDecimal all = cashSum.add(cardSum); // Выручка
      BigDecimal allSale = cashSum.add(cardSum).add(returnCashSum).add(returnCardSum);
      BigDecimal realCashSale = cashSum.add(returnCashSum);
      BigDecimal realCardSale = cardSum.add(returnCardSum);
      BigDecimal realCashFromStore =
          cashSum.add(openingSum).subtract(leftSum).subtract(returnCashSum);

      StringBuilder sb = new StringBuilder();

      alignCenter(sb);
      line(sb, "ЗАКРЫТИЕ СМЕНЫ");
      feed(sb, 1);

      // ===== Реквизиты и даты =====
      alignLeft(sb);
      line(sb, formatFullLine("ИИН", bin));
      line(sb, formatFullLine("КАССИР", organizationName));
      if (!isBlank(cashierName)) {
        line(sb, formatFullLine("Место продажи", cashierName));
      }
      line(sb, formatFullLine("Открытие", dtFormat(cashShift.getShiftOpenedDate())));
      line(sb, formatFullLine("Закрытие", dtFormat(cashShift.getShiftClosedDate())));
      sep(sb);

      // ===== Итоги по продажам =====
      line(sb, formatFullLine("Выручка", money(all)));
      line(sb, formatFullLine("  Продажа", money(allSale)));
      line(sb, formatFullLine("  Наличными", money(realCashSale)));
      line(sb, formatFullLine("  Безналичными", money(realCardSale)));
      line(sb, formatFullLine("  Наличные с кассы", money(realCashFromStore)));
      line(sb, formatFullLine("  Возврат (наличные)", money(returnCashSum)));
      line(sb, formatFullLine("  Возврат (безнал)", money(returnCardSum)));
      line(sb, formatFullLine("  При открытии", money(openingSum)));
      line(sb, formatFullLine("  Оставлено в кассе", money(leftSum)));
      sep(sb);

      // ===== Движение денег =====
      alignCenter(sb);
      line(sb, "ДВИЖЕНИЕ ДЕНЕГ");
      alignLeft(sb);
      line(sb, formatFullLine("Внос средств", money(zeroIfNull(depositedFunds))));
      line(sb, formatFullLine("Вынос средств", money(zeroIfNull(withdrawalFunds))));

      finalizeAndPrint(sb);

    } catch (Exception e) {
      handlePrintException(e);
    }
  }

  /**
   * Печать товарного чека. Шапка по центру, таблица товаров, итоги — «лейбл слева / значение
   * справа».
   */
  public void printReceiptRawWithLine(PaymentReceipt currentReceipt) {
    if (currentReceipt == null) {
      uiNotificationService.showError("Чек для распечатки пустой");
      return;
    }

    refreshAppSettings();

    try {
      StringBuilder sb = new StringBuilder();

      // ===== Шапка =====
      alignCenter(sb);
      if (!isBlank(organizationName)) {
        line(sb, organizationName);
      }
      if (!isBlank(cashierName)) {
        line(sb, cashierName);
      }
      line(sb, "ЧЕК #" + currentReceipt.getId());
      line(sb, dtFormat(currentReceipt.getCreated()));
      line(sb, currentReceipt.getPaymentType().getDisplayName());
      sep(sb);

      // ===== Таблица (товары) =====
      alignLeft(sb);
      printSalesTable(sb, salesService.getSalesByPaymentReceipt(currentReceipt));

      sep(sb);

      // ===== Итоги =====
      line(sb, formatFullLine("ИТОГО", money(zeroIfNull(currentReceipt.getTotal()))));
      line(sb, formatFullLine(currentReceipt.getPaymentType().getDisplayName(),
          money(zeroIfNull(currentReceipt.getReceivedPayment()))));

      if (currentReceipt.getPaymentType() == PaymentType.MIXED) {
        line(sb, formatFullLine("Наличные", money(zeroIfNull(currentReceipt.getCashPayment()))));
        line(sb, formatFullLine("Карта", money(zeroIfNull(currentReceipt.getCardPayment()))));
      }

      line(sb, formatFullLine("СДАЧА", money(zeroIfNull(currentReceipt.getChangeMoney()))));

      finalizeAndPrint(sb);

    } catch (Exception e) {
      handlePrintException(e);
    }
  }

  // Протяжка, обрезка, отправка в принтер.
  private void finalizeAndPrint(StringBuilder sb)
      throws PrintException, UnsupportedEncodingException {
    feed(sb, 4);
    cutPaper(sb);
    sendToPrinter(sb.toString().getBytes("CP1251"));
  }

  // Печатает таблицу: "Товар | Кол-во | Цена | Сумма".
  private void printSalesTable(StringBuilder sb, List<SalesWithProductName> sales) {
    // Заголовок
    line(sb, formatRow("Товар", "Кол-во", "Цена", "Сумма"));

    for (SalesWithProductName sale : sales) {
      String productName = emptyIfNull(sale.productName());
      String qty = sale.quantity() + "шт";
      String price = money(zeroIfNull(sale.soldPrice()));
      String total = money(zeroIfNull(sale.total()));

      List<String> parts = splitByLength(productName);

      // Первая строка — с колонками
      line(sb, formatRow(parts.getFirst(), qty, price, total));

      // Остальные строки — только название
      for (int i = 1; i < parts.size(); i++) {
        String nameOnly = padRight(parts.get(i), 21)
            + padLeft("", 5)
            + padLeft("", 7)
            + padLeft("", 9);
        line(sb, nameOnly);
      }
    }
  }

  // -------------------- Вспомогательные форматтеры --------------------
  // ESC a 0 — влево.
  private void alignLeft(StringBuilder sb) {
    sb.append((char) 0x1B).append('a').append((char) 0);
  }

  //ESC a 1 — центр.
  private void alignCenter(StringBuilder sb) {
    sb.append((char) 0x1B).append('a').append((char) 1);
  }

  // Короткая запись "добавить строку и перевод".
  private void line(StringBuilder sb, String text) {
    sb.append(text).append("\n");
  }

  // Горизонтальный разделитель.
  private void sep(StringBuilder sb) {
    line(sb, "-".repeat(LINE_WIDTH));
  }

  // Протяжка бумаги (n пустых строк).
  private void feed(StringBuilder sb, int n) {
    if (n <= 0) {
      return;
    }
    sb.append("\n".repeat(n));
  }

  // Обрезка бумаги: fullCut = true → полная, false → частичная.
  private void cutPaper(StringBuilder sb) {
    sb.append((char) 0x1D)                 // GS
        .append('V')                         // команда обрезки бумаги
        .append((char) (66))  // 65=A=полная, 66=B=частичная
        .append((char) 0);                   // обрезка сразу
  }

  // Отправка данных в принтер по умолчанию.
  private void sendToPrinter(byte[] data) throws PrintException {
    DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
    PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
    if (printService == null) {
      uiNotificationService.showError("Ошибка печати, Не найден принтер по умолчанию!");
      return;
    }
    DocPrintJob job = printService.createPrintJob();
    Doc doc = new SimpleDoc(data, flavor, null);
    PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
    job.print(doc, attr);
  }

  // Форматирование пары «лейбл слева — значение справа» с переносом длинных лейблов.
  private String formatFullLine(String label, String value) {
    label = emptyIfNull(label);
    value = emptyIfNull(value);

    // помещается в одну строку
    if (label.length() + value.length() <= LINE_WIDTH) {
      int spaces = LINE_WIDTH - label.length() - value.length();
      return label + " ".repeat(Math.max(1, spaces)) + value;
    }

    // переносим лейбл на несколько строк, значение — в последней строке
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < label.length()) {
      int end = Math.min(i + LINE_WIDTH, label.length());
      String part = label.substring(i, end);
      i = end;

      // последняя часть — попробуем уместить значение
      if (i >= label.length() && part.length() + value.length() <= LINE_WIDTH) {
        int spaces = LINE_WIDTH - part.length() - value.length();
        out.append(part)
            .append(" ".repeat(Math.max(1, spaces)))
            .append(value);
      } else {
        out.append(part).append("\n");
      }
    }
    return out.toString();
  }

  // Табличная строка «Товар | Кол-во | Цена | Сумма» (ширины 21/5/7/9).
  private String formatRow(String col1, String col2, String col3, String col4) {
    int w1 = 21, w2 = 5, w3 = 7, w4 = 9;
    return padRight(emptyIfNull(col1), w1)
        + padLeft(emptyIfNull(col2), w2)
        + padLeft(emptyIfNull(col3), w3)
        + padLeft(emptyIfNull(col4), w4);
  }

  private String padRight(String text, int width) {
    text = emptyIfNull(text);
    if (text.length() >= width) {
      return text;
    }
    return text + " ".repeat(width - text.length());
  }

  private String padLeft(String text, int width) {
    text = emptyIfNull(text);
    if (text.length() >= width) {
      return text;
    }
    return " ".repeat(width - text.length()) + text;
  }


  // Разбить длинный текст на части фиксированной длины.
  private List<String> splitByLength(String text) {
    text = emptyIfNull(text);
    int n = Math.max(1, 21);
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < text.length(); i += n) {
      parts.add(text.substring(i, Math.min(i + n, text.length())));
    }
    if (parts.isEmpty()) {
      parts.add("");
    }
    return parts;
  }

  /**
   * Безопасное форматирование денег (через utilNumbers, как у тебя).
   */
  private String money(BigDecimal v) {
    v = zeroIfNull(v);
    try {
      return utilNumbers.formatPrice(v);
    } catch (Exception ignore) {
      return String.format("%.2f", v);
    }
  }

  private static String dtFormat(LocalDateTime dt) {
    return (dt == null) ? "" : DTF.format(dt);
  }

  private void refreshAppSettings() {
    var appSetting = appSettingService.getSingleton().orElse(null);
    if (appSetting == null) {
      organizationName = "";
      cashierName = "";
      bin = "";
    } else {
      organizationName = emptyIfNull(appSetting.getOrgName());
      cashierName = emptyIfNull(appSetting.getSaleStore());
      bin = emptyIfNull(appSetting.getBin());
    }
  }

  private void handlePrintException(Exception e) {
    log.error("Ошибка печати чека", e);
    uiNotificationService.showError(
        "Ошибка печати, Не удалось отправить чек в принтер.\n" + e.getMessage());
  }

  // -------------------- Мелкие утилиты --------------------
  private static BigDecimal zeroIfNull(BigDecimal v) {
    return (v == null) ? BigDecimal.ZERO : v;
  }

  private static String emptyIfNull(String s) {
    return (s == null) ? "" : s;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}