package kz.store.cash.service;

import static kz.store.cash.model.enums.CashShiftStatus.OPENED;
import static kz.store.cash.model.enums.ReceiptStatus.PENDING;
import static kz.store.cash.model.enums.ReceiptStatus.SALE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import org.springframework.data.domain.Pageable;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.PaymentReceiptMapper;
import kz.store.cash.mapper.SalesMapper;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.repository.CashShiftRepository;
import kz.store.cash.repository.PaymentReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentReceiptServiceTest {
  PaymentReceiptMapper receiptMapper = mock(PaymentReceiptMapper.class);
  PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
  CashShiftRepository shifts = mock(CashShiftRepository.class);
  SalesService sales = mock(SalesService.class);
  SalesMapper salesMapper = mock(SalesMapper.class);
  PaymentReceiptService service;
  CashShift opened;

  @BeforeEach void setUp() {
    service = new PaymentReceiptService(receiptMapper, receipts, shifts, sales, salesMapper);
    opened = new CashShift(); opened.setStatus(OPENED);
  }

  @Test void processesPaymentAndAttachesSalesAndOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class);
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem item = item(null); Sales sale = new Sales();
    when(receiptMapper.saleToPaymentReceipt(details)).thenReturn(receipt);
    when(salesMapper.fromProductItemToSales(item, receipt)).thenReturn(sale);
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    service.processPayment(details, FXCollections.observableArrayList(item));
    assertThat(receipt.getCashShift()).isSameAs(opened);
    assertThat(receipt.getSalesList()).containsExactly(sale);
    verify(receipts).save(receipt);
  }

  @Test void paymentFailsWithoutOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class); PaymentReceipt receipt = new PaymentReceipt();
    when(receiptMapper.saleToPaymentReceipt(details)).thenReturn(receipt);
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.processPayment(details, FXCollections.observableArrayList()))
        .isInstanceOf(BusinessException.class);
    verify(receipts, never()).save(any());
  }

  @Test void createsAndDeletesDeferredReceipt() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    when(receipts.save(any())).thenAnswer(i -> i.getArgument(0));
    PaymentReceipt receipt = service.createDeferredPaymentReceipt();
    assertThat(receipt.getReceiptStatus()).isEqualTo(PENDING);
    assertThat(receipt.getCashShift()).isSameAs(opened);
    receipt.setId(10L); service.deleteDeferredPaymentReceipt(receipt);
    verify(receipts).deleteById(10L);
  }

  @Test void emptyCartDeletesExistingDeferredReceipt() {
    PaymentReceipt receipt = new PaymentReceipt(); receipt.setId(3L);
    service.mergeDeferredPaymentReceipts(receipt, List.of());
    verify(receipts).deleteById(3L);
    verifyNoInteractions(sales);
  }

  @Test void mergeCreatesNewAndSavesNewSale() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    when(receipts.save(any())).thenAnswer(i -> i.getArgument(0));
    ProductItem item = item(null); Sales mapped = new Sales();
    when(sales.findAllById(List.of())).thenReturn(List.of());
    when(salesMapper.fromProductItemToSales(eq(item), any(PaymentReceipt.class))).thenReturn(mapped);
    service.mergeDeferredPaymentReceipts(null, List.of(item));
    verify(sales).deleteSalesByPaymentReceipt(any(PaymentReceipt.class));
    verify(sales).saveAll(List.of(mapped));
  }

  @Test void mergeUpdatesExistingSale() {
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem item = item(8L);
    Sales existing = new Sales(); existing.setId(8L);
    when(sales.findAllById(List.of(8L))).thenReturn(List.of(existing));
    service.mergeDeferredPaymentReceipts(receipt, List.of(item));
    verify(sales).deleteSalesByIdNotInAndPaymentReceipt(receipt, List.of(8L));
    verify(salesMapper).updateToSale(existing, item, receipt);
    verify(sales).saveAll(List.of(existing));
  }

  @Test void mergeFailsWhenReferencedSaleIsMissing() {
    ProductItem item = item(99L);
    when(sales.findAllById(List.of(99L))).thenReturn(List.of());
    assertThatThrownBy(() -> service.mergeDeferredPaymentReceipts(new PaymentReceipt(), List.of(item)))
        .isInstanceOf(BusinessException.class).hasMessageContaining("99");
  }

  @Test void aggregateMethodsDelegateToRepository() {
    service.getSumCash(opened); service.getSumCard(opened); service.getReturnedSumCash(opened);
    service.getReturnedSumCard(opened); service.getDebtSum(opened); service.getDebtReturnSum(opened);
    verify(receipts).sumCashByShiftAndStatus(eq(opened), any(), any());
    verify(receipts).sumCardByShiftAndStatus(eq(opened), any(), any());
    verify(receipts).sumReturnCashByShiftAndStatus(eq(opened), any());
    verify(receipts).sumReturnCardByShiftAndStatus(eq(opened), any());
    verify(receipts).sumDebtTotalByShiftAndStatus(eq(opened), any());
    verify(receipts).sumDebtReturnTotalByShiftAndStatus(eq(opened), any());
  }

  @Test void processDebtMapsDebtReceiptAndAttachesSalesAndOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class);
    DiaryTransaction tx = mock(DiaryTransaction.class);
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem item = item(null); Sales sale = new Sales();
    when(receiptMapper.debtToPaymentReceipt(details, tx)).thenReturn(receipt);
    when(salesMapper.fromProductItemToSales(item, receipt)).thenReturn(sale);
    openShift();
    service.processDebt(details, FXCollections.observableArrayList(item), tx);
    assertThat(receipt.getCashShift()).isSameAs(opened);
    assertThat(receipt.getSalesList()).containsExactly(sale);
    verify(receipts).save(receipt);
  }

  @Test void deferredPaymentCreatesNewSalesAndUpdatesExistingOnes() {
    PaymentReceipt receipt = new PaymentReceipt(); PaymentSumDetails details = mock(PaymentSumDetails.class);
    ProductItem fresh = item(null), saved = item(5L);
    Sales created = new Sales(), existing = new Sales();
    when(salesMapper.fromProductItemToSales(fresh, receipt)).thenReturn(created);
    when(sales.getSaleById(5L)).thenReturn(existing);
    openShift();
    service.processPaymentSaveWithDeferredPayment(receipt, details,
        FXCollections.observableArrayList(fresh, saved));
    verify(receiptMapper).updateDeferredToPaymentReceipt(receipt, details);
    verify(salesMapper).updateToSale(existing, saved, receipt);
    assertThat(receipt.getSalesList()).containsExactly(created, existing);
    assertThat(receipt.getCashShift()).isSameAs(opened);
    verify(receipts).save(receipt);
  }

  @Test void deferredPaymentSkipsSaleThatNoLongerExists() {
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem gone = item(404L);
    when(sales.getSaleById(404L)).thenReturn(null);
    openShift();
    service.processPaymentSaveWithDeferredPayment(receipt, mock(PaymentSumDetails.class),
        FXCollections.observableArrayList(gone));
    assertThat(receipt.getSalesList()).isEmpty();
    verify(salesMapper, never()).updateToSale(any(), any(), any());
    verify(receipts).save(receipt);
  }

  @Test void debtWithDeferredPaymentUpdatesReceiptFromDiaryTransaction() {
    PaymentReceipt receipt = new PaymentReceipt(); PaymentSumDetails details = mock(PaymentSumDetails.class);
    DiaryTransaction tx = mock(DiaryTransaction.class); ProductItem fresh = item(null); Sales created = new Sales();
    when(salesMapper.fromProductItemToSales(fresh, receipt)).thenReturn(created);
    openShift();
    service.processDebtWithDeferredPayment(receipt, details, FXCollections.observableArrayList(fresh), tx);
    verify(receiptMapper).updateDebtToPaymentReceipt(receipt, details, tx);
    assertThat(receipt.getSalesList()).containsExactly(created);
    verify(receipts).save(receipt);
  }

  @Test void returnSaveBuildsReturnSalesAndAttachesOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class);
    PaymentReceipt original = new PaymentReceipt(), ret = new PaymentReceipt();
    ProductItem item = item(null); Sales returned = new Sales();
    when(receiptMapper.toReturnPaymentReceipt(details, original)).thenReturn(ret);
    when(salesMapper.fromProductItemToReturnSales(item, ret)).thenReturn(returned);
    openShift();
    service.processReturnSave(details, FXCollections.observableArrayList(item), original);
    assertThat(ret.getSalesList()).containsExactly(returned);
    assertThat(ret.getCashShift()).isSameAs(opened);
    verify(receipts).save(ret);
  }

  @Test void returnSaveFailsWithoutOpenShiftAndSavesNothing() {
    when(receiptMapper.toReturnPaymentReceipt(any(), any())).thenReturn(new PaymentReceipt());
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.processReturnSave(mock(PaymentSumDetails.class),
        FXCollections.observableArrayList(), new PaymentReceipt()))
        .isInstanceOf(BusinessException.class);
    verify(receipts, never()).save(any());
  }

  @Test void deferredReceiptsAreLoadedForOpenShiftOnly() {
    PaymentReceipt pending = new PaymentReceipt();
    openShift();
    when(receipts.getAllByReceiptStatusAndCashShift(PENDING, opened)).thenReturn(List.of(pending));
    assertThat(service.getDeferredPaymentReceipts()).containsExactly(pending);
  }

  @Test void deferredReceiptsFailWithoutOpenShift() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getDeferredPaymentReceipts()).isInstanceOf(BusinessException.class);
  }

  @Test void subtractReturnedReturnsOriginalsWhenNothingWasReturned() {
    PaymentReceipt receipt = new PaymentReceipt();
    List<SalesWithProductName> original = List.of(line("111", "10.00", 3));
    when(receipts.getPaymentReceiptByOriginalReceipt(receipt)).thenReturn(List.of());
    assertThat(service.subtractReturnedSales(receipt, original)).isSameAs(original);
    verifyNoInteractions(sales);
  }

  @Test void subtractReturnedReducesQuantityAndRecalculatesTotal() {
    PaymentReceipt receipt = new PaymentReceipt(), ret = new PaymentReceipt();
    when(receipts.getPaymentReceiptByOriginalReceipt(receipt)).thenReturn(List.of(ret));
    when(sales.getSalesByPaymentReceiptIn(List.of(ret))).thenReturn(List.of(
        returnedSale("111", "10.00", 1), returnedSale("111", "10.00", 1)));
    List<SalesWithProductName> result = service.subtractReturnedSales(receipt,
        List.of(line("111", "10.00", 5)));
    assertThat(result).singleElement().satisfies(l -> {
      assertThat(l.quantity()).isEqualTo(3);
      assertThat(l.total()).isEqualByComparingTo("30.00");
    });
  }

  @Test void subtractReturnedDropsFullyReturnedLinesAndKeepsOthers() {
    PaymentReceipt receipt = new PaymentReceipt(), ret = new PaymentReceipt();
    when(receipts.getPaymentReceiptByOriginalReceipt(receipt)).thenReturn(List.of(ret));
    when(sales.getSalesByPaymentReceiptIn(List.of(ret))).thenReturn(List.of(returnedSale("111", "10.00", 2)));
    List<SalesWithProductName> result = service.subtractReturnedSales(receipt,
        List.of(line("111", "10.00", 2), line("222", "5.00", 1)));
    assertThat(result).extracting(SalesWithProductName::barcode).containsExactly("222");
  }

  @Test void subtractReturnedMatchesByBarcodeAndSoldPrice() {
    PaymentReceipt receipt = new PaymentReceipt(), ret = new PaymentReceipt();
    when(receipts.getPaymentReceiptByOriginalReceipt(receipt)).thenReturn(List.of(ret));
    when(sales.getSalesByPaymentReceiptIn(List.of(ret))).thenReturn(List.of(returnedSale("111", "8.00", 1)));
    List<SalesWithProductName> result = service.subtractReturnedSales(receipt,
        List.of(line("111", "10.00", 1)));
    assertThat(result).singleElement().extracting(SalesWithProductName::quantity).isEqualTo(1);
  }

  @Test void searchAndLookupMethodsDelegateToRepository() {
    Pageable page = Pageable.unpaged(); PaymentReceipt newest = new PaymentReceipt(), original = new PaymentReceipt();
    LocalDateTime from = LocalDateTime.now().minusDays(1), to = LocalDateTime.now();
    when(receipts.getFirstByReceiptStatusOrderByIdDesc(SALE)).thenReturn(newest);
    when(receipts.getPaymentReceiptByOriginalReceipt(original)).thenReturn(List.of(newest));
    service.findByCreatedDate(from, to, page); service.findById(4L, page);
    verify(receipts).findByCreatedDate(from, to, page);
    verify(receipts).findById(4L, page);
    assertThat(service.getNewestSalePaymentReceipt()).isSameAs(newest);
    assertThat(service.getReturnPaymentReceipt(original)).containsExactly(newest);
  }

  private void openShift() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
  }

  private static SalesWithProductName line(String barcode, String price, int qty) {
    BigDecimal p = new BigDecimal(price);
    return new SalesWithProductName(1L, barcode, "name", p, p, p, qty, p.multiply(BigDecimal.valueOf(qty)), false, null);
  }

  private static Sales returnedSale(String barcode, String price, int qty) {
    Sales s = new Sales(); s.setBarcode(barcode); s.setSoldPrice(new BigDecimal(price)); s.setQuantity(qty);
    return s;
  }

  private ProductItem item(Long id) { ProductItem item = new ProductItem("b", "p", 1, 1); item.setSalesId(id); return item; }
}
