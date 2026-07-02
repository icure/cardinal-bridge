> [!NOTE]
> If you are not sure what this is about you are probably looking for https://github.com/icure/cardinal-sdk instead.

> [!CAUTION]
> Due to use of ignoreUnknownKeys this project is safe only for reading data. 
> If the need to modify data arises we should first replace the ignoreUnknownKeys with proper decrypted json patching.
> 
> Known entity that would have problems with that are:
> - `Measure`:
>   - Deprecated `min` and `max` in favor of `referenceRanges`
>   - Some encrypted measures use non-existing fields
> 
> Additional info in internal jira ticket CSM-797.

This is a Kotlin Multiplatform project targeting Server.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
