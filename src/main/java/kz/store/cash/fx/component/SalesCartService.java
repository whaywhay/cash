package kz.store.cash.fx.component;

import java.util.List;
import javafx.scene.control.TableView;
import kz.store.cash.fx.model.ProductItem;
import org.springframework.stereotype.Component;

@Component
public class SalesCartService {

  public double calculateTotal(List<ProductItem> cart) {
    return cart.stream().mapToDouble(ProductItem::getTotal).sum();
  }

  public ProductItem findByBarcodeInCartList(List<ProductItem> cart, ProductItem productItem) {
    return cart.stream()
        .filter(p -> p.getBarcode().equals(productItem.getBarcode()))
        .findFirst()
        .orElse(null);
  }

  public void increaseOrAddNewToCart(List<ProductItem> cart, ProductItem productItem,
      TableView<ProductItem> salesTable) {
    ProductItem existing = findByBarcodeInCartList(cart, productItem);
    if (existing != null) {
      existing.increaseQuantity();
      salesTable.getSelectionModel().select(existing);
    } else {
      cart.add(productItem);
      salesTable.getSelectionModel().select(productItem);
    }
    salesTable.refresh();
  }

}
