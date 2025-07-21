package kz.store.cash.service;

import java.math.BigDecimal;
import java.util.List;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductMapper productMapper;

  public ProductItem findByBarcode(String barcode) {
    return productRepository.findFirstByBarcode(barcode)
        .map(productMapper::toProductItem)
        .orElse(null);
  }

  public List<ProductItem> searchByPartialBarcode(String partial) {
    String barCode = partial + "%";
    String productName = "%" + partial + "%";
    return productRepository.findByBarcodeLikeOrProductNameLikeIgnoreCase(barCode, productName)
        .stream()
        .map(productMapper::toProductItem)
        .toList();
  }

  public void updateRetailPrice(String barcode, double retailPrice) {
    if (retailPrice <= 0) {
      return;
    }
    productRepository.findFirstByBarcode(barcode)
        .ifPresent(prod -> {
          prod.setOriginalPrice(BigDecimal.valueOf(retailPrice));
          productRepository.save(prod);
        });


  }
}