# LAJProject

学习用Java开发Android，一开始学了点Kotlin，现在为了速度先用相对比较熟一点的Java了。

## 集成OpenCV
1. 下载Android OpenCV，https://opencv.org/releases/
2. 解压，把SDK目录导入为module，https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off
3. OpenCV里的build.gradle SDK版本改为和app里的build.gradle SDK版本一致
4. OpenCV里的build.gradle，不用kotlin的话，把apply plugin: 'kotlin-android' 注释掉
5. Project Structure里给app增加OpenCV dependency
6. Android SDK 增加CMake和NDK
