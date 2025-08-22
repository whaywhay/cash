package kz.store.cash.security;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import kz.store.cash.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserStore {
  private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>(null);

  public ObjectProperty<User> currentUserProperty() { return currentUser; }
  public User get() { return currentUser.get(); }
  public void set(User u) { currentUser.set(u); }


  public BooleanBinding loggedInBinding() {
    return currentUser.isNotNull();
  }
  public boolean isLoggedIn() { return get() != null; }
}
