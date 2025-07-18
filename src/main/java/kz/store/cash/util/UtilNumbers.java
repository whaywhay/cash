package kz.store.cash.util;


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

}
