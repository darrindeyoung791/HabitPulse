package io.github.darrindeyoung791.habitpulse

import io.github.darrindeyoung791.habitpulse.data.model.Habit
import io.github.darrindeyoung791.habitpulse.data.model.HabitCompletion
import io.github.darrindeyoung791.habitpulse.data.model.RepeatCycle
import org.json.JSONArray

/**
 * 生成 40 个示例习惯及对应打卡记录，用于调试页「添加示例数据」。
 * 原位于旧 SettingsActivity.kt，随旧设置界面删除后迁移至此。
 */
internal fun generateSampleHabits(): Pair<List<Habit>, List<HabitCompletion>> {
    val random = kotlin.random.Random(System.currentTimeMillis())

    // Unique supervisors for testing Contacts screen (~20 total contacts)
    // 12 unique email addresses
    val uniqueEmail1 = "alice.supervisor@example.com"
    val uniqueEmail2 = "bob.coach@health.com"
    val uniqueEmail3 = "carol.mentor@fitness.com"
    val uniqueEmail4 = "david.trainer@sports.com"
    val uniqueEmail5 = "emma.guide@wellness.com"
    val uniqueEmail6 = "frank.advisor@lifestyle.com"
    val uniqueEmail7 = "grace.helper@habits.com"
    val uniqueEmail8 = "henry.support@daily.com"
    val uniqueEmail9 = "iris.partner@goals.com"
    val uniqueEmail10 = "jack.buddy@routine.com"
    val uniqueEmail11 = "kate.friend@tracker.com"
    val uniqueEmail12 = "leo.monitor@check.com"
    
    // 8 unique phone numbers
    val uniquePhone1 = "+8613800138001"
    val uniquePhone2 = "+8613900139002"
    val uniquePhone3 = "+8613700137003"
    val uniquePhone4 = "+8613600136004"
    val uniquePhone5 = "+8613500135005"
    val uniquePhone6 = "+8613400134006"
    val uniquePhone7 = "+8613300133007"
    val uniquePhone8 = "+8613200132008"

    // Frequent supervisors (appear in multiple habits for testing Bottom Sheet scrolling)
    val frequentEmail1 = "supervisor@example.com"
    val frequentEmail2 = "manager@example.com"
    val frequentPhone1 = "+8613800138000"
    val frequentPhone2 = "+8613900139000"

    // Generate ~1000 character long note for testing
    fun generateLongNote(): String {
        val baseText = "这是一段测试用的超长备注文本，用于检验 UI 在显示大量文字时的表现。"
        val repeatedText = baseText.repeat(15)
        return repeatedText + "\n\n此外，还需要注意以下几点：\n" +
                "1. 每天保持足够的饮水量\n" +
                "2. 注意饮食均衡\n" +
                "3. 保证充足的睡眠\n" +
                "4. 定期进行体育锻炼\n" +
                "5. 保持良好的心态\n" +
                "6. 避免过度使用电子设备\n" +
                "7. 注意用眼卫生\n" +
                "8. 定期体检\n" +
                "9. 养成良好的生活习惯\n" +
                "10. 坚持就是胜利！\n\n" +
                "这段文字的目的是测试界面在显示大量文本时的滚动性能和视觉效果。" +
                "请确保文字能够正常换行，不会出现溢出或截断的问题。" +
                "同时也测试卡片高度是否能够自适应内容变化。"
    }

    // Generate ~10 reminder times for testing
    fun generateManyReminders(): String {
        val times = listOf("06:00", "07:00", "08:00", "09:00", "10:00",
                          "11:00", "12:00", "13:00", "14:00", "15:00",
                          "16:00", "17:00", "18:00", "19:00", "20:00")
        return JSONArray().apply {
            times.take(random.nextInt(8, 12)).forEach { put(it) }
        }.toString()
    }

    // Helper to generate date string
    fun formatDate(daysAgo: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    // Helper to generate timestamp for a specific day (days ago)
    fun generateTimestamp(daysAgo: Int, hour: Int = 12, minute: Int = 0): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val longNote = generateLongNote()
    val habits = mutableListOf<Habit>()
    val completions = mutableListOf<HabitCompletion>()

    // Chinese habits with unique supervisors (10 habits = ~10 contacts)
    val habit1 = Habit(
        title = "每天喝水",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = longNote,


        supervisorEmails = JSONArray().apply { put(uniqueEmail1) }.toString(),
        completionCount = 45,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit1)
    // Generate completion records for the past 30 days (randomly)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) { // 70% completion rate
            completions.add(HabitCompletion(
                habitId = habit1.id,
                completedDate = generateTimestamp(i, 9 + random.nextInt(12), random.nextInt(60)),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit2 = Habit(
        title = "晨跑锻炼",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "跑步前记得热身\n跑完后要拉伸\n注意呼吸节奏\n选择合适的跑鞋\n循序渐进增加距离",


        supervisorPhones = JSONArray().apply { put(uniquePhone1) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit2)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to 0=Monday
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit2.id,
                completedDate = generateTimestamp(i, 7, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit3 = Habit(
        title = "阅读书籍",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("21:00") }.toString(),
        notes = "每天至少读 30 分钟\n记录读书笔记\n分享读书心得",


        supervisorEmails = JSONArray().apply { put(uniqueEmail2) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit3)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit3.id,
                completedDate = generateTimestamp(i, 21, random.nextInt(60)),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit4 = Habit(
        title = "冥想练习",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:00"); put("22:00") }.toString(),
        notes = longNote,


        supervisorPhones = JSONArray().apply { put(uniquePhone2) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit4)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit4.id,
                completedDate = generateTimestamp(i, 7, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit5 = Habit(
        title = "学习编程",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "完成一个小型项目\n复习基础知识\n练习算法题\n阅读技术文档\n参与开源项目",


        supervisorEmails = JSONArray().apply { put(uniqueEmail3) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit5)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit5.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit6 = Habit(
        title = "早睡早起",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:30"); put("06:30") }.toString(),


        supervisorPhones = JSONArray().apply { put(uniquePhone3) }.toString(),
        completionCount = 22,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit6)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit6.id,
                completedDate = generateTimestamp(i, 6, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit7 = Habit(
        title = "健康饮食",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = "少油少盐\n多吃蔬菜水果\n控制糖分摄入\n适量蛋白质\n多喝水",


        supervisorEmails = JSONArray().apply { put(uniqueEmail4) }.toString(),
        completionCount = 27,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit7)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit7.id,
                completedDate = generateTimestamp(i, 12, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit8 = Habit(
        title = "瑜伽拉伸",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("20:00") }.toString(),


        supervisorPhones = JSONArray().apply { put(uniquePhone4) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit8)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(0, 2, 4, 6) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit8.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit9 = Habit(
        title = "写日记",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("23:00") }.toString(),
        notes = longNote,


        supervisorEmails = JSONArray().apply { put(uniqueEmail5) }.toString(),
        completionCount = 19,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit9)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit9.id,
                completedDate = generateTimestamp(i, 23, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit10 = Habit(
        title = "戒烟",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),
        notes = "坚持就是胜利\n想想健康的重要性\n避免诱因\n寻找替代方法",


        supervisorPhones = JSONArray().apply { put(uniquePhone5) }.toString(),
        completionCount = 30,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit10)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.1f) { // 90% success rate
            completions.add(HabitCompletion(
                habitId = habit10.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // English habits with unique supervisors (10 habits = ~10 more contacts, total ~20)
    val habit11 = Habit(
        title = "Drink Water",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("09:00"); put("14:00"); put("17:00") }.toString(),
        notes = "This is a super long note for testing UI display performance. " +
                "It should contain around 1000 characters to properly test how the interface " +
                "handles large amounts of text. The note should wrap correctly and not overflow " +
                "or get cut off. Additionally, the card height should adapt to the content. " +
                "Remember to stay hydrated throughout the day. Drink at least 8 glasses of water. " +
                "Keep a water bottle with you. Set reminders if needed. Track your progress. " +
                "Make it a habit. Your body will thank you. Water is essential for health. " +
                "It helps with digestion, circulation, and temperature regulation. " +
                "Proper hydration improves energy levels and cognitive function. " +
                "Don't wait until you're thirsty. Drink regularly throughout the day. " +
                "Monitor your urine color - it should be light yellow. " +
                "Adjust intake based on activity level and weather. " +
                "This text continues to ensure we have enough characters for testing purposes. " +
                "The UI should handle this gracefully with proper scrolling and layout. " +
                "Make sure the text is readable and the card expands as needed. " +
                "Testing edge cases is important for robust application development. " +
                "This note serves that purpose effectively.",


        supervisorEmails = JSONArray().apply { put(uniqueEmail6) }.toString(),
        completionCount = 26,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit11)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit11.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit12 = Habit(
        title = "Morning Jog",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4) }.toString(),
        reminderTimes = generateManyReminders(),
        notes = "Warm up before running\nStretch after exercise\nWear proper shoes\nStart slow and increase gradually\nTrack your progress",


        supervisorPhones = JSONArray().apply { put(uniquePhone6) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit12)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit12.id,
                completedDate = generateTimestamp(i, 6, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit13 = Habit(
        title = "Read Books",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("20:00") }.toString(),
        notes = "This is another extended note for comprehensive UI testing. " +
                "Reading is an excellent habit that improves knowledge and cognitive abilities. " +
                "Set aside dedicated time each day for reading. Choose books that interest you. " +
                "Take notes while reading to retain information better. Join a book club to discuss. " +
                "Set reading goals like finishing a certain number of books per year. " +
                "Create a comfortable reading space with good lighting. " +
                "Minimize distractions while reading. Turn off electronic devices. " +
                "Read before bed to improve sleep quality. " +
                "Mix different genres to broaden your perspective. " +
                "Keep a reading journal to track your thoughts. " +
                "Share book recommendations with friends. " +
                "Visit libraries and bookstores regularly. " +
                "This extended text ensures proper testing of the UI components. " +
                "The interface should display this content clearly with appropriate formatting. " +
                "Scrolling should be smooth and the layout should be responsive. " +
                "Testing with realistic data helps identify potential issues early.",


        supervisorEmails = JSONArray().apply { put(uniqueEmail7) }.toString(),
        completionCount = 24,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit13)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit13.id,
                completedDate = generateTimestamp(i, 20, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit14 = Habit(
        title = "Meditation",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),


        supervisorPhones = JSONArray().apply { put(uniquePhone7) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit14)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit14.id,
                completedDate = generateTimestamp(i, 6, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit15 = Habit(
        title = "Learn Coding",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("18:00") }.toString(),
        notes = "Complete one small project\nReview basic concepts\nPractice algorithms\nRead documentation",


        supervisorEmails = JSONArray().apply { put(uniqueEmail8) }.toString(),
        completionCount = 16,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit15)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit15.id,
                completedDate = generateTimestamp(i, 19, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit16 = Habit(
        title = "Early Sleep",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:00"); put("06:00") }.toString(),
        notes = "Avoid screens before bed\nCreate a bedtime routine\nKeep bedroom cool and dark",


        supervisorPhones = JSONArray().apply { put(uniquePhone8) }.toString(),
        completionCount = 21,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit16)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit16.id,
                completedDate = generateTimestamp(i, 22, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit17 = Habit(
        title = "Healthy Eating",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00"); put("12:00"); put("18:00") }.toString(),
        notes = "Less oil and salt\nMore vegetables and fruits\nBalanced nutrition\nPortion control",


        supervisorEmails = JSONArray().apply { put(uniqueEmail9) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit17)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.15f) {
            completions.add(HabitCompletion(
                habitId = habit17.id,
                completedDate = generateTimestamp(i, 12, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit18 = Habit(
        title = "Yoga Stretching",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("19:00") }.toString(),


        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 14,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit18)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(0, 2, 4, 6) && random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit18.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit19 = Habit(
        title = "Write Journal",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:30") }.toString(),
        notes = "Record today's events\nReflect and improve\nSet goals for tomorrow",


        supervisorEmails = JSONArray().apply { put(uniqueEmail10) }.toString(),
        completionCount = 17,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit19)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit19.id,
                completedDate = generateTimestamp(i, 22, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit20 = Habit(
        title = "Quit Sugar",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),
        notes = "Avoid sugary drinks\nChoose healthy snacks\nRead food labels carefully",


        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit20)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit20.id,
                completedDate = generateTimestamp(i, 10, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // Additional 20 habits (21-40) with frequent supervisors for Bottom Sheet scrolling test
    // These habits use frequentEmail1, frequentEmail2, frequentPhone1, frequentPhone2
    // Plus the last 2 unique contacts (uniqueEmail11, uniqueEmail12)

    val habit21 = Habit(
        title = "每日护肤",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:00"); put("21:00") }.toString(),
        notes = "洁面→爽肤水→精华→面霜\n每周去角质 1-2 次\n注意防晒",


        supervisorEmails = JSONArray().apply { put(uniqueEmail11) }.toString(),
        completionCount = 23,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit21)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit21.id,
                completedDate = generateTimestamp(i, 21, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit22 = Habit(
        title = "记账理财",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("22:00") }.toString(),
        notes = longNote,


        supervisorEmails = JSONArray().apply { put(uniqueEmail12) }.toString(),
        completionCount = 19,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit22)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit22.id,
                completedDate = generateTimestamp(i, 22, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit23 = Habit(
        title = "练习吉他",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0); put(2); put(4) }.toString(),
        reminderTimes = JSONArray().apply { put("19:00") }.toString(),
        notes = "练习音阶\n学习和弦\n弹奏曲目\n节奏训练",


        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit23)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 3, 5) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit23.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit24 = Habit(
        title = "学习外语",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),
        notes = "背单词\n练听力\n口语对话\n阅读理解",


        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 22,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit24)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit24.id,
                completedDate = generateTimestamp(i, 18, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit25 = Habit(
        title = "整理房间",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),


        completionCount = 12,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit25)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit25.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit26 = Habit(
        title = "午休时间",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("13:00") }.toString(),
        notes = "午休 20-30 分钟\n不要超过 30 分钟\n避免影响夜间睡眠",


        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 26,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit26)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.2f) {
            completions.add(HabitCompletion(
                habitId = habit26.id,
                completedDate = generateTimestamp(i, 13, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit27 = Habit(
        title = "补充维生素",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("08:00") }.toString(),


        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 28,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit27)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.15f) {
            completions.add(HabitCompletion(
                habitId = habit27.id,
                completedDate = generateTimestamp(i, 8, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit28 = Habit(
        title = "散步放松",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("19:30") }.toString(),
        notes = "饭后散步 30 分钟\n呼吸新鲜空气\n放松身心",


        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 24,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit28)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.25f) {
            completions.add(HabitCompletion(
                habitId = habit28.id,
                completedDate = generateTimestamp(i, 19, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit29 = Habit(
        title = "练习书法",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("15:00") }.toString(),
        notes = longNote,


        supervisorEmails = JSONArray().apply { put(frequentEmail2) }.toString(),
        completionCount = 14,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit29)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 6) && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit29.id,
                completedDate = generateTimestamp(i, 15, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit30 = Habit(
        title = "深度清洁",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),


        completionCount = 11,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit30)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 6 && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit30.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    // English habits 21-30
    val habit31 = Habit(
        title = "Skincare Routine",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("07:30"); put("21:30") }.toString(),
        notes = "Cleanse → Tone → Serum → Moisturize\nExfoliate 1-2 times per week\nApply sunscreen daily",


        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 21,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit31)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.3f) {
            completions.add(HabitCompletion(
                habitId = habit31.id,
                completedDate = generateTimestamp(i, 21, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit32 = Habit(
        title = "Track Expenses",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("23:00") }.toString(),
        notes = "Record all daily expenses\nReview weekly budget\nCategorize spending\nSet savings goals",


        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 18,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit32)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit32.id,
                completedDate = generateTimestamp(i, 23, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit33 = Habit(
        title = "Guitar Practice",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(2); put(4); put(6) }.toString(),
        reminderTimes = JSONArray().apply { put("20:00") }.toString(),
        notes = "Practice scales\nLearn new chords\nPlay songs\nWork on rhythm",


        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 16,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit33)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(2, 4, 6) && random.nextFloat() > 0.4f) {
            completions.add(HabitCompletion(
                habitId = habit33.id,
                completedDate = generateTimestamp(i, 20, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit34 = Habit(
        title = "Language Learning",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = generateManyReminders(),
        notes = "Vocabulary practice\nListening exercises\nSpeaking practice\nReading comprehension",


        supervisorEmails = JSONArray().apply { put(frequentEmail2); put(frequentEmail1) }.toString(),
        completionCount = 20,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit34)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.28f) {
            completions.add(HabitCompletion(
                habitId = habit34.id,
                completedDate = generateTimestamp(i, 17, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit35 = Habit(
        title = "Tidy Up Room",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("11:00") }.toString(),


        completionCount = 13,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit35)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.35f) {
            completions.add(HabitCompletion(
                habitId = habit35.id,
                completedDate = generateTimestamp(i, 11, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit36 = Habit(
        title = "Afternoon Break",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("15:00") }.toString(),
        notes = "Take a 15-minute break\nStretch and move around\nRest your eyes from screens",


        supervisorPhones = JSONArray().apply { put(frequentPhone1) }.toString(),
        completionCount = 25,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit36)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.22f) {
            completions.add(HabitCompletion(
                habitId = habit36.id,
                completedDate = generateTimestamp(i, 15, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit37 = Habit(
        title = "Take Vitamins",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("09:00") }.toString(),


        supervisorEmails = JSONArray().apply { put(frequentEmail1) }.toString(),
        completionCount = 27,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit37)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.18f) {
            completions.add(HabitCompletion(
                habitId = habit37.id,
                completedDate = generateTimestamp(i, 9, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit38 = Habit(
        title = "Evening Walk",
        repeatCycle = RepeatCycle.DAILY,
        reminderTimes = JSONArray().apply { put("18:30") }.toString(),
        notes = "Walk for 30 minutes\nBreathe fresh air\nRelax and unwind",


        supervisorPhones = JSONArray().apply { put(frequentPhone2) }.toString(),
        completionCount = 23,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit38)
    for (i in 0 until 30) {
        if (random.nextFloat() > 0.27f) {
            completions.add(HabitCompletion(
                habitId = habit38.id,
                completedDate = generateTimestamp(i, 18, 30),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit39 = Habit(
        title = "Calligraphy Practice",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(1); put(5) }.toString(),
        reminderTimes = JSONArray().apply { put("16:00") }.toString(),
        notes = "Practice basic strokes\nCopy master works\nFocus on form and rhythm\nEnjoy the process",


        supervisorEmails = JSONArray().apply { put(frequentEmail2) }.toString(),
        completionCount = 15,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit39)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek in listOf(1, 5) && random.nextFloat() > 0.38f) {
            completions.add(HabitCompletion(
                habitId = habit39.id,
                completedDate = generateTimestamp(i, 16, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    val habit40 = Habit(
        title = "Deep Cleaning",
        repeatCycle = RepeatCycle.WEEKLY,
        repeatDays = JSONArray().apply { put(0) }.toString(),
        reminderTimes = JSONArray().apply { put("10:00") }.toString(),


        completionCount = 10,
        createdDate = generateTimestamp(30)
    )
    habits.add(habit40)
    for (i in 0 until 30) {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        if (dayOfWeek == 0 && random.nextFloat() > 0.45f) {
            completions.add(HabitCompletion(
                habitId = habit40.id,
                completedDate = generateTimestamp(i, 10, 0),
                completedDateLocal = formatDate(i)
            ))
        }
    }

    return Pair(habits, completions)
}
