# ResguarderLogger Usage Guide

## Overview

`ResguarderLogger` is a professional logging system designed for the Resguarder SDK and plugin ecosystem. It provides comprehensive logging capabilities with performance optimization and thread safety.

## Features

- **Multi-level logging**: VERBOSE, DEBUG, INFO, WARN, ERROR
- **Performance optimized**: Lazy initialization and early returns
- **Thread-safe**: Uses `AtomicBoolean` and `ConcurrentHashMap`
- **Configurable**: Customizable log levels, tags, and output
- **Memory efficient**: Tag caching and optimized string handling
- **Resource-specific methods**: Specialized logging for resource operations
- **API 21+ compatible**: Uses `getOrPut` instead of `computeIfAbsent` for better compatibility

## Basic Usage

### Initialization

```kotlin
// Initialize with default settings
ResguarderLogger.init()

// Initialize with custom configuration
ResguarderLogger.init(
    enabled = true,
    level = ResguarderLogger.Level.DEBUG,
    tag = "MyApp"
)
```

### Basic Logging

```kotlin
// Different log levels
ResguarderLogger.v("Verbose message")
ResguarderLogger.d("Debug message")
ResguarderLogger.i("Info message")
ResguarderLogger.w("Warning message")
ResguarderLogger.e("Error message")

// With custom tags
ResguarderLogger.d("Custom message", "my_tag")
ResguarderLogger.e("Error occurred", exception, "error_handler")
```

## Advanced Usage

### Performance Logging

```kotlin
val startTime = System.currentTimeMillis()
// ... perform operation ...
ResguarderLogger.logPerformance("Resource obfuscation", startTime)

// Memory usage logging
ResguarderLogger.logMemoryUsage("After resource processing")
```

### Resource-Specific Logging

```kotlin
// Log resource obfuscation
ResguarderLogger.logResourceObfuscation("ic_launcher", "res_a1b2c3")

// Log preserved resources
ResguarderLogger.logResourcePreserved("app_name", "System required")

// Log big image detection
ResguarderLogger.logBigImageDetected(R.drawable.large_image, 1024 * 1024)
```

### SDK Integration Logging

```kotlin
// SDK initialization
ResguarderLogger.logSdkInitialization("1.0.0", "default")

// Plugin integration
ResguarderLogger.logPluginIntegration("2.1.0")

// Build process
ResguarderLogger.logBuildProcess("Resource scanning", "Found 150 resources")

// Error with context
ResguarderLogger.logErrorWithContext("File not found", "Resource loading", exception)
```

## Configuration

### Runtime Configuration

```kotlin
// Enable/disable logging
ResguarderLogger.setEnabled(false)

// Set minimum log level
ResguarderLogger.setLevel(ResguarderLogger.Level.INFO)

// Set custom tag
ResguarderLogger.setTag("MyCustomTag")
```

### Log Levels

```kotlin
enum class Level(val priority: Int) {
    VERBOSE(Log.VERBOSE),  // 2
    DEBUG(Log.DEBUG),      // 3
    INFO(Log.INFO),        // 4
    WARN(Log.WARN),        // 5
    ERROR(Log.ERROR)       // 6
}
```

## Best Practices

### 1. Performance Considerations

```kotlin
// Good: Use early returns for expensive operations
if (!ResguarderLogger.isEnabled()) {
    return
}
val expensiveData = calculateExpensiveData()
ResguarderLogger.d("Data: $expensiveData")

// Good: Use string templates efficiently
ResguarderLogger.d("Processing ${resources.size} resources")
```

### 2. Error Handling

```kotlin
try {
    // ... operation ...
} catch (e: Exception) {
    ResguarderLogger.e("Operation failed", e, "operation_handler")
    // Handle error
}
```

### 3. Contextual Logging

```kotlin
// Use descriptive tags for better filtering
ResguarderLogger.d("Starting resource scan", "resource_scanner")
ResguarderLogger.d("Found drawable resource", "resource_scanner")
ResguarderLogger.d("Resource scan completed", "resource_scanner")
```

### 4. Conditional Logging

```kotlin
// Only log in debug builds
if (BuildConfig.DEBUG) {
    ResguarderLogger.d("Debug information")
}

// Or use the built-in debug mode check
if (ResguarderLogger.isDebugMode()) {
    ResguarderLogger.d("Debug information")
}
```

## Integration Examples

### In SDK Library

```kotlin
class Resguarder {
    companion object {
        private const val TAG = "Resguarder"
        
        fun initialize() {
            ResguarderLogger.logSdkInitialization("1.0.0", "default")
        }
    }
    
    fun processResources(resources: List<String>) {
        val startTime = System.currentTimeMillis()
        ResguarderLogger.d("Processing ${resources.size} resources", TAG)
        
        try {
            resources.forEach { resource ->
                ResguarderLogger.logResourceObfuscation(resource, generateNewName(resource), TAG)
            }
            ResguarderLogger.logPerformance("Resource processing", startTime, TAG)
        } catch (e: Exception) {
            ResguarderLogger.e("Resource processing failed", e, TAG)
            throw e
        }
    }
}
```

### In Gradle Plugin

```kotlin
class ResguarderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        ResguarderLogger.logPluginIntegration("2.1.0")
        
        project.afterEvaluate {
            ResguarderLogger.logBuildProcess("Plugin applied", "Project: ${project.name}")
        }
    }
}
```

### In Sample App

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize logger for the app
        ResguarderLogger.init(
            enabled = BuildConfig.DEBUG,
            level = ResguarderLogger.Level.DEBUG,
            tag = "SampleApp"
        )
        
        ResguarderLogger.i("MainActivity created")
        setContentView(R.layout.activity_main)
    }
}
```

## Log Output Format

The logger formats messages as:
```
[LEVEL] [ThreadName] Message
```

Example outputs:
```
[DEBUG] [main] Big image IDs loaded successfully
[INFO] [main] Loaded 5 big image IDs
[WARN] [AsyncTask #1] Big image detected: R.id.large_image (1024KB)
[ERROR] [main] Failed to load big image IDs
```

## Troubleshooting

### Common Issues

1. **Logs not appearing**: Check if logging is enabled and level is appropriate
2. **Performance impact**: Use early returns and avoid expensive operations in log statements
3. **Tag too long**: Tags are automatically truncated to 23 characters (Android limit)

### Debug Tips

```kotlin
// Enable verbose logging for debugging
ResguarderLogger.setLevel(ResguarderLogger.Level.VERBOSE)

// Use specific tags for filtering
ResguarderLogger.d("Debug info", "my_specific_tag")

// Log stack traces for errors
ResguarderLogger.e("Error occurred", exception, "error_tracking")
``` 