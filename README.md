# How to use it

Paste the following code in your build.gradle file

```groovy
android {
    ...
    packagingOptions {
        exclude 'META-INF/NOTICE'
        exclude 'META-INF/LICENSE'
    }
}

allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
	compile 'com.github.tentello:chino-notifications:1.2'
}
```
