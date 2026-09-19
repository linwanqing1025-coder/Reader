# 任务列表：阅读历史异步同步

- [x] 1. 在 `ReadingScreenViewModel.kt` 中添加 `historySyncJob` 变量
- [x] 2. 实现私有方法 `syncHistory()` 封装进度存储逻辑
- [x] 3. 实现私有方法 `startHistorySync()` 实现 1 分钟轮询
- [x] 4. 修改 `loadVolume` 启动同步任务
- [x] 5. 修改 `onCleared` 实现销毁前存盘
- [x] 6. 验证全流程同步逻辑
