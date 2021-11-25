# LAJProject

学习用Java开发Android，一开始学了点Kotlin，现在为了速度先用相对比较熟一点的Java了。

## 集成OpenCV
1. 下载Android OpenCV，https://opencv.org/releases/
2. 解压，把SDK目录导入为module，https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off
3. OpenCV里的build.gradle SDK版本改为和app里的build.gradle SDK版本一致
4. OpenCV里的build.gradle，不用kotlin的话，把apply plugin: 'kotlin-android' 注释掉
5. Project Structure里给app增加OpenCV dependency
6. Android SDK 增加CMake和NDK


## 集成讯飞语音SDK
1. 下载SDK解压
2. Msc.jar放在app/libs下，右键add as library，app的build.gradle中依赖项应该会增加implementation files('libs\\Msc.jar')
3. 把so文件带文件夹拷贝到app/src/main/jniLibs中


## 实时处理视频
1. 目前想到的比较好的方法就是用OpenGL处理
2. 但学习成本就高，先用了一个别人封装好的开源库，地址是https://github.com/pavelsemak/alpha-movie
3. 主要用于对绿幕设置Alpha通道
