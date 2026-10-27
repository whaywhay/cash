package kz.store.cash.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.Test;

class UtilNumbersTest {
  @ParameterizedTest
  @CsvSource({"'123.45',123.45", "'123,45',123.45", "'₸ 1 234,50',1234.50", "'abc',0"})
  void parsesUserAmounts(String input, double expected) {
    assertThat(UtilNumbers.parseDoubleAmount(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullSource
  void nullAmountIsZero(String input) {
    assertThat(UtilNumbers.parseDoubleAmount(input)).isZero();
  }

  @ParameterizedTest
  @CsvSource({"0.1,0.10", "12.345,12.35", "12.344,12.34", "0.005,0.01", "100,100.00", "-2.675,-2.68"})
  void toMoneyAlwaysHasTwoDigitsRoundedHalfUp(double input, String expected) {
    assertThat(UtilNumbers.toMoney(input)).isEqualTo(new BigDecimal(expected));
  }

  @Test void toMoneyRemovesBinaryNoiseOfDoubleArithmetic() {
    assertThat(UtilNumbers.toMoney(0.1 * 7)).isEqualTo(new BigDecimal("0.70"));
    assertThat(UtilNumbers.toMoney(0.1 + 0.2)).isEqualTo(new BigDecimal("0.30"));
  }

  @ParameterizedTest
  @CsvSource({"'',0", "'.',0", "'..',0", "'1.2.3',0", "'12abc',12", "'1 000',1000", "'0,5',0.5"})
  void parsesEdgeCaseAmounts(String input, double expected) {
    assertThat(UtilNumbers.parseDoubleAmount(input)).isEqualTo(expected);
  }

  @Test void formatDoubleIgnoresDefaultLocale() {
    java.util.Locale old = java.util.Locale.getDefault();
    try {
      java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"));
      assertThat(UtilNumbers.formatDouble(1234.5)).isEqualTo("1234.50");
    } finally {
      java.util.Locale.setDefault(old);
    }
  }

  @Test void formatPriceKeepsTwoDigitsForFractionalValues() {
    java.util.Locale old = java.util.Locale.getDefault();
    try {
      java.util.Locale.setDefault(java.util.Locale.US);
      UtilNumbers util = new UtilNumbers();
      assertThat(util.formatPrice(new BigDecimal("12.50"))).isEqualTo("12.50");
      assertThat(util.formatPrice(new BigDecimal("12.5"))).isEqualTo("12.50");
      assertThat(util.formatPrice(new BigDecimal("1200"))).isEqualTo("1200");
      assertThat(util.formatPrice(new BigDecimal("0.00"))).isEqualTo("0");
    } finally {
      java.util.Locale.setDefault(old);
    }
  }

  @Test void formatsDoubleWithTwoFractionDigits() {
    assertThat(UtilNumbers.formatDouble(12.5)).isEqualTo("12.50");
  }

  @Test void formatsWholeBigDecimalWithoutFraction() {
    assertThat(new UtilNumbers().formatPrice(new BigDecimal("12.00"))).isEqualTo("12");
  }
}
