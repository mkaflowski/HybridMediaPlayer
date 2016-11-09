# HybridMediaPlayer
Android music player from URL. Uses ExoPlayer and MediaPlayer for lower APIs.

## Installation

To use the library, first include it your project using Gradle

    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
        }
    }

	dependencies {
	        compile 'com.github.mkaflowski:GoogleFormHandler:v1.0.5'
	}
	

## How to use

```java
        FormHandler formHandler = FormHandler.getInstance();
        formHandler.setActionURL("https://docs.google.com/forms/d/e/1FAIpQLSckxYU7gI1B8bZzWQvGe7Vk6Lb6Uko1fF8l_ryKL52TVJUzLw/formResponse");
        formHandler.setEntries("entry.714513599", "entry.7145135955", "entry.714513599");
        formHandler.setValues("One", "Two", "Three");
        formHandler.post();
```
```

