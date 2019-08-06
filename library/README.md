# JsBridge 集成指南
-----
#### 1. Library 库依赖建议配置: 
```
dependencies {
    // ......
    compileOnly 'yingt.external.tencent:tbs:1.0.0'
    implementation 'yingt.core:jsbridge:1.0.0'
}
```

#### 2. App 壳依赖建议配置: 
```
dependencies {
    // ......
    api 'yingt.external.tencent:tbs:1.0.0'
    implementation 'yingt.core:jsbridge:1.0.0'
}
```

#### 3. 如 App 壳集成遇到 X5 冲突, 要么剔除原项目 X5 的 Jar 及 SO 库文件, 要么仅配置: 
```
dependencies {
    // ......
    implementation 'yingt.core:jsbridge:1.0.0'
}
```