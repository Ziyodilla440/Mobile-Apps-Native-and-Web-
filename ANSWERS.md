# KMP Practice 4 - Answers

## Theoretical Questions

### Question 14
Calling `dependsOn` is not the same as adding a module to the `dependencies {}` block. `dependsOn` is a Kotlin-specific feature used to connect source sets inside the same project, representing a visibility, actualization, and target inheritance relation[cite: 1]. It is not a standard Gradle packaging or build dependency[cite: 1].

When `A.dependsOn(B)` is declared, four things happen simultaneously:
1. `A` observes `B`'s API, including internal declarations[cite: 1].
2. `A` can provide `actual` implementations for the `expect` declarations in `B`[cite: 1].
3. `B` will compile to every target that `A` compiles to, plus its own[cite: 1].
4. `A` inherits all of `B`'s regular dependencies[cite: 1].

### Question 15
Both attempts are incorrect because they misuse what `dependsOn` is built for.
* **`commonTest.dependsOn(commonMain)`**: Kotlin strictly separates production (`*Main`) and test (`*Test`) source sets into separate compilation runs so test code never accidentally ends up in production[cite: 1]. Manually wiring them with `dependsOn` merges them and breaks this isolation.
* **Calling `dependsOn` on a library**: `dependsOn` is exclusively for internal source sets within the same Gradle project[cite: 1]. It cannot link to a published external library[cite: 1]. Published libraries belong in the standard `dependencies {}` block[cite: 1].

### Question 18
Propagation means that when a dependency (like `kotlinx-coroutines-core:1.7.3`) is declared in `commonMain.dependencies {}`, Kotlin automatically passes it down the tree to all source sets that depend on `commonMain`[cite: 1]. You do not need to add it separately to `androidMain` or `iosMain`[cite: 1]. 
If a developer only wants this dependency on iOS, they should declare it inside `iosMain.dependencies {}` instead[cite: 1].

### Question 19
This happens due to target compatibility. A KMP library ships its own internal source sets, and you can only use one if it covers *at least* the targets your consuming source set compiles to[cite: 1].
* `Dispatchers.Main` works because it comes from the library's `commonMain`, which covers all available Kotlin targets (including Android and iOS)[cite: 1].
* `runBlocking` works because it comes from `concurrentMain`, which covers all targets except JS (which covers Android and iOS)[cite: 1].
* `dispatcher.asExecutor()` fails because it comes from the library's JVM-specific (`jvmMain`) source set[cite: 1]. Since `jvmMain` doesn't cover iOS, it is incompatible with our project's `commonMain` and shows up as unresolved[cite: 1].

## Hands-On Observations

### Question 16
*Code implemented in the `advanced-lab` module.*

### Question 17
*Code implemented in the `template-lab` module.*
### Question 20
Because `commonMain` compiles multiple times across targets, every compilation must agree on a single version[cite: 1]. Since `androidx.navigation-compose:2.7.7` in `androidMain` requires coroutines `1.8.0+`, Gradle resolves the conflict by automatically upgrading `commonMain` from `1.7.3` to `1.8.0` and propagating `1.8.0` down to `iosMain` as well[cite: 1].
Upgrading a dependency in `iosTest` does not affect `*Main` source sets because `*Test` source sets resolve and align their dependencies on their own separate hierarchy tree, completely isolated from production code[cite: 1].

### Question 21
*Code implemented in `q18-shared-deps/build.gradle.kts`.*

## Section 4 — Choosing a KMP Project Configuration (Lecture 11)

### Question 22
* **Single shared module:** Pros: Simple setup that reduces cognitive load since you don't have to decide where code goes[cite: 2]. Cons: Compilation time grows as the module gets larger, there is no feature separation, and you can't depend on only part of the shared code[cite: 2].
* **Several shared modules:** Pros: Clean separation of concerns and better scalability as the app grows[cite: 2]. Cons: More complex setup (requires an umbrella module for iOS) and extra cross-module dependency management[cite: 2].
* **Transition point:** Teams usually start with a single shared module and split it into several feature modules when the codebase grows large enough that compilation times slow down or teams need modular boundaries[cite: 2].

### Question 23
Compiling each feature module into its own iOS framework bundles all of its dependencies inside each binary separately[cite: 2]. If two modules share a dependency and are exported as separate frameworks, that dependency is duplicated twice in the iOS app as two incompatible copies[cite: 2]. Consequently, state passed through "the same" dependency across the two modules won't actually be connected, causing iOS-only bugs[cite: 2]. An **umbrella module** fixes this by pulling all feature modules into a single compiled iOS framework artifact so every shared dependency exists only once[cite: 2].

### Question 24
* **Monorepo:** Advantage: Easy setup via wizards and iOS devs can access shared code in one place[cite: 2]. Disadvantage: iOS devs must configure Gradle/Android Studio, and it doesn't fit apps already split across repos[cite: 2].
* **Two repositories (Android+shared | iOS):** Advantage: Keeps Xcode separate so iOS and Android can be versioned independently[cite: 2]. Disadvantage: Shared code is versioned in lockstep with the Android repo, making it feel owned by the Android team[cite: 2].
* **Three repositories (Android | iOS | shared):** Advantage: All three parts are versioned independently, creating version parity so both teams consume the same versioned artifact[cite: 2]. Disadvantage: Higher setup and publishing overhead[cite: 2].
* **Key versioning change:** Moving from two to three repositories means the shared modules must now be published as versioned artifacts for Android/JVM too (either per feature module or via the umbrella module), not just for iOS[cite: 2].

### Question 25
* **Repository layout:** Use the **Many Repositories** setup where Logging, Networking, and Analytics each live in their own repository with independent versioning, published for both Android/JVM and iOS[cite: 2].
* **Consuming subsets:** Give each of the five apps its own **umbrella module/repository** that depends only on the specific libraries that app needs, bundling them into one framework for iOS[cite: 2].
* **Distribution workflow:** Use the **Hybrid Workflow**[cite: 2]. Defaulting to **remote artifact distribution** (via SPM or CocoaPods) prevents all 200 engineers (especially iOS devs not touching Kotlin) from having to set up Gradle and Android Studio[cite: 2]. Layering **local distribution** on top allows developers actively building or debugging shared features to switch to a local dependency temporarily for a fast compile-and-debug loop, before publishing the versioned artifact and switching back to remote[cite: 2].
