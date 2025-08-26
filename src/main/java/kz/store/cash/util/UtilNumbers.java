package kz.store.cash.util;


import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import org.springframework.stereotype.Component;

@Component
public class UtilNumbers {

  public static double parseDoubleAmount(String text) {
    if (text == null) {
      return 0.0;
    }
    String cleaned = text.replaceAll("[^\\d.,]", "")
        .replaceAll("\\s+", "");
    if (cleaned.isEmpty()) {
      return 0.0;
    }
    cleaned = cleaned.replace(',', '.');
    try {
      return Double.parseDouble(cleaned);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  public static String formatDouble(double doubleValue) {
    return String.format(Locale.US, "%.2f", doubleValue);
  }

  public void setupDecimalFilter(TextField textField) {
    UnaryOperator<Change> filter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("\\d*(\\.\\d{0,2})?")) {
        return change;
      } else {
        return null;
      }
    };
    textField.setTextFormatter(new TextFormatter<>(filter));
  }

  public void setupIntegerFilter(TextField textField, Predicate<Integer> rule) {
    UnaryOperator<Change> filter = change -> {
      String newText = change.getControlNewText();
      if (newText.isEmpty()) {
        return change; // разрешить удаление всего текста
      }
      if (!newText.matches("\\d+")) {
        return null;
      }
      try {
        int value = Integer.parseInt(newText);
        if (!rule.test(value)) {
          return null;
        }
      } catch (NumberFormatException e) {
        return null;
      }
      return change;
    };
    textField.setTextFormatter(new TextFormatter<>(filter));
  }

  public void setupLongFilter(TextField textField, Predicate<Long> rule) {
    UnaryOperator<Change> filter = change -> {
      String newText = change.getControlNewText();
      if (newText.isEmpty()) {
        return change; // разрешить удаление всего текста
      }
      if (!newText.matches("\\d+")) {
        return null;
      }
      try {
        long value = Long.parseLong(newText);
        if (!rule.test(value)) {
          return null;
        }
      } catch (NumberFormatException e) {
        return null;
      }
      return change;
    };
    textField.setTextFormatter(new TextFormatter<>(filter));
  }

  public String formatPrice(BigDecimal value) {
    if (value.stripTrailingZeros().scale() <= 0) {
      return String.format("%d", value.longValue());
    } else {
      return String.format("%.2f", value);
    }
  }
}
