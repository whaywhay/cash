package kz.store.cash.fx.component;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;

public class BarcodeScannerListener {
  private final StringBuilder buffer = new StringBuilder();
  private long lastInputTime = 0;
  private static final long TIME_THRESHOLD = 100;

  private final Consumer<String> onBarcodeScanned;

  public BarcodeScannerListener(Consumer<String> handler) {
    this.onBarcodeScanned = handler;
  }

  public void attachTo(Scene scene) {
    scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
      long now = System.currentTimeMillis();

      if (now - lastInputTime > TIME_THRESHOLD) {
        buffer.setLength(0);
      }

      lastInputTime = now;

      String ch = event.getCharacter();
      if ("\r\n".contains(ch)) {
        String barcode = buffer.toString().trim();
        buffer.setLength(0);

        if (!barcode.isEmpty()) {
          Platform.runLater(() -> onBarcodeScanned.accept(barcode));
        }
      } else {
        buffer.append(ch);
      }
    });
  }
}