package com.example.manageapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manageapplication.ui.theme.ManageApplicationTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.border
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.Image

val color405B5E = Color(0xFF405B5E) // Основной фон
val color718189 = Color(0xFF718189) // Серый фон
val color6E828C = Color(0xFF6E828C)
val colorFFF1E1 = Color(0xFFFFF1E1)
val color053740 = Color(0xFF053740)
val colorFF06373E = Color(0xFF06373E) //Таб меню
val color6E828C6B = Color(0x6B6E828C) // 42% прозрачности
val color06373E7A = Color(0x7A06373E) // 48% прозрачности
val color718189AD = Color(0xAD718189) // 68% прозрачности
val curtainPositionState = mutableStateOf(50f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManageApplicationTheme() {
                Surface(modifier = Modifier.fillMaxSize(), color = color405B5E) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    var selectedTab by remember { mutableStateOf(1) }
    var currentScreen by remember { mutableStateOf("main") } // "main", "login", "register"

    Scaffold(
        bottomBar = {
            // показываем нижнее меню только на главной и конструкторе
            if (currentScreen == "main" || currentScreen == "manage") {
                NavigationBar(
                    containerColor = colorFF06373E
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            currentScreen = "manage"
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.manage),
                                tint = Color.White,
                                contentDescription = "Управление"
                            )
                        },
                        label = { Text("Управление", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            currentScreen = "main"
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Главная",
                                tint = Color.Unspecified
                            )
                        },
                        label = { Text("Главная", color = Color.White) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            currentScreen = "login"
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.entry),
                                tint = Color.White,
                                contentDescription = "Вход"
                            )
                        },
                        label = { Text("Вход", color = Color.White) }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            "main" -> MainScreen(Modifier.padding(paddingValues))
            "login" -> LoginScreen(
                modifier = Modifier.padding(paddingValues),
                onRegisterClick = { currentScreen = "register" },
                onBackClick = { currentScreen = "main" }
            )
            "register" -> RegisterScreen(
                modifier = Modifier.padding(paddingValues),
                onLoginClick = { currentScreen = "login" },
                onBackClick = { currentScreen = "main" }
            )
            "manage" -> ManageScreen(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun BackButton(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Назад",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .clickable { onBackClick() }
                .align(Alignment.TopStart)
        )
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var curtainPosition by curtainPositionState
    var preciseValue by remember { mutableStateOf(50) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color405B5E)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "ОКНО в Россию",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Свет под вашим контролем",
            fontSize = 16.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Быстрое управление шторой
        Text(
            text = "Быстрое управление шторой",
            fontSize = 22.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Название основной шторки
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF06373E), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Гостиная (левое окно)",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ползунок
        Text(
            text = "Положение: ${curtainPosition.toInt()}%",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = curtainPosition,
            onValueChange = { curtainPosition = it },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color6E828C6B,
                inactiveTrackColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки Открыть/Закрыть
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { curtainPosition = 0f },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81CA86)),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text("Открыть", color = Color.White, fontSize = 16.sp)
            }

            Button(

                onClick = { curtainPosition = 100f },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC26767)),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("Закрыть", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Точная настройка - исправленная логика
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Точная настройка",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row {
                Button(
                    onClick = {
                        curtainPosition = (curtainPosition - 10).coerceAtLeast(0f) // Относительно текущего положения
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06373E)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("-10%", color = Color.White, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        curtainPosition = (curtainPosition + 10).coerceAtMost(100f) // Относительно текущего положения
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06373E))
                ) {
                    Text("+10%", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}


@Composable
fun ManageScreen(modifier: Modifier = Modifier) {
    var curtainPosition by curtainPositionState
    var selectedCurtain by remember { mutableStateOf("Гостиная (левое окно)") }
    var sunriseEnabled by remember { mutableStateOf(false) }
    var sunsetEnabled by remember { mutableStateOf(false) }
    var showAddSchedule by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("08:00") }
    var selectedDays by remember { mutableStateOf(emptySet<String>()) }
    var savedSchedules by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }

    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color405B5E)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Управление шторкой",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Положение шторки и выбор
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Положение шторки",
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = selectedCurtain,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFF06373E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Анимация шторки
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(color6E828C6B, RoundedCornerShape(12.dp))
        ) {
            // Свет (под шторкой)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(colorFFF1E1, RoundedCornerShape(12.dp))
            )

            // Шторка (двигается в зависимости от положения) - 100% = полностью закрыта
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp * (curtainPosition / 100f)) // 100% = полная высота
                    .background(
                        color6E828C6B,
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (curtainPosition == 100f) 0.dp else 12.dp,
                            bottomEnd = if (curtainPosition == 100f) 0.dp else 12.dp
                        )
                    )
                    .align(Alignment.TopCenter)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ползунок
        Text(
            text = "Положение: ${curtainPosition.toInt()}%",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = curtainPosition,
            onValueChange = { curtainPosition = it },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp), // Отступы чтобы не выезжал за границы
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color6E828C6B,
                inactiveTrackColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Быстрые проценты
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(0, 33, 66, 100).forEach { percent ->
                Button(
                    onClick = { curtainPosition = percent.toFloat() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (curtainPosition == percent.toFloat()) color6E828C6B else Color(0xFF06373E)
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text("$percent%", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Автоматизация по расписанию
        Text(
            text = "Автоматизация по расписанию",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Карточка "Подъем с солнцем"
        AutomationCard(
            title = "Подъем с солнцем",
            description = "Открыть штору на рассвете",
            isEnabled = sunriseEnabled,
            onToggle = { sunriseEnabled = it },
            icon = R.drawable.ic_sunrise // ← ПЕРЕДАЕМ ИКОНКУ
        )

        AutomationCard(
            title = "Опускание на закате",
            description = "Закрывать штору на закате",
            isEnabled = sunsetEnabled,
            onToggle = { sunsetEnabled = it },
            icon = R.drawable.ic_sunset // ← ПЕРЕДАЕМ ИКОНКУ
        )

        // Сохраненные сценарии с тумблерами
        savedSchedules.forEach { schedule ->
            var scheduleEnabled by remember { mutableStateOf(false) }
            SavedScheduleCard(
                schedule = schedule,
                isEnabled = scheduleEnabled,
                onToggle = { scheduleEnabled = it },
                onDelete = {
                    savedSchedules = savedSchedules.filter { it != schedule }
                }
            )
        }

        // Карточка "Добавить сценарий" - убрать стрелочку
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddSchedule = !showAddSchedule }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кружок с картинкой plusik.png и плюсом сверху
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Фон - картинка plusik.png
                        Image(
                            painter = painterResource(id = R.drawable.plusik),
                            contentDescription = "Добавить",
                            modifier = Modifier.size(40.dp)
                        )
                        // Плюс поверх картинки
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06373E) // colorFF06373E
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Добавить сценарий",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Создать новое расписание",
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Раскрывающаяся часть
                if (showAddSchedule) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Выбор времени
                        Text(
                            text = "Время",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = selectedTime,
                            onValueChange = { selectedTime = it },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Выбор дней недели
                        Text(
                            text = "Дни недели",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Дни недели в одну строку
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            daysOfWeek.forEach { day ->
                                DayChip(day, selectedDays.contains(day)) {
                                    selectedDays = if (selectedDays.contains(day)) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val newSchedule = ScheduleItem(
                                    time = selectedTime,
                                    days = selectedDays.toList(),
                                    action = "Открыть штору"
                                )
                                savedSchedules = savedSchedules + newSchedule
                                showAddSchedule = false
                                selectedTime = "08:00"
                                selectedDays = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = color6E828C6B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сохранить сценарий", color = Color.White)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

// Сценарии
        Text(
            text = "Сценарии",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

// Сценарий 1 - Утренний подъем
        ScenarioCard(
            icon = R.drawable.morning,
            title = "Доброе утро",
            slogan = "Пробуждение с плавным подъемом шторки",
            description = listOf(
                "Открыть шторку на 80%",
                "Включить чайник",
                "Включить мягкий свет",
                "Запустить утренний плейлист"
            ),
            onActivate = { curtainPosition = 80f }, // ← ДОБАВЬТЕ
            onDeactivate = { curtainPosition = 50f } // ← ДОБАВЬТЕ
        )

// Сценарий 2 - Вечерний отдых
        ScenarioCard(
            icon = R.drawable.kino,
            title = "Кинотеатр",
            slogan = "Создайте атмосферу для просмотра фильма",
            description = listOf(
                "Закрыть шторку полностью",
                "Приглушить свет до 10%",
                "Включить TV и саундбар",
                "Активировать режим 'Не беспокоить'"
            ),
            onActivate = { curtainPosition = 100f }, // ← ДОБАВЬТЕ
            onDeactivate = { curtainPosition = 50f } // ← ДОБАВЬТЕ
        )
    }
}

@Composable
fun ScenarioCard(
    icon: Int,
    title: String,
    slogan: String,
    description: List<String>,
    onActivate: () -> Unit, // ← ДОБАВЬТЕ
    onDeactivate: () -> Unit // ← ДОБАВЬТЕ
) {
    var isActive by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Верхняя часть с иконкой и названием
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = slogan,
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Описание с точками
            Column {
                description.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = item,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка активации/выключения
            if (isActive) {
                Button(
                    onClick = {
                        isActive = false
                        onDeactivate() // ← ВЫЗОВ ПРИ ВЫКЛЮЧЕНИИ
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC26767)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Выключить", color = Color.White, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = {
                        isActive = true
                        onActivate() // ← ВЫЗОВ ПРИ АКТИВАЦИИ
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = color6E828C6B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Активировать", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AutomationCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: Int? = null // ← ДОБАВИЛ ПАРАМЕТР ИКОНКИ
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // ← ДОБАВЬТЕ ЭТО
            ) {
                // Показываем иконку только если она передана
                icon?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = title,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFF9D6B0),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

// Модель данных для сценария
data class ScheduleItem(
    val time: String,
    val days: List<String>,
    val action: String
)

@Composable
fun SavedScheduleCard(
    schedule: ScheduleItem,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${schedule.action} в ${schedule.time}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Дни: ${schedule.days.joinToString(", ")}",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier
                        .scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF9D6B0),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
                // Заменяем на стандартную иконку удаления
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDelete() }
                )
            }
        }
    }
}

@Composable
fun DayChip(day: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = day,
        color = if (isSelected) Color.White else Color.LightGray,
        modifier = Modifier
            .background(
                if (isSelected) color6E828C6B else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isSelected) color6E828C6B else Color.LightGray,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun SuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("OK", color = Color.White)
            }
        },
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .wrapContentHeight()
    )
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        SuccessDialog(
            title = "Успешный вход!",
            message = "Добро пожаловать в приложение!",
            onDismiss = {
                showSuccessDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color053740)
    ) {
        BackButton(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(color718189, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Авторизация",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Электронная почта", color = Color.LightGray) }, // ← БЕЛЫЙ LABEL
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray, // обводка при фокусе
                    unfocusedIndicatorColor = Color.LightGray, // обводка без фокуса
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", color = Color.LightGray) }, // ← БЕЛЫЙ LABEL
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray, // обводка при фокусе
                    unfocusedIndicatorColor = Color.LightGray, // обводка без фокуса
                    cursorColor = Color.Black
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        showSuccessDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color053740),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Войти", color = Color.White, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Нет аккаунта? Зарегистрироваться",
                color = Color.White,
                modifier = Modifier.clickable { onRegisterClick() },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        SuccessDialog(
            title = "Успешная регистрация!",
            message = "Аккаунт для $email успешно создан.",
            onDismiss = {
                showSuccessDialog = false
                onLoginClick()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color053740)
    ) {
        BackButton(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(color718189, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Регистрация",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { newText ->
                    val filteredText = newText.filter { it.isLetter() || it.isWhitespace() }
                    name = filteredText
                },

                label = { Text("Имя", color = Color.LightGray) }, // ← БЕЛЫЙ LABEL
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray, // обводка при фокусе
                    unfocusedIndicatorColor = Color.LightGray, // обводка без фокуса
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Электронная почта", color = Color.LightGray) }, // ← БЕЛЫЙ LABEL
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray, // обводка при фокусе
                    unfocusedIndicatorColor = Color.LightGray, // обводка без фокуса
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", color = Color.LightGray) }, // ← БЕЛЫЙ LABEL
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray, // обводка при фокусе
                    unfocusedIndicatorColor = Color.LightGray, // обводка без фокуса
                    cursorColor = Color.Black
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                        showSuccessDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color053740),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Создать аккаунт", color = Color.White, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Уже есть аккаунт? Войти",
                color = Color.White,
                modifier = Modifier.clickable { onLoginClick() },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AuthApp(onBackClick: () -> Unit) {
    var showLogin by remember { mutableStateOf(true) }

    if (showLogin) {
        LoginScreen(
            onRegisterClick = { showLogin = false },
            onBackClick = onBackClick
        )
    } else {
        RegisterScreen(
            onLoginClick = { showLogin = true },
            onBackClick = onBackClick
        )
    }
}

data class Product(val name: String, val description: String, val image: Int)