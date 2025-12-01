package com.example.event_app.repositories;
 
import com.example.event_app.models.User;
 
import java.util.Optional;
 
 /**
  * Repository abstraction for persisting and retrieving users.
+ * <p>
+ * This interface lives in the main source set so unit tests (for example
+ * {@code UserServiceTest}) can import it directly without needing any Android
+ * dependencies or Firebase SDK mocks.
+ * </p>
  */
public interface UserRepository {
 
    Optional<User> findById(String userId);
 
    Optional<User> findByDeviceId(String deviceId);
 
    User save(User user);
}
 
EOF
)
