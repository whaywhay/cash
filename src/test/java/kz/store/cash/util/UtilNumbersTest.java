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

  @Test void formatsDoubleWithTwoFractionDigits() {
    assertThat(UtilNumbers.formatDouble(12.5)).isEqualTo("12.50");
  }

  @Test void formatsWholeBigDecimalWithoutFraction() {
    assertThat(new UtilNumbers().formatPrice(new BigDecimal("12.00"))).isEqualTo("12");
  }
}
