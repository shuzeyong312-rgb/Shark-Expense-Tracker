package com.example.personalledger.data

// Ordered groups and accessible labels from docs/specs/icon-mapping.md.
val iconGroups = listOf(
    "娱乐" to listOf("gamepad" to "游戏", "pingpong" to "乒乓球", "swimmer" to "游泳", "car" to "赛车", "ferris" to "游乐", "hiking" to "徒步", "target" to "射击", "cards" to "棋牌", "basketball" to "篮球", "stroller" to "休闲", "badminton" to "羽毛球", "baseball" to "棒球", "helmet" to "头盔", "billiards" to "台球", "sailboat" to "帆船", "film" to "电影", "ski" to "滑雪", "poker" to "桌游", "bowling" to "保龄球"),
    "饮食" to listOf("popsicle" to "冰品", "banana" to "水果", "meal" to "餐食", "apple" to "水果", "watermelon" to "西瓜", "noodles" to "面食", "beer" to "啤酒", "bottle" to "饮料", "drumstick" to "鸡腿", "cake" to "蛋糕", "salad" to "沙拉", "skewers" to "烧烤", "coffee" to "咖啡", "cocktail" to "饮品", "sandwich" to "三明治", "soup" to "汤", "burger" to "汉堡", "fish" to "鱼类", "icecream" to "冰淇淋", "takeaway" to "外卖"),
    "日用" to listOf("daily" to "日用品", "shower" to "洗浴", "toothbrush" to "洗漱", "plant" to "绿植", "flower" to "鲜花", "lamp" to "灯具", "bed" to "寝具", "sofa" to "家具", "medical" to "医疗", "pet" to "宠物"),
    "工作" to listOf("briefcase" to "办公", "computer" to "电脑", "calculator" to "计算器", "printer" to "打印", "wrench" to "维修", "book" to "书籍"),
    "交通出行" to listOf("bus" to "公交", "car" to "汽车", "bike" to "骑行", "train" to "火车", "plane" to "飞机", "ship" to "轮船"),
)
val customIconKeys = iconGroups.flatMap { group -> group.second.map { it.first } }.toSet()
