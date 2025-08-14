package kz.store.cash.fx.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.TableView;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesCartService {

  private final ProductMapper productMapper;

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

  public List<ProductItem> getMapSalesToProductItem(List<SalesWithProductName> sales) {
    return sales.stream()
        .map(productMapper::toProductItem)
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
