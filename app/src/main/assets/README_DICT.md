# 词典导入测试说明

本目录包含用于测试词典导入功能的示例文件。

## 支持的格式

### 1. 带音标格式 (sample_dict.txt)
```
abandon [ə'bændən] vt. 放弃；遗弃
ability [ə'biliti] n. 能力；才能
```

### 2. 简单Tab格式
```
abandon	vt. 放弃；遗弃
ability	n. 能力；才能
```

### 3. JSON格式（每行一个JSON对象）
```json
{"wordRank":1,"headWord":"abandon","content":{"word":{"content":{"trans":[{"tranCn":"放弃"}]}}}}
```

### 4. 空格分隔格式
```
abandon 放弃
ability 能力
```

## 使用方法

1. 点击设置按钮 ⚙
2. 点击"导入词典"
3. 选择词典文件
4. 等待导入完成
