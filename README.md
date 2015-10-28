Smirk
===================
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

### Demo
<p>
   <img src="https://raw.githubusercontent.com/baoboy/image.baoboy.github.io/master/2015-10/smirk_demo.gif" width="320" alt="demo.gif"/>
</p>
### Gradle
JCenter审核还未通过，想尝试的可以添加个maven的url。
```groovy
repositories {
    // ...
    maven{
        url 'https://bintray.com/artifact/download/baoyongzhang/maven'
    }
}

compile 'com.baoyz.smirk:smirk:0.1.0'
provided 'com.baoyz.smirk:smirk-compiler:0.1.0'
```
