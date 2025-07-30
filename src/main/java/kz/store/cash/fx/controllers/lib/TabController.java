package kz.store.cash.fx.controllers.lib;

public interface TabController {

  /**
   * Вызывается каждый раз, когда вкладка активируется
   */
  default void onTabSelected() {
    // по умолчанию ничего не делаем
  }
}
