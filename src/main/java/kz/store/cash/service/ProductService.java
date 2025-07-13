package kz.store.cash.service;

import java.util.List;
import kz.store.cash.fx.model.ProductItem;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
  private final List<ProductItem> products = List.of(
      new ProductItem("123456", "салфетка микро фибер 300", 2500.00),
      new ProductItem("654321", "перчатки резиновые", 1800.00),
      new ProductItem("111222", "мыло хозяйственное", 1200.00),
      new ProductItem("111223", "мыло la de creme", 1600.00),
      new ProductItem("111224", "мыло с приятным запахом", 1400.00)
  );

  public ProductItem findByBarcode(String barcode) {
    return products.stream()
        .filter(p -> p.getBarcode().equals(barcode))
        .findFirst()
        .map(p -> new ProductItem(p.getBarcode(), p.getName(), p.getPrice()))
        .orElse(null);
  }

  public List<ProductItem> searchByPartialBarcode(String partial) {
    return products.stream()
        .filter(p -> p.getBarcode().startsWith(partial))
        .map(p -> new ProductItem(p.getBarcode(), p.getName(), p.getPrice()))
        .toList();
  }
}