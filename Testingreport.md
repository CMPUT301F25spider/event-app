# Testing report - LuckySpot Event App

```bash
./gradlew test

./gradlew test --tests "com.example.event_app.domain.ResultTest"

./gradlew test --info
```
## Current Status
- **Execution**: `./gradlew test` currently fails in this environment because the Android SDK location is not configured; configure `ANDROID_HOME`/`sdk.dir` locally or in CI before running.
- **Scope**: All tests rely on dummy data, mocked Firebase/Auth/Firestore, or Robolectric shadows—no live services or UI automation.

# COVERAGE 
```


## Coverage Summary (unit tests)
- **ActivityLifecycleTest** – 21 lifecycle/intent wiring checks across every activity (entrant, organizer, admin) using mocked Firebase and dummy intent extras.
- **ActivityIntentFlowTest** – 6 intent payload validations for navigation targets.
- **NavigatorTest** – Verifies event navigation intent is launched with the correct extra.
- **QRServiceTest** – 3 QR-code validation paths (valid, invalid, blank) exercising navigation decisions.
- **UserServiceTest** – 6 service behaviors including validation failures, persistence interactions, and favorite/notification toggles with dummy users.
- **UserValidatorTest** – Valid and invalid user data permutations plus required-field aggregation.
- **EventModelTest** – 6 event-derived calculations (spots remaining, cancellation rate, capacity full, replacement pool, past-event logic).
 

**Total: 45 test methods across 7 test classes**

### Unit Test Template

```java
package com.example.event_app.domain;

import org.junit.Test;
import static org.junit.Assert.*;

public class YourClassTest {
    @Test
    public void testMethodName_Scenario_ExpectedResult() {
        // Arrange
        YourClass instance = new YourClass();
        
        // Act
        Result result = instance.method();
        
        // Assert
        assertEquals("Expected value", expected, result);
    }
}
```

# Testname convention

- Test method names: `testMethodName_Scenario_ExpectedResult`
- Example: `testGetCancellationRate_ZeroSelected_ReturnsZero`

# Problems during testing

### Tests Not Running in CI

1. **Check Android SDK setup** - Ensure API level matches `compileSdk` in `build.gradle.kts`
2. **Check JDK version** - Must match `sourceCompatibility` (currently Java 17)
3. **Check workflow file** - Ensure `.github/workflows/android.yml` exists and is valid YAML

 ### Tests Failing Locally

1. **Android SDK not found**: Set `ANDROID_HOME` environment variable
2. **Gradle daemon issues**: Run with `--no-daemon` flag
3. **Dependencies**: Run `./gradlew --refresh-dependencies`

# future implementatuons

1. ** Jacoco implementation
   ```kotlin
   plugins {
       id("jacoco")
   }
   ```

2. **More Test Coverage needed ** for part 4:
   - QR validation 
   - Navigator
   - Repo query
   - Activity form
