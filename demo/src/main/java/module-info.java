module kz.cash.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens kz.cash.demo to javafx.fxml;
    exports kz.cash.demo;
}