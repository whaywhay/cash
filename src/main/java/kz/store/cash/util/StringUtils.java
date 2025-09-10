package kz.store.cash.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {

  public static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  public static String trimSafely(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

}
