**1. KMP vs. Flutter vs. React Native**
Flutter bypasses native UI components and draws its own widgets directly onto a canvas through its own rendering engine (Impeller/Skia). React Native runs JavaScript in a separate runtime and talks to real native views across a JS bridge (or JSI). Kotlin Multiplatform doesn't touch the UI by default; instead, shared Kotlin code is compiled directly into a native iOS framework (`.framework` / `.xcframework`) that the iOS app calls just like regular native code.

---

**2. How Kotlin Reaches Each Target**

* **Android:** Compiled to JVM bytecode via `Kotlin/JVM`.
* **iOS:** Compiled to native machine code (binaries/frameworks) using **`Kotlin/Native`** (backed by LLVM).
* **Browser:** Reached via two options—`Kotlin/JS` (transpiled to JavaScript) or `Kotlin/Wasm` (compiled to WebAssembly).
* **Desktop:** Compiled to JVM bytecode via `Kotlin/JVM` (or native binaries via `Kotlin/Native`).
* **Server:** Compiled to JVM bytecode via `Kotlin/JVM`.

---

**3. Shared (`commonMain`) vs. Kept Native**

* **Ktor API client — Shared (`commonMain`):** Network requests, serialization, and endpoints are identical across platforms, while Ktor swaps out the underlying HTTP engine automatically.
* **Biometric login — Kept native:** It relies heavily on OS-specific hardware APIs and system prompts (`BiometricPrompt` on Android, `LocalAuthentication` / FaceID on iOS).
* **Input validation — Shared (`commonMain`):** It is pure business logic (checking strings and regexes) with zero platform dependencies.
* **Push notifications — Kept native:** Requires tight integration with platform-specific OS services and lifecycle hooks (FCM on Android, APNs on iOS).
* **SQLDelight storage — Shared (`commonMain`):** Database schemas and queries are 100% identical, requiring only a platform-specific SQLite driver under the hood.
* **Deep-link navigation — Kept native:** Deep links are intercepted by OS-level entry points (`Intent` filters on Android, Universal Links on iOS) that tie directly into native navigation graphs.

---

**4. Complete the `expect`/`actual` Setup**

```kotlin
// androidMain
class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
actual fun getPlatform(): Platform = AndroidPlatform()

// iosMain
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
actual fun getPlatform(): Platform = IOSPlatform()

// jvmMain (Desktop)
class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}
actual fun getPlatform(): Platform = JVMPlatform()

```

---

**5. Find and Fix the Error**
**Error:** An `expect` function in `commonMain` only declares a contract; it cannot have a function body (implementation).
**Fix:** Remove the body `{ return 0L }` so it is just a signature:

```kotlin
// commonMain
expect fun currentTimeMillis(): Long

```

---

**6. Missing `actual` Implementations**

* **What fails:** The iOS compilations (`iosArm64` and `iosSimulatorArm64`) fail at compile time because `commonMain` compiles to every declared target, and the compiler cannot find an `actual` implementation for the iOS targets.


* **Two ways to fix it:**
1. Provide an `actual` implementation inside `iosMain` (which both `iosArm64Main` and `iosSimulatorArm64Main` depend on and inherit from).


2. Convert the `expect` function into a regular function with a default implementation in `commonMain` (or use an interface injected per platform) if platform-specific code isn't strictly required on iOS.



---

**7. Source Set Merging & Visibility**

* **Merged source sets for `iosArm64`:** In a full default hierarchy, the compiler merges `commonMain` -> `nativeMain` -> `appleMain` -> `iosMain` -> `iosArm64Main`.


* **Why `appleMain` is visible to iOS but not `androidMain`:** Visibility follows the `dependsOn` hierarchy tree. `iosMain` (and `iosArm64Main`) has a `dependsOn` relation pointing up to `appleMain`, so it observes `appleMain`'s API. `androidMain` branches directly off `commonMain` and does not depend on `appleMain`, making `appleMain` completely invisible to Android.



---

**8. iOS System Version Function**

```kotlin
// iosMain
import platform.UIKit.UIDevice

fun getIosVersion(): String {
    return UIDevice.currentDevice.systemVersion
}

```

**Why it cannot live in `commonMain`:** `UIDevice` belongs to Apple's `UIKit` framework (`platform.UIKit.*`), which is only available to iOS targets. Because `commonMain` must compile against all targets (including Android/JVM), platform-specific iOS APIs are not visible or available there.

---

**9. Ktor `sourceSets` Configuration**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:2.3.12")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.12")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.12")
        }
    }
}

```

---

**10. KMP vs. Compose Multiplatform**

* **Difference:** Kotlin Multiplatform (KMP) is a technology for sharing business logic, data, and non-UI code across platforms, whereas Compose Multiplatform (CMP) is a declarative UI framework built on top of KMP to share the user interface itself.
* **Module layout for shared logic + native UI:** The project has a `:shared` KMP module (containing `commonMain`, `androidMain`, `iosMain`), an `:androidApp` module built with Jetpack Compose that depends on `:shared` as a regular Gradle dependency, and an `iosApp` Xcode project built with SwiftUI that consumes the compiled framework generated from `:shared`.


* **Which can be used without the other:** **KMP** can be used without Compose Multiplatform (you cannot use Compose Multiplatform across platforms without KMP underneath).

---

**11. Stability Levels & Production Readiness**

* **iOS (KMP):** **Stable.** *Ship in production?* **Yes.** It is production-ready, officially supported by JetBrains, and battle-tested by major companies.
* **Kotlin/Wasm:** **Alpha.** *Ship in production?* **No** (not for critical apps). The specification and tooling are still evolving rapidly, though it's fine for internal demos or experiments.
* **`expect`/`actual` classes:** **Beta.** *Ship in production?* **Yes, with caution** (or prefer interfaces). While stable enough to work, JetBrains recommends using interfaces or `expect`/`actual` functions where possible because `expect`/`actual` classes have stricter compiler constraints.
* **Compose Multiplatform for Web:** **Alpha** (powered by Kotlin/Wasm). *Ship in production?* **No.** Canvas-based web rendering still has accessibility, SEO, and browser-compatibility trade-offs that make it risky for consumer web apps today.

---

**12. `commonTest` Unit Test & Gradle Tasks**

```kotlin
// commonTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EmailValidatorTest {
    @Test
    fun testValidAndInvalidEmails() {
        assertTrue(isValidEmail("student@ttpu.uz"))
        assertFalse(isValidEmail("invalid-email.com"))
    }
}

```

* **JVM Gradle task:** `jvmTest` (or `testDebugUnitTest` on Android).
* **iOS Simulator Gradle task:** `iosSimulatorArm64Test`.

---

**13. Two Limitations of KMP & Mitigations**

1. **Team skills & tooling friction:** With local source distribution or a monorepo, iOS developers are forced to install and learn unfamiliar tools like Gradle, Kotlin, and Android Studio.


* *Mitigation:* Use **Remote Artifact Distribution** (publishing a versioned XCFramework via SPM or CocoaPods) or a **Hybrid Workflow** so non-participating iOS developers never have to touch Kotlin or Gradle.




2. **Swift interop & binary size (umbrella bottlenecks):** Kotlin translates to Objective-C headers under the hood (making generics, sealed classes, and coroutines awkward in Swift), and compiling multiple separate KMP modules for iOS duplicates dependencies and bloats binary size.


* *Mitigation:* Bundle all shared modules through a single **Umbrella Framework** so dependencies exist only once, and use interop tools like **SKIE** or **KMP-NativeCoroutines** to generate clean Swift APIs for Flows, suspend functions, and sealed classes.





---

**14. `LoginValidator` in `commonMain` and Platform Consumption**

```kotlin
// commonMain
class LoginValidator {
    fun isEmailValid(email: String): Boolean =
        email.contains("@") && email.contains(".")

    fun isPasswordValid(password: String): Boolean =
        password.length >= 8
}

```

```kotlin
// Android ViewModel (androidApp)
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    private val validator = LoginValidator()

    fun validate(email: String, pass: String): Boolean {
        return validator.isEmailValid(email) && validator.isPasswordValid(pass)
    }
}

```

```swift
// SwiftUI ViewModel (iosApp)
import Foundation
import Shared

class IosLoginViewModel: ObservableObject {
    private let validator = LoginValidator()

    func validate(email: String, pass: String) -> Bool {
        return validator.isEmailValid(email: email) && validator.isPasswordValid(password: pass)
    }
}

```

---

**15. Layer Migration Order for an Existing Android App**

* **Order of migration:**
1. **Data models** (DTOs / domain entities)
2. **Business rules** (validation, use cases, calculations)
3. **Networking** (API calls with Ktor, serialization)
4. **Local storage** (caching/databases with SQLDelight or Room KMP)
5. **UI** (kept native or migrated last)


**Part B — KMP Project Configuration (16–30)

**1. Single Shared Module vs. Several Shared Modules**

* **Single Shared Module:**
* *Advantage:* A simple design reduces cognitive load because there is no deciding where functionality goes, making it a great starting point.


* *Drawback:* Compilation time increases as the shared module grows, and there is no way to depend on only part of the shared code.




* **Several Shared Modules:**
* *Advantage:* Separation of concerns for shared code allows for better scalability as the app grows.


* *Drawback:* It requires a more complicated setup, including dependency management across modules and configuring an umbrella framework for iOS.




* **Signal to split:** When one shared module is no longer enough (e.g., compilation times spike) and you need to split into features so targets can depend on only parts of the codebase.



**2. `settings.gradle.kts` and Dependency Line**

* **`settings.gradle.kts`:**
```kotlin
include(":shared", ":androidApp", ":desktopApp")

```


* **Dependency line in app modules (`build.gradle.kts`):**
```kotlin
implementation(project(":shared"))

```



**3. Two Feature Modules, Same Library, Two Frameworks**

* **Inside the app:** The shared dependency gets duplicated—meaning it exists twice in the app as two completely incompatible copies.


* **Concrete failure:** State passed through "the same" dependency from two different modules won't actually be connected, leading to bugs that only manifest on iOS.



**4. Umbrella Module `build.gradle.kts**`

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":feature:auth"))
            export(project(":feature:profile"))
        }
    }
    sourceSets.commonMain.dependencies {
        api(project(":feature:auth"))
        api(project(":feature:profile"))
    }
}

```

**5. Swift Type Visibility Fix**

* **Cause:** `implementation` hides the dependency API from consumers, and the framework block does not explicitly export it.
* **Fix:** Change `implementation` to `api` and add `export` to the framework block.

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { 
            baseName = "Shared" 
            export(project(":feature:auth"))
        }
    }
    sourceSets.commonMain.dependencies {
        api(project(":feature:auth"))
    }
}

```

**6. The Necessity of the Umbrella Framework**
Each Kotlin module compiled to its own framework bundles all of its dependencies inside it. Kotlin cannot share a common dependency across separate builds because each binary is trimmed exactly to what that single build needs. An umbrella framework bundles every shared module into a single compiled artifact, ensuring every dependency exists exactly once with one consistent code structure.

**7. Repository Layouts**

* **Monorepo:**
* *Advantage:* Easy to set up with project wizards, and iOS developers don't have to hunt across repositories for shared code.


* *Drawback:* iOS developers must set up unfamiliar tools like Gradle and Android Studio/IntelliJ.


* *Versioning:* No separate versioning; the shared code moves in lockstep with the Android/iOS apps.


* **Two-Repository (Android + shared | iOS):**
* *Advantage:* Keeps the Xcode project separate while everything else stays together.


* *Drawback:* The iOS app must pull in the shared code across repositories.
* *Versioning:* Shared module version moves in lockstep with the Android app since they share a repo, while iOS is versioned separately.




* **Three-Repository (Android | iOS | shared):**
* *Advantage:* Both teams consuming the same versioned artifact creates version parity, so the shared code stops feeling "owned" by the Android team.


* *Drawback:* Highest publishing overhead; the shared modules must be published for both Android/JVM and iOS.


* *Versioning:* Shared code is versioned independently of both platforms.





**8. Three Repositories: Publishing Options**

* **Two options:** Publish each feature module separately, or publish just the umbrella module.


* **Trade-off:** Publishing features separately allows Android developers to pull in only the modules they need, but it requires more maintenance. Publishing just the umbrella module is simpler to maintain but forces the Android app to consume all shared code at once.

**9. `maven-publish` Configuration**

```kotlin
group = "uz.ttpu.shared"
version = "1.0.0"

// Android dependency consumption line:
implementation("uz.ttpu.shared:shared:1.0.0")

```

**10. XCFramework and Swift Package**

```kotlin
// build.gradle.kts
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

kotlin {
    val xcf = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            xcf.add(this)
        }
    }
}

```

```swift
// Package.swift
.binaryTarget(
    name: "Shared", 
    url: "https://your-server.com/Shared.xcframework.zip", 
    checksum: "a1b2c3d4..."
)

```

* **Why a checksum is required:** SPM requires a checksum to guarantee the downloaded `.zip` file hasn't been tampered with or corrupted during transit.

**11. Local vs. Remote Distribution**

* **iOS Developer Onboarding:** Remote ensures non-participating iOS developers never have to touch Kotlin or Gradle. Local requires all iOS developers to learn Gradle and IntelliJ/Android Studio.


* **Iteration Speed:** Local gives a faster loop with no waiting for artifacts. Remote is slower because every change requires a publish, version, and rebuild.


* **Versioning Overhead:** Local has no publishing step or separate versioning. Remote requires strictly versioning and publishing the framework.


* **Who can edit shared code:** Local makes maintaining code a shared responsibility. With remote, debugging shared Kotlin from iOS is hard, so iOS contributions taper off over time.



**12. The Hybrid Workflow**

1. **Default to remote:** Most of the time, the iOS app consumes the published, versioned framework.


2. **Switch to local:** Point the iOS build at a local dependency on the framework to iterate on shared Kotlin code directly.


3. **Verify the change:** Changes to the Kotlin code are visible on the next iOS build for immediate debugging.


4. **Publish, then switch back:** Publish the shared module *first*, then update the app to depend on the new remote version.



**13. Multiple Libraries & Umbrella Repositories**

* **Repository Graph:**
* Logging library (own repo) -> App A umbrella
* Networking library (own repo) -> App A umbrella & App B umbrella
* Analytics library (own repo) -> App B umbrella
* App A umbrella -> App A (Android + iOS)
* App B umbrella -> App B (Android + iOS)




* **Why each gets its own umbrella:** If two apps use different subsets of libraries, each app needs its own umbrella repository pulling in only the specific libraries it needs to avoid duplicate dependencies and bloat.



**14. Pick a Configuration**

* **Solo developer with a small app:** Monorepo + single shared module + local distribution. *Reason:* Easiest setup with zero publishing or versioning overhead.
* **Four engineers releasing together:** Monorepo + several shared modules (with umbrella) + local distribution. *Reason:* Keeps everyone in sync without versioning friction, but modularizes code for faster build times.
* **Separate Android and iOS teams (independent cycles):** Three-repository + remote artifact distribution. *Reason:* Provides strict versioning boundaries and keeps the iOS developer barrier to entry low.

**15. Splitting a 40k Line Shared Module Without Changing iOS API**

1. Create new feature modules (e.g., `:feature:auth`, `:feature:networking`) and migrate the relevant code out of the bloated `:shared` module.
2. Turn the original `:shared` module into a lightweight **umbrella module** by stripping its source code.
3. In the umbrella module's `build.gradle.kts`, declare `api(project(":feature:..."))` dependencies on all the new feature modules.
4. Inside the iOS `framework` block of the umbrella module, call `export(project(":feature:..."))` for each feature module.
5. Because the umbrella framework still outputs the exact same `Shared` name and exports all the underlying APIs, the iOS app can consume it without changing a single line of Swift code.

---
