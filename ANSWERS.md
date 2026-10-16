# KMP Practice 4 - Answers

## Theoretical Questions

### Question 14
Calling `dependsOn` is not the same as adding a module to the `dependencies {}` block. `dependsOn` is a Kotlin-specific feature used to connect source sets inside the same project, representing a visibility, actualization, and target inheritance relation. It is not a standard Gradle packaging or build dependency.

When `A.dependsOn(B)` is declared:
1. `A` observes `B`'s API, including internal declarations.
2. `A` can provide `actual` implementations for the `expect` declarations in `B`.
3. `B` will compile to every target that `A` compiles to, plus its own.
4. `A` inherits all of `B`'s regular dependencies.

### Question 15
Both attempts are incorrect because they misuse what `dependsOn` is built for.
* **`commonTest.dependsOn(commonMain)`**: Kotlin strictly separates production (`*Main`) and test (`*Test`) source sets into separate compilation runs so test code never accidentally ends up in production. Manually wiring them merges them and breaks this isolation.
* **Calling `dependsOn` on a library**: `dependsOn` is exclusively for internal source sets within the same Gradle project. It cannot link to a published external library, which belongs in the standard `dependencies {}` block.

### Question 18
Propagation means that when a dependency (like `kotlinx-coroutines-core`) is declared in `commonMain.dependencies {}`, Kotlin automatically passes it down the tree to all source sets that depend on `commonMain`. You do not need to add it separately to `androidMain` or `iosMain`. If a developer only wants this dependency on iOS, they should declare it inside `iosMain.dependencies {}` instead.

### Question 19
This happens due to target compatibility. A KMP library ships its own internal source sets, and you can only use one if it covers *at least* the targets your consuming source set compiles to.
* `Dispatchers.Main` works because it comes from the library's `commonMain`, which covers all available Kotlin targets.
* `runBlocking` works because it comes from `concurrentMain`, which covers all targets except JS (which covers Android and iOS).
* `dispatcher.asExecutor()` fails because it comes from the library's JVM-specific (`jvmMain`) source set. Since `jvmMain` doesn't cover iOS, it is incompatible with our project's `commonMain` and shows up as unresolved.

## Hands-On Observations

### Question 16
*Code implemented in the `q16-custom-sources` module.*
Once `jvmMain` and `jsMain` were wired to `jvmAndJsMain` using `dependsOn`, the custom source set pulled the JVM and JS targets onto itself. The shared function compiles properly for JVM and JS, but remains completely isolated from the Linux target.

### Question 17
*Code implemented in the `q17-ios-hierarchy` module.*
After adding the iOS targets, the default intermediate source set (like `iosMain` or `appleMain`) did not automatically appear. This proves that writing manual `dependsOn` calls disables Kotlin's default hierarchy template, meaning the compiler stops inferring the tree and wiring intermediate source sets for you.
