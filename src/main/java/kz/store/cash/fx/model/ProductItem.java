package kz.store.cash.fx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProductItem {

  private final StringProperty barcode = new SimpleStringProperty();
  private final StringProperty name = new SimpleStringProperty();
  private final DoubleProperty originalPrice = new SimpleDoubleProperty();
  private final DoubleProperty wholesalePrice = new SimpleDoubleProperty();
  private final DoubleProperty price = new SimpleDoubleProperty(); // текущее отображаемое значение
  private final IntegerProperty quantity = new SimpleIntegerProperty(1);
  private final BooleanProperty selected = new SimpleBooleanProperty(false);

  public ProductItem(String barcode, String name, double originalPrice, double wholesalePrice) {
    this.barcode.set(barcode);
    this.name.set(name);
    this.originalPrice.set(originalPrice);
    this.wholesalePrice.set(wholesalePrice);
    this.price.set(originalPrice); // по умолчанию отображается обычная цена
  }

  public String getBarcode() {
    return barcode.get();
  }

  public String getName() {
    return name.get();
  }

  public double getPrice() {
    return price.get();
  }

  public int getQuantity() {
    return quantity.get();
  }

  public boolean isSelected() {
    return selected.get();
  }

  public void setSelected(boolean value) {
    selected.set(value);
  }

  public double getTotal() {
    return getPrice() * getQuantity();
  }

  public void setToOriginalPrice() {
    price.set(originalPrice.get());
  }

  public void setToWholesalePrice() {
    price.set(wholesalePrice.get());
  }

  public synchronized void increaseQuantity() {
    quantity.set(getQuantity() + 1);
  }

  public void decreaseQuantity() {
    if (getQuantity() > 1) {
      quantity.set(getQuantity() - 1);
    }
  }

  public StringProperty nameProperty() {
    return name;
  }

  public DoubleProperty priceProperty() {
    return price;
  }

  public IntegerProperty quantityProperty() {
    return quantity;
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }
}
