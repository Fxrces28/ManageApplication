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
import kotlinx.coroutines.launch
import retrofit2.Converter
import okhttp3.ResponseBody
import java.lang.reflect.Type
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import androidx.compose.foundation.shape.CircleShape

val color405B5E = Color(0xFF405B5E) // Основной фон
val color718189 = Color(0xFF718189) // Серый фон
val colorFFF1E1 = Color(0xFFFFF1E1)
val color053740 = Color(0xFF053740)
val colorFF06373E = Color(0xFF06373E) //Таб меню
val color6E828C6B = Color(0x6B6E828C) // 42% прозрачности
val curtainPositionState = mutableStateOf(50f)

data class RegisterRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)


// API Service interface - измените возвращаемые типы
interface ApiService {
    @POST("api/v1/users/register")
    suspend fun register(@Body request: com.example.manageapplication.RegisterRequest): String

    @POST("api/v1/users/login")
    suspend fun login(@Body request: LoginRequest): String
}

class StringConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == String::class.java) {
            return Converter<ResponseBody, String> { responseBody ->
                responseBody.string()
            }
        }
        return null
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://xn----dtbwmdc.xn--p1ai/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: com.example.manageapplication.ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(StringConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(com.example.manageapplication.ApiService::class.java)
    }
}

// Token manager (you can store token in SharedPreferences)
object TokenManager {
    var token: String by mutableStateOf("")
}
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
    var currentScreen by remember { mutableStateOf("main") }
    var isLoggedIn by remember { mutableStateOf(TokenManager.token.isNotEmpty()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Состояние для имени пользователя
    var currentUserName by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            if (currentScreen == "main" || currentScreen == "manage" || currentScreen == "devices") {
                NavigationBar(
                    containerColor = colorFF06373E
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "manage",
                        onClick = {
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
                        selected = currentScreen == "main",
                        onClick = {
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
                        selected = currentScreen == "devices",
                        onClick = {
                            if (isLoggedIn) {
                                currentScreen = "devices" // Прямой переход на устройства
                            } else {
                                currentScreen = "login"
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = if (isLoggedIn) R.drawable.profile else R.drawable.logout),
                                contentDescription = if (isLoggedIn) "Профиль" else "Вход"
                            )
                        },
                        label = { Text(if (isLoggedIn) "Профиль" else "Вход", color = Color.White) }
                    )
                    if (isLoggedIn) {
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                showLogoutDialog = true
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.logout),
                                    tint = Color.White,
                                    contentDescription = "Выйти"
                                )
                            },
                            label = { Text("Выйти", color = Color.White) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            "main" -> MainScreen(Modifier.padding(paddingValues))
            "login" -> LoginScreen(
                modifier = Modifier.padding(paddingValues),
                onRegisterClick = { currentScreen = "register" },
                onBackClick = { currentScreen = "main" },
                onLoginSuccess = { username ->
                    isLoggedIn = true
                    currentUserName = username
                    currentScreen = "main"
                }
            )
            "register" -> RegisterScreen(
                modifier = Modifier.padding(paddingValues),
                onLoginClick = { currentScreen = "login" },
                onBackClick = { currentScreen = "main" },
                onRegisterSuccess = {
                    currentScreen = "login"
                }
            )
            "manage" -> ManageScreen(Modifier.padding(paddingValues))
            "devices" -> DevicesScreen(Modifier.padding(paddingValues))
            // Убираем экран "profile" полностью
        }
    }

    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Выход из аккаунта",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите выйти из аккаунта?",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        TokenManager.token = ""
                        isLoggedIn = false
                        currentUserName = ""
                        currentScreen = "main"
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("Выйти", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Отмена", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun DevicesScreen(modifier: Modifier = Modifier) {
    var showDevelopmentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color405B5E)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Мои устройства",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Карточка устройства
        DeviceCard(
            deviceName = "Умная штора - Гостиная",
            deviceType = "Шторка с электроприводом",
            status = "Подключено",
            isOnline = true
        )

        DeviceCard(
            deviceName = "Умная штора - Спальня",
            deviceType = "Шторка с электроприводом",
            status = "Не подключено",
            isOnline = false
        )

        Spacer(modifier = Modifier.height(32.dp))

// Кнопка добавления устройства
        Button(
            onClick = { showDevelopmentDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = color6E828C6B),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.plusik),
                    contentDescription = "Добавить устройство",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Добавить устройство",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }

    // Диалоговое окно о разработке
    if (showDevelopmentDialog) {
        AlertDialog(
            onDismissRequest = { showDevelopmentDialog = false },
            title = {
                Text(
                    text = "Функционал в разработке",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Данная функция находится в разработке и будет доступна в ближайшее время.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDevelopmentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = color053740)
                ) {
                    Text("Понятно", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun DeviceCard(
    deviceName: String,
    deviceType: String,
    status: String,
    isOnline: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deviceName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = deviceType,
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
                Text(
                    text = status,
                    fontSize = 14.sp,
                    color = if (isOnline) Color(0xFF81CA86) else Color(0xFFC26767)
                )
            }

            // Индикатор статуса
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isOnline) Color(0xFF81CA86) else Color(0xFFC26767),
                        shape = CircleShape
                    )
            )
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
                "Какой-то текст ещё"
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
                "Какой-то текст ещё",
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

fun isValidEmail(email: String): Boolean {
    val emailRegex = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )
    return emailRegex.matcher(email).matches()
}

fun isValidName(name: String): Boolean {
    val nameRegex = Pattern.compile("^[a-zA-Zа-яА-ЯёЁ\\s-]+\$")
    return nameRegex.matcher(name).matches() && name.length >= 2
}

fun isValidUsername(username: String): Boolean {
    val usernameRegex = Pattern.compile("^[a-zA-Z0-9_]+\$")
    return usernameRegex.matcher(username).matches() && username.length >= 3
}

fun isValidPhone(phone: String): Boolean {
    val phoneRegex = Pattern.compile("^[+0-9\\s-()]+\$")
    return phoneRegex.matcher(phone).matches() && phone.length >= 5
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginSuccess: (username: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color053740)
    ) {
        com.example.manageapplication.BackButton(onBackClick = onBackClick)

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

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Имя пользователя", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val response = com.example.manageapplication.RetrofitClient.instance.login(
                                    LoginRequest(username, password)
                                )

                                // Проверяем, что это не сообщение об ошибке
                                if (response.contains("Bad credentials", ignoreCase = true) ||
                                    response.contains("error", ignoreCase = true) ||
                                    response.contains("403", ignoreCase = true) ||
                                    response.contains("forbidden", ignoreCase = true) ||
                                    response.contains("unauthorized", ignoreCase = true) ||
                                    response.length < 20 // JWT токен обычно длинный (>20 символов)
                                ) {
                                    errorMessage = "Неверный логин или пароль"
                                    isLoading = false
                                    return@launch
                                }

                                com.example.manageapplication.TokenManager.token = response
                                isLoading = false
                                onLoginSuccess(username)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = when {
                                    e is java.net.ConnectException -> "Не удалось подключиться к серверу"
                                    e is java.net.SocketTimeoutException -> "Таймаут подключения"
                                    e is retrofit2.HttpException -> {
                                        when (e.code()) {
                                            403, 401 -> "Неверный логин или пароль"
                                            500 -> "Ошибка сервера (500). Попробуйте позже"
                                            400 -> "Неверные данные"
                                            else -> "Ошибка авторизации"
                                        }
                                    }
                                    e.message?.contains("Unable to resolve host") == true -> "Проблемы с интернет-соединением"
                                    else -> "Неверный логин или пароль"
                                }
                                println("Login error: ${e.message}")
                            }
                        }
                    } else {
                        errorMessage = "Заполните все поля"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color053740),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Войти", color = Color.White, fontSize = 18.sp)
                }
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

// Обновите RegisterScreen (уберите проверку существующих пользователей)
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Валидационные состояния
    var usernameError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateAll(): Boolean {
        var isValid = true

        // Валидация имени пользователя
        usernameError = when {
            username.isBlank() -> "Обязательное поле"
            !com.example.manageapplication.isValidUsername(username) -> "Только буквы, цифры и подчеркивание"
            username.length < 3 -> "Минимум 3 символа"
            else -> null
        }
        if (usernameError != null) isValid = false

        // Валидация имени
        firstNameError = when {
            firstName.isBlank() -> "Обязательное поле"
            !com.example.manageapplication.isValidName(firstName) -> "Только буквы и дефисы"
            firstName.length < 2 -> "Минимум 2 символа"
            else -> null
        }
        if (firstNameError != null) isValid = false

        // Валидация фамилии
        lastNameError = when {
            lastName.isBlank() -> "Обязательное поле"
            !com.example.manageapplication.isValidName(lastName) -> "Только буквы и дефисы"
            lastName.length < 2 -> "Минимум 2 символа"
            else -> null
        }
        if (lastNameError != null) isValid = false

        // Валидация email
        emailError = when {
            email.isBlank() -> "Обязательное поле"
            !com.example.manageapplication.isValidEmail(email) -> "Неверный формат email"
            else -> null
        }
        if (emailError != null) isValid = false

        // Валидация телефона
        phoneError = when {
            phoneNumber.isBlank() -> "Обязательное поле"
            !com.example.manageapplication.isValidPhone(phoneNumber) -> "Только цифры и символы +-()"
            phoneNumber.length < 5 -> "Минимум 5 символов"
            else -> null
        }
        if (phoneError != null) isValid = false

        // Валидация пароля
        passwordError = when {
            password.isBlank() -> "Обязательное поле"
            password.length < 6 -> "Минимум 6 символов"
            else -> null
        }
        if (passwordError != null) isValid = false

        return isValid
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color053740)
    ) {
        com.example.manageapplication.BackButton(onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
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

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Поле имени пользователя
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = null
                },
                label = { Text("Имя пользователя", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = usernameError != null,
                supportingText = {
                    usernameError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Поле имени
            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    firstNameError = null
                },
                label = { Text("Имя", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = firstNameError != null,
                supportingText = {
                    firstNameError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Поле фамилии
            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    lastNameError = null
                },
                label = { Text("Фамилия", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = lastNameError != null,
                supportingText = {
                    lastNameError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Поле email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Email", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = emailError != null,
                supportingText = {
                    emailError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Поле телефона
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    phoneError = null
                },
                label = { Text("Номер телефона", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Поле пароля
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = { Text("Пароль", color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color.Black
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = passwordError != null,
                supportingText = {
                    passwordError?.let { error ->
                        Text(text = error, color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (validateAll()) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val token = com.example.manageapplication.RetrofitClient.instance.register(
                                    RegisterRequest(
                                        username = username,
                                        firstName = firstName,
                                        lastName = lastName,
                                        email = email,
                                        phoneNumber = phoneNumber,
                                        password = password
                                    )
                                )
                                com.example.manageapplication.TokenManager.token = token
                                isLoading = false
                                onRegisterSuccess()
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = when {
                                    e is java.net.ConnectException -> "Не удалось подключиться к серверу"
                                    e is java.net.SocketTimeoutException -> "Таймаут подключения"
                                    e is retrofit2.HttpException -> {
                                        when (e.code()) {
                                            403 -> "Доступ запрещен. Проверьте данные или обратитесь к администратору"
                                            500 -> "Ошибка сервера (500). Попробуйте позже"
                                            400 -> "Неверные данные"
                                            else -> "Ошибка HTTP: ${e.code()}"
                                        }
                                    }
                                    else -> "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                                }
                                println("Registration error: ${e.stackTraceToString()}")
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color053740),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Создать аккаунт", color = Color.White, fontSize = 18.sp)
                }
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