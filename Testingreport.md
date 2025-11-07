# Testing report - LuckySpot Event App

```bash
./gradlew test

./gradlew test --tests "com.example.event_app.domain.ResultTest"

./gradlew test --info

#NOT IMPLEMENTED, COVERAGE IS NOT COMPLETE
./gradlew test jacocoTestReport
```
# COVERAGE 
```

##Test Coverage

Result - ResultTest.java : 5 methods Successful
EntrantsFilter - EntrantsFilterTest.java : 12 methods Successful
Event - EventTest.java : 13 methods Successful
UserRole - UserRoleTest.java : 5 methods Successful 
Basic Tests - ExampleUnitTest.java  

**Total: 37 test methods across 5 test classes**

##Test Details

ResultTest.java
- Tests success cases (`Result.ok()`)
- Tests error cases with Throwable and string random
- Multiple data types

EntrantsFilterTest.java
- Tests enum values
- Tests case matching

EventTest.java
- Tests cancellation rate Computation
- Tests edge cases 0, 100+
- Tests constructors

UserRoleTest.java
- Tests all role constants

# thorough Null testing in all 5


**Removed `.github/workflows/ant.yml`** - Was trying to build with Ant (not applicable for Android/Gradle project)

# Current Status

1. **Single CI Workflow**: Only one workflow file (android.yml) - no conflicts
2. **Proper Android Setup**: Android SDK configured correctly
3. **Test Execution**: Tests explicitly run before build
4. **Error Handling**: Proper error handling and artifact upload


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
