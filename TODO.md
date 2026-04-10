# TODO
1. MainActivity
    - [x] Extended FAB 无法被 TalkBack 读取
    - [x] 撤销打卡和打卡的逻辑需要修改
    - [x] 暂无习惯界面需要适配横屏
    - [ ] 平板横屏最终适配摄像头
2. SettingsActivity
    - [ ] 添加用户自称设定
    - [ ] 添加彩蛋
3. AI
    - [ ] 接入 AI，让 AI 新建习惯或给出建议
4. 局域网同步
    - [ ] **阶段 1：基础架构**
        - [ ] 添加 Ktor Server 依赖
        - [ ] 实现 HTTP 服务器基础框架
        - [ ] 实现 mDNS 服务注册与发现 (`android.net.nsd`)
        - [ ] 设备 UUID 生成与存储
        - [ ] 新增 `paired_devices` 数据库表
        - [ ] 新增同步偏好设置 (UserPreferences)
    - [ ] **阶段 2：安全认证**
        - [ ] 实现配对流程 (挑战-响应)
        - [ ] 实现密码哈希存储 (PBKDF2)
        - [ ] 实现 DH 密钥交换
        - [ ] 实现 AES-256-GCM 加密传输
        - [ ] 会话令牌管理 (session_token)
    - [ ] **阶段 3：数据同步**
        - [ ] 实现增量数据获取 (基于 modifiedDate)
        - [ ] 实现冲突解决逻辑 (Last-Write-Wins)
        - [ ] 实现双向同步流程
        - [ ] 数据库事务包装
        - [ ] 同步历史记录
    - [ ] **阶段 4：UI 实现**
        - [ ] 设置页局域网同步选项
        - [ ] 设备搜索与配对 UI
        - [ ] 二维码扫描 UI
        - [ ] 同步进度与状态显示
        - [ ] 配对设备管理列表
    - [ ] **阶段 5：测试与优化**
        - [ ] 两台设备真实测试
        - [ ] 冲突场景测试
        - [ ] 性能优化
        - [ ] 错误处理与重试机制

5. 应用内 WebView 浏览器
    - [ ] **阶段 1：基础实现**
        - [ ] 创建 `WebViewActivity`
        - [ ] 实现基本布局 (Toolbar + WebView + 导航按钮)
        - [ ] 实现 URL 加载和导航
        - [ ] 实现后退/前进/刷新/关闭按钮
        - [ ] 实现加载进度条
    - [ ] **阶段 2：安全加固**
        - [ ] 实现 `SafeWebViewClient` URL 拦截
        - [ ] 实现 `SafeWebChromeClient` 弹窗限制
        - [ ] 配置 WebView 安全设置 (禁用文件访问等)
        - [ ] 添加 Cookie 清除逻辑
    - [ ] **阶段 3：UI 优化**
        - [ ] 错误页面设计 (网络错误友好提示)
        - [ ] 更多菜单 (在浏览器中打开、分享、复制链接)
        - [ ] URL 显示优化
    - [ ] **阶段 4：集成现有功能**
        - [ ] AdScreen 升级为 WebView 展示
        - [ ] SettingsActivity 链接集成
        - [ ] 便捷调用扩展函数
        - [ ] 安全与兼容性测试

> 详细规划文档:
> - 局域网同步: `doc/lan-sync-plan.md`
> - WebView 浏览器: `doc/webview-plan.md`
