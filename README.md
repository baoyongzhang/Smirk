Smirk
===================
[ ![Travis CI](https://travis-ci.org/baoyongzhang/Smirk.svg?branch=master) ](https://travis-ci.org/baoyongzhang/Smirk) [ ![Download](https://api.bintray.com/packages/baoyongzhang/maven/Smirk/images/download.svg) ](https://bintray.com/baoyongzhang/maven/Smirk/_latestVersion)  
`Smirk`是一个Android运行时动态扩展库，其本质就是动态加载dex文件，`Smirk`进行了封装，可以很方便的完成动态dex的加载，以及调用。
## 使用方法
##### 1、定义接口
我们需要定义一套接口，因为扩展肯定是由主程序调用执行的，也就是主程序必须提前知道扩展程序有哪些方法，所以需要定义接口规范。例如：
```java
@Extension
public interface TextExtension {

    void showText(TextView textView);
}
```

这里定义了一个接口`TextExtension`，暴露了一个方法`showText(TextView textView)`，需要用`@Extension`注解修饰。

##### 2、在是适当的地方调用扩展
我们必须在主程序需要的地方调用扩展，这里是动态加载的重点。由于我们在编写主程序的时候还不知道以后想要扩展什么功能，当我们想要添加扩展功能的时候，需要将新功能编译为一个dex文件，主程序把dex下载到本地，使用`Smirk`加载即可。
```java
// 加载dex，dexPath可以是一个dex文件，也可以是一个目录
// 如果dexPath是目录，会把目录中以及子目录中所有dex文件加载
Smirk smirk = new Smirk.Builder(context)
                .addDexPath(dexPath)
                .build();
// 通过之前定义的TextExtension接口创建一个扩展对象
TextExtension textExt = smirk.create(TextExtension.class);
// 适当的地方，调用扩展
textExt.showText(textView);
```

我们通过`Smirk.create()`方法创建了一个`TextExtension`对象，这个对象相当于是一个代理对象，当调用方法的时候，例如`textExt.showText(textView)`，会依次调用dex中所有实现了`TextExtension`接口类的对象的`showText(textView)`方法。

##### 3、编写扩展功能
假设我们的App已经上线，现在想动态的增加一个功能，例如：
```java
public class TextExtension1 implements TextExtension {

    @Override
    public void showText(TextView textView) {
        textView.setText("TextExtension1 执行");
    }
}
```
我们编写了一个扩展类，实现了`TextExtension`，在`showText(TextView textView)`方法中将`textView`显示的文本设置为`TextExtension1 执行`。  
当然我们编写多个`TextExtension`，写完之后，需要把扩展类文件编译为dex文件，然后主程序下载dex文件即可。

## Demo
<p>
   <img src="https://raw.githubusercontent.com/baoboy/image.baoboy.github.io/master/2015-10/smirk_demo.gif" width="320" alt="demo.gif"/>
</p>
## Gradle
```groovy
compile 'com.baoyz.smirk:smirk:0.1.0'
provided 'com.baoyz.smirk:smirk-compiler:0.1.0'
```

## 编译dex
当编写扩展程序的时候，我们可以在`AndroidStudio`中新建一个`Android Library`，下面是我自己写的脚本，用于编译dex文件，复制到`build.gradle`中。
```groovy
task buildDex(dependsOn: build, type: Exec) {
    Properties properties = new Properties()
    properties.load(project.rootProject.file('local.properties').newDataInputStream())
    def sdkDir = properties.getProperty('sdk.dir')
    if (sdkDir == null || sdkDir.isEmpty()) {
        sdkDir = System.getenv("ANDROID_HOME")
    }
    def dx = "${sdkDir}/build-tools/${android.buildToolsVersion}/dx"
    def output = file("build/outputs/dex")
    output.mkdirs()
    commandLine dx
    args '--dex', "--output=${output}/${project.name}.dex", 'build/intermediates/bundles/release/classes.jar'
}
```
然后运行
```shell
$ ./gradlew buildDex
```
成功之后，会在`build/outputs/dex`目录中生成一个dex文件。  
(Gradle我并不熟悉，如果有更好的写法感谢指正)

## 感谢
[javapoet](https://github.com/square/javapoet)
License
=======

    The MIT License (MIT)

	Copyright (c) 2015 baoyongzhang <baoyz94@gmail.com>
	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
