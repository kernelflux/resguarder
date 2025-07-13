# Resguarder
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Gradle Plugin](https://img.shields.io/badge/gradle-plugin-brightgreen.svg)]()

Resguarder is an Android resource obfuscation and protection plugin designed to enhance APK security and reduce the risk of resource decompilation and misuse. It supports resource renaming, reference tracking, and integrates seamlessly with existing Android projects.

## Features

- 🔒 **Resource Obfuscation**: Automatically obfuscate and rename Android resource files
- 🧩 **Plugin Architecture**: Easy integration with existing projects through Gradle plugin
- ⚙️ **Customizable Rules**: Support for custom obfuscation rules and configurations
- 🔧 **ProGuard/R8 Compatible**: Works alongside existing code obfuscation tools
- 📊 **Detailed Logging**: Comprehensive logging and error reporting
- 🚀 **Performance Optimized**: Minimal impact on build times

## Project Structure

```
resguarder/
├── build-logic/                # Build logic and conventions
├── resguarder/                 # Core library implementation
├── resguarder-plugin/          # Gradle plugin
├── sample/                     # Sample application
├── gradle/                     # Gradle configuration
├── build.gradle.kts           # Root build script
├── settings.gradle.kts        # Project settings
└── README.md                  # This file
```

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/kernelflux/resguarder.git
cd resguarder
```

### 2. Add Plugin to Your Project

In your `build.gradle.kts`:

```kotlin
plugins {
    id("com.kernelflux.plugin.resguarder") version "latest-version"
}
```

### 3. Configure Obfuscation Rules

Add configuration to your build script:

```kotlin
resguarder {
    allBitmapUseImageLoader = true
    // Add your custom rules here
}
```

### 4. Build Your Project

```bash
./gradlew assembleRelease
```

## Usage

### Basic Configuration

```kotlin
resguarder {
    allBitmapUseImageLoader = true
}
```

### Advanced Configuration

```kotlin
resguarder {
    allBitmapUseImageLoader = true
}
```

## Examples

Check out the `sample/` directory for a complete example of how to integrate and use the Resguarder plugin in an Android project.

## API Reference

### Core Classes

- `IResguarder`: Main interface for resource obfuscation
- `Resguarder`: Default implementation of the obfuscation logic
- `ResguarderUtils`: Utility functions for resource handling
- `ResguarderViewFactory`: Factory for creating obfuscated resource views

### Plugin Classes

- `ResguarderPlugin`: Main Gradle plugin implementation
- `ResguarderExtension`: Plugin configuration extension
- `GenerateResourceConstantsTask`: Task for generating resource constants

## Troubleshooting

### Common Issues

1. **Build fails with resource not found errors**
   - Ensure all resource references are properly obfuscated
   - Check that preserved resources are correctly configured

2. **Plugin not found**
   - Verify the plugin ID and version in your build script
   - Ensure the plugin is published to the correct repository

3. **Performance issues**
   - Consider excluding large resource directories
   - Use incremental builds when possible

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to all contributors who have helped improve this project
- Inspired by various Android security and obfuscation tools
- Built with modern Android development practices

## Support

- 📧 Email: your-email@example.com
- 🐛 Issues: [GitHub Issues](https://github.com/your-repo/resguarder/issues)
- 📖 Documentation: [Wiki](https://github.com/your-repo/resguarder/wiki)

---

**Note**: This plugin is designed for legitimate use cases such as protecting intellectual property and preventing resource theft. Please ensure compliance with applicable laws and regulations when using this tool.







