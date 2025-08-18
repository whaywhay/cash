package kz.store.cash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.entity.QuickProduct;
import kz.store.cash.repository.QuickProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuickProductService {

  private final QuickProductRepository quickRepository;
  private final ProductService productService;

  @Transactional
  public boolean toggle(String barcode) {
    var existing = quickRepository.findByBarcode(barcode);
    if (existing.isPresent()) {
      quickRepository.delete(existing.get());
      return false; // был — удалили
    } else {
      quickRepository.save(createNewQuickProduct(barcode));
      return true; // добавили
    }
  }

  private QuickProduct createNewQuickProduct(String barcode) {
    var newQuickProduct = new QuickProduct();
    newQuickProduct.setBarcode(barcode);
    newQuickProduct.setSortedDate(LocalDateTime.now());
    return newQuickProduct;
  }

  @Transactional(readOnly = true)
  public boolean isQuick(String barcode) {
    return quickRepository.existsByBarcode(barcode);
  }

  /**
   * Список продуктов «быстрых» в порядке новых сверху (sorted_date desc).
   */
  @Transactional(readOnly = true)
  public List<Product> listProducts() {
    var quick = quickRepository.findAllByOrderBySortedDateDesc();
    if (quick.isEmpty()) {
      return List.of();
    }

    var barcodes = quick.stream().map(QuickProduct::getBarcode).collect(Collectors.toSet());
    // достаём одним запросом
    var products = productService.findByBarcodeIn(barcodes);

    // упорядочим по порядку quick (новые сверху)
    var order = quick.stream().map(QuickProduct::getBarcode).toList();
    var index = new HashMap<String, Integer>(order.size());
    for (int i = 0; i < order.size(); i++) {
      index.put(order.get(i), i);
    }

    var sorted = new ArrayList<>(products);
    sorted.sort((a, b) -> {
      int ia = index.getOrDefault(a.getBarcode(), Integer.MAX_VALUE);
      int ib = index.getOrDefault(b.getBarcode(), Integer.MAX_VALUE);
      return Integer.compare(ia, ib);
    });
    return sorted;
  }
}
