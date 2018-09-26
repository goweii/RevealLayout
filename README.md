# **RevealLayout**

**揭示效果布局，可以指定2个子布局，以圆形揭示效果切换选中状态**

[Demo下载](https://github.com/goweii/RevealLayout/raw/master/app/release/app-release.apk)

[GitHub主页](https://github.com/goweii/RevealLayout)

## 截图

![](https://raw.githubusercontent.com/goweii/RevealLayout/master/picture/reveal_layout_demo.gif?raw=true)



## 集成方式

### 添加依赖

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```java
allprojects {
	repositories {
		...
		maven { url 'https://www.jitpack.io' }
	}
}
```

Step 2. Add the dependency

```java
dependencies {
	implementation 'com.github.goweii:RevealLayout:v1.0.0'
}
```

### 布局文件引用

```xml
<per.goweii.reveallayout.RevealLayout
	android:layout_width="wrap_content"
	android:layout_height="wrap_content"
	android:layout_margin="5dp"
	app:rl_allow_revert="true"
	app:rl_anim_duration="1000"
	app:rl_check_with_expand="true"
	app:rl_checked="false"
	app:rl_checked_layout="@layout/reveal_layout_follow_checked"
	app:rl_uncheck_with_expand="true"
	app:rl_unchecked_layout="@layout/reveal_layout_follow_unchecked" />
```

