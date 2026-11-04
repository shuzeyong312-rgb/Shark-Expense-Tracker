# V1 图标资源映射与转换记录

- 唯一来源：`docs/prototypes/final-prototype.html`，冻结 SHA-256 `76e0836cb09817b2117ff5f5e03a5848958b2bfa7465edbdb5d8d96ed990994f`。
- 映射依据：`docs/specs/icon-mapping.md`。系统分类使用映射表中的稳定 `iconKey`，自定义选择库的 5 个分组、61 个入口及其名称和顺序保存在 `data/IconCatalog.kt`。
- 原型 `iconSvg` 的 77 个 key 均转换为本地 Android VectorDrawable；额外提取 4 个导航 SVG、2 个账伴 SVG 和分类设置 SVG，共 84 个 key、168 个 filled/outline XML，位于 `app/src/main/res/drawable/ic_cat_<key>_<variant>.xml`。中间记账加号由 Compose 绘制。
- `IconResources.kt` 显式映射 key 与本地资源，分类使用 filled 或 outline 版本；无运行时网络和外部图标库。
- `tools/convert_icons.py` 从冻结 HTML 读取路径，转换 path、rect、circle 与原型中 rotate；将黄色填充和深色线条转换为无填充和灰色线条生成 outline。唯一 SVG `<text>`（台球 8）转换为两个闭合描边路径；该字形为路径近似，需要人工核对其视觉效果。
- 用脚本核对图标表中的 27 个系统分类及 61 个选择器入口，所有对应 key 均有两种资源；Debug、Release 资源编译通过。当前未对 84 个 key 逐一进行真机像素级比对。

重生成：在项目根目录执行 `python tools/convert_icons.py`。转换器仅使用 Python 标准库。
