# V1 图标映射与素材集成规范

> 基线：`docs/prototypes/final-prototype.html`（V1.6，已验收）  
> 目的：让 Android 原生实现复用原型中的图标，不另起一套视觉语言。

## 1. 图标来源原则

1. **优先直接复用最终 HTML 原型内嵌的 SVG。** 分类图标由原型 `iconSvg(name)` 定义；选择页描边版本由 `categoryIconSvg(name)` 生成。底部导航和部分操作图标则直接以内嵌 SVG 写在 HTML 中。
2. 不使用 Emoji 代替图标；不使用未经调整的 Material 默认图标替换原型图标；不混用 Phosphor、Lucide 等第三方图标库。
3. 原型图标均为项目内嵌 SVG，不需要运行时联网，也不需要下载整套外部图标包。
4. 正式 APP 将使用 Android 本地 VectorDrawable 或等效的 Compose `ImageVector`/自绘实现。所有图标必须随 APK 本地打包。
5. 分类图标需保留两种视觉状态：
   - `filled`：原型账单行、排行榜使用的黄色填充 `#FFDA43` + 深色描边 `#292929`；
   - `outline`：原型记账分类选择页、分类管理页使用的无填充 + 灰色描边 `#626262`。
6. 图标尺寸、圆形底、内边距和线宽以原型页面中的实际使用为准；不要只复用路径却改变外围容器尺寸。

## 2. 一级分类与图标 key

`iconKey` 必须保存稳定 key，不要把图标名称或 SVG 字符串直接存入账单。

| Category ID | 分类名称 | iconKey | Android 资源建议名 |
|---|---|---|---|
| `food` | 餐饮 | `food` | `ic_category_food` |
| `shop` | 购物 | `bag` | `ic_category_bag` |
| `daily` | 日用 | `daily` | `ic_category_daily` |
| `traffic` | 交通 | `bus` | `ic_category_bus` |
| `sport` | 运动 | `bike` | `ic_category_bike` |
| `fun` | 娱乐 | `mic` | `ic_category_mic` |
| `comm` | 通讯 | `phone` | `ic_category_phone` |
| `clothes` | 服饰 | `shirt` | `ic_category_shirt` |
| `house` | 住房 | `house` | `ic_category_house` |
| `medical` | 医疗 | `medical` | `ic_category_medical` |
| `study` | 学习 | `study` | `ic_category_study` |
| `pet` | 宠物 | `pet` | `ic_category_pet` |
| `gift` | 礼物 | `gift` | `ic_category_gift` |
| `office` | 办公 | `briefcase` | `ic_category_briefcase` |
| `repair` | 维修 | `wrench` | `ic_category_wrench` |
| `relatives` | 亲友 | `family` | `ic_category_family` |
| `express` | 快递 | `package` | `ic_category_package` |
| `cashgift` | 礼金（支出） | `cash` | `ic_category_cash` |
| `digital` | 数码 | `device` | `ic_category_device` |
| `social` | 社交 | `chat` | `ic_category_chat` |
| `travel` | 旅行 | `plane` | `ic_category_plane` |
| `salary` | 工资 | `wallet` | `ic_category_wallet` |
| `bonus` | 奖金 | `gift` | `ic_category_gift` |
| `parttime` | 兼职 | `briefcase` | `ic_category_briefcase` |
| `finance` | 理财 | `chart` | `ic_category_chart` |
| `giftmoney` | 礼金（收入） | `cash` | `ic_category_cash` |
| `otherincome` | 其他 | `wallet` | `ic_category_wallet` |

说明：支出“礼金”和收入“礼金”是不同 Category ID，只是复用相同图形。系统分类的 `id`、`iconKey` 必须按 Spec 初始化，不能由显示名称动态推断。

## 3. 自定义分类图标选择库

当前原型图标选择器分为 5 组。自定义分类只能从下表所列图标中选择；新建时默认选中 `gamepad`。图标库顺序和分组按原型保持。

| 分组 | 图标 key | 原型显示名称 |
|---|---|---|
| 娱乐 | `gamepad` | 游戏 |
| 娱乐 | `pingpong` | 乒乓球 |
| 娱乐 | `swimmer` | 游泳 |
| 娱乐 | `car` | 赛车 |
| 娱乐 | `ferris` | 游乐 |
| 娱乐 | `hiking` | 徒步 |
| 娱乐 | `target` | 射击 |
| 娱乐 | `cards` | 棋牌 |
| 娱乐 | `basketball` | 篮球 |
| 娱乐 | `stroller` | 休闲 |
| 娱乐 | `badminton` | 羽毛球 |
| 娱乐 | `baseball` | 棒球 |
| 娱乐 | `helmet` | 头盔 |
| 娱乐 | `billiards` | 台球 |
| 娱乐 | `sailboat` | 帆船 |
| 娱乐 | `film` | 电影 |
| 娱乐 | `ski` | 滑雪 |
| 娱乐 | `poker` | 桌游 |
| 娱乐 | `bowling` | 保龄球 |
| 饮食 | `popsicle` | 冰品 |
| 饮食 | `banana` | 水果 |
| 饮食 | `meal` | 餐食 |
| 饮食 | `apple` | 水果 |
| 饮食 | `watermelon` | 西瓜 |
| 饮食 | `noodles` | 面食 |
| 饮食 | `beer` | 啤酒 |
| 饮食 | `bottle` | 饮料 |
| 饮食 | `drumstick` | 鸡腿 |
| 饮食 | `cake` | 蛋糕 |
| 饮食 | `salad` | 沙拉 |
| 饮食 | `skewers` | 烧烤 |
| 饮食 | `coffee` | 咖啡 |
| 饮食 | `cocktail` | 饮品 |
| 饮食 | `sandwich` | 三明治 |
| 饮食 | `soup` | 汤 |
| 饮食 | `burger` | 汉堡 |
| 饮食 | `fish` | 鱼类 |
| 饮食 | `icecream` | 冰淇淋 |
| 饮食 | `takeaway` | 外卖 |
| 日用 | `daily` | 日用品 |
| 日用 | `shower` | 洗浴 |
| 日用 | `toothbrush` | 洗漱 |
| 日用 | `plant` | 绿植 |
| 日用 | `flower` | 鲜花 |
| 日用 | `lamp` | 灯具 |
| 日用 | `bed` | 寝具 |
| 日用 | `sofa` | 家具 |
| 日用 | `medical` | 医疗 |
| 日用 | `pet` | 宠物 |
| 工作 | `briefcase` | 办公 |
| 工作 | `computer` | 电脑 |
| 工作 | `calculator` | 计算器 |
| 工作 | `printer` | 打印 |
| 工作 | `wrench` | 维修 |
| 工作 | `book` | 书籍 |
| 交通出行 | `bus` | 公交 |
| 交通出行 | `car` | 汽车 |
| 交通出行 | `bike` | 骑行 |
| 交通出行 | `train` | 火车 |
| 交通出行 | `plane` | 飞机 |
| 交通出行 | `ship` | 轮船 |

原型 `iconSvg` 还包含 `flag`（仅用于 HTML 演示自定义分类样例）和 `wrench2`（未被当前分类或图标选择器使用）。正式 APP 不需要把它们作为可选项；如需兼容原型演示状态，只能作为内部兼容资源，不加入用户选择器。

## 4. 底部导航和常驻入口

以下图标不是 `iconSvg` key，而是直接内嵌在最终原型的 SVG，应从对应 HTML 节点提取路径：

| 入口 | 原型节点 | Android 资源建议名 | 说明 |
|---|---|---|---|
| 明细 | `#bottom-nav [data-page="home"] svg` | `ic_nav_ledger` | 账单列表图形 |
| 图表 | `#bottom-nav [data-page="chart"] svg` | `ic_nav_chart` | 趋势折线图形 |
| 中间记账 | `#bottom-nav .plus-btn` | Compose 绘制 | 黄色圆形按钮 + 加号，不必单独下载图标 |
| 账伴 | `#bottom-nav [data-page="agent"] svg` | `ic_nav_companion` | 对话气泡 + 星光 |
| 我的 | `#bottom-nav [data-page="mine"] svg` | `ic_nav_mine` | 用户头像圆形图形 |
| 分类设置 | `settingsCategorySvg()` | `ic_category_settings` | 常驻“设置”网格入口，不能作为可管理分类 |

月份选择器、返回、编辑、删除、关闭、排序手柄、日期选择等操作图标也以原型当前内嵌 SVG/字符为准；实现时逐页核对原型，不要从外部图标库替换成另一种线条风格。

## 5. Android 资源实现

建议统一封装：

- `CategoryIcon(iconKey, variant)`：负责系统分类、自定义分类的图标展示；`variant` 至少区分 `FILLED` 与 `OUTLINE`。
- `NavigationIcon`：负责底部导航图标。
- `ActionIcon`：负责页面操作图标。
- `CategoryIconRegistry`：集中维护 `iconKey -> VectorDrawable/ImageVector` 映射。

资源可放在：

```text
app/src/main/res/drawable/
  ic_category_<key>_filled.xml
  ic_category_<key>_outline.xml
  ic_nav_ledger.xml
  ic_nav_chart.xml
  ic_nav_companion.xml
  ic_nav_mine.xml
  ic_category_settings.xml
```

不要求机械地为 60 个选择器图标都创建两份资源；若使用一个 VectorDrawable 配合颜色参数能忠实还原两种样式，可以复用路径。但不得丢失原型中的黄色填充/灰色描边差异。

注意：`billiards` 图标的原型 SVG 使用了 `<text>` 元素显示数字 8，Android VectorDrawable 不支持 SVG `<text>`。转换时必须将该字符转为路径，或由 Compose 在图形上叠加绘制数字；不得导致资源导入失败或图标缺字。

所有资源本地打包。无需为当前原型图标下载第三方图标库，也无需增加图标运行时依赖。若开发过程中确实需要引入外部资源，必须先确认许可并在 `THIRD_PARTY_NOTICES.md` 中记录来源、版本和许可证。

## 6. 验收要求

- 27 个系统分类的 `iconKey` 映射完整、稳定，无错配或缺失。
- 5 个底部导航/记账入口与已验收原型一致。
- 自定义分类图标选择器的 5 个分组、图标顺序和名称与原型一致。
- 分类选择页、分类管理页、账单列表和图表排行榜的图标状态与原型一致。
- APK 离线运行时所有图标正常显示。
- 不出现 Emoji 图标或 Material 默认图标与原型图标混搭的情况。
- Codex 交付 `docs/implementation/icon-assets-report.md`，列出实际生成的图标资源、映射 key、资源路径和转换验证结果。
