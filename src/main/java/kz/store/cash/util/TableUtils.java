package kz.store.cash.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TableUtils {
  /**
   * Привязывает ширину колонок к ширине таблицы на основе весов.
   *
   * @param table   TableView
   * @param weights массив весов для колонок (например: 0.1, 0.2, 0.7)
   * @param columns колонки в том же порядке
   */
  public static void bindColumnWidths(TableView<?> table, double[] weights, TableColumn<?, ?>... columns) {
    if (weights.length != columns.length) {
      throw new IllegalArgumentException("Количество весов и количество колонок должно совпадать");
    }

    double total = 0;
    for (double w : weights) total += w;
    if (Math.abs(total - 1.0) > 0.001) {
      throw new IllegalArgumentException("Сумма весов должна быть равна 1.0");
    }

    table.widthProperty().addListener((obs, oldVal, newVal) -> {
      double width = newVal.doubleValue();
      for (int i = 0; i < columns.length; i++) {
        columns[i].setPrefWidth(width * weights[i]);
      }
    });
  }
}
