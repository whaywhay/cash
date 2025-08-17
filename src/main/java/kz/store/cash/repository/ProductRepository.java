package kz.store.cash.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kz.store.cash.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

  Optional<Product> findFirstByBarcode(String barcode);

  List<Product> findByBarcodeLikeOrProductNameLikeIgnoreCase(String barCode, String productName);

  List<Product> findByBarcodeIn(Collection<String> barcodes);

  List<Product> findByCategoryRef_CategoryCode(String categoryCode);
}
