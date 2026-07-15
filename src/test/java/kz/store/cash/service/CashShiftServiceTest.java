package kz.store.cash.service;

import static kz.store.cash.model.enums.CashShiftStatus.CLOSED;
import static kz.store.cash.model.enums.CashShiftStatus.OPENED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kz.store.cash.fx.component.ReceiptPrintService;
import kz.store.cash.mapper.CashMovementMapper;
import kz.store.cash.mapper.CashShiftMapper;
import kz.store.cash.model.entity.CashMovement;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.CashMovementType;
import kz.store.cash.repository.CashMovementRepository;
import kz.store.cash.repository.CashShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashShiftServiceTest {
  @Mock CashShiftRepository shifts;
  @Mock CashMovementRepository movements;
  @Mock CashShiftMapper shiftMapper;
  @Mock CashMovementMapper movementMapper;
  @Mock PaymentReceiptService receipts;
  @Mock ReceiptPrintService printer;
  CashShiftService service;

  @BeforeEach void setUp() {
    service = new CashShiftService(shifts, movements, shiftMapper, movementMapper, receipts, printer);
  }

  @Test void opensShiftWithCashLeftFromLastClosedShift() {
    User user = new User();
    CashShift closed = shift(1L, CLOSED);
    closed.setLeftInDrawer(new BigDecimal("12500"));
    CashShift opened = shift(2L, OPENED);
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.empty());
    when(shifts.findFirstByStatusOrderByShiftClosedDateDesc(CLOSED)).thenReturn(Optional.of(closed));
    when(shiftMapper.toOpenCashShift(user, new BigDecimal("12500"))).thenReturn(opened);
    when(shifts.save(opened)).thenReturn(opened);

    assertThat(service.openShift(user)).isSameAs(opened);
  }

  @Test void refusesToOpenSecondShift() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED))
        .thenReturn(Optional.of(shift(1L, OPENED)));
    assertThatThrownBy(() -> service.openShift(new User()))
        .isInstanceOf(IllegalStateException.class);
    verify(shifts, never()).save(any());
  }

  @Test void addsMovementOnlyToOpenShift() {
    CashShift shift = shift(1L, OPENED);
    User actor = new User();
    CashMovement movement = new CashMovement();
    when(shifts.findById(1L)).thenReturn(Optional.of(shift));
    when(movementMapper.toCashMovement(shift, CashMovementType.IN, BigDecimal.TEN, "deposit", actor))
        .thenReturn(movement);

    service.addMovement(1L, CashMovementType.IN, BigDecimal.TEN, "deposit", actor);
    verify(movements).save(movement);
  }

  @Test void refusesMovementForClosedShift() {
    when(shifts.findById(1L)).thenReturn(Optional.of(shift(1L, CLOSED)));
    assertThatThrownBy(() -> service.addMovement(1L, CashMovementType.OUT, BigDecimal.ONE, "x", new User()))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(movementMapper, movements);
  }

  @Test void refusesToCloseShiftWithDeferredReceipts() {
    when(shifts.findById(1L)).thenReturn(Optional.of(shift(1L, OPENED)));
    when(receipts.getDeferredPaymentReceipts()).thenReturn(List.of(new PaymentReceipt()));
    assertThatThrownBy(() -> service.closeShift(1L, BigDecimal.ZERO, null, new User()))
        .isInstanceOf(RuntimeException.class);
    verify(shifts, never()).save(any());
    verifyNoInteractions(printer);
  }

  @Test void closesAndPrintsShiftWithAggregatedMovements() {
    CashShift shift = shift(1L, OPENED);
    when(shifts.findById(1L)).thenReturn(Optional.of(shift));
    when(receipts.getDeferredPaymentReceipts()).thenReturn(List.of());
    when(receipts.getSumCash(shift)).thenReturn(new BigDecimal("100"));
    when(receipts.getSumCard(shift)).thenReturn(new BigDecimal("200"));
    when(receipts.getReturnedSumCash(shift)).thenReturn(BigDecimal.TEN);
    when(receipts.getReturnedSumCard(shift)).thenReturn(BigDecimal.ONE);
    when(receipts.getDebtSum(shift)).thenReturn(new BigDecimal("30"));
    when(receipts.getDebtReturnSum(shift)).thenReturn(new BigDecimal("5"));
    when(movements.findAllByCashShiftIdOrderByCreatedDesc(1L)).thenReturn(List.of(
        movement(CashMovementType.IN, new BigDecimal("50")),
        movement(CashMovementType.IN, null),
        movement(CashMovementType.OUT, new BigDecimal("20"))));
    when(shifts.save(shift)).thenReturn(shift);
    User user = new User();

    service.closeShift(1L, new BigDecimal("120"), "ok", user);

    verify(shiftMapper).toCloseCashShift(shift, user, new BigDecimal("120"),
        new BigDecimal("100"), new BigDecimal("200"), BigDecimal.TEN, BigDecimal.ONE,
        "ok", new BigDecimal("30"), new BigDecimal("5"));
    verify(printer).printCashShift(shift, new BigDecimal("50"), new BigDecimal("20"),
        new BigDecimal("30"), new BigDecimal("5"));
  }

  private CashShift shift(Long id, kz.store.cash.model.enums.CashShiftStatus status) {
    CashShift shift = new CashShift();
    shift.setId(id);
    shift.setStatus(status);
    return shift;
  }

  private CashMovement movement(CashMovementType type, BigDecimal amount) {
    CashMovement movement = new CashMovement();
    movement.setType(type);
    movement.setAmount(amount);
    return movement;
  }
}
