# 文件下载器 [![](https://jitpack.io/v/CodyyAndroid/Downloader.svg)](https://jitpack.io/#CodyyAndroid/Downloader)

## How to
**Step 1. Add the JitPack repository to your build file**

Add it in your root build.gradle at the end of repositories:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
**Step 2. Add the dependency**
```
dependencies {
            //如果项目中已有com.android.support.*包,则从Downloader中剔除;
	        compile('com.github.CodyyAndroid:Downloader:1.0.3') {
                   exclude group: 'com.android.support'
            }
            //如果项目中无com.android.support.*包,则保留;
            compile('com.github.CodyyAndroid:Downloader:1.0.3')
	}

```
## [API](https://jitpack.io/com/github/CodyyAndroid/Downloader/1.0.3/javadoc/)


