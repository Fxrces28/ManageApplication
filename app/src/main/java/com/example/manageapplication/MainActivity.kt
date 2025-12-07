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
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.flow.first
import androidx.compose.material3.Divider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import java.util.Locale

val color405B5E = Color(0xFF405B5E) // Основной фон
val color718189 = Color(0xFF718189) // Серый фон
val colorFFF1E1 = Color(0xFFFFF1E1)
val color053740 = Color(0xFF053740)
val colorFF06373E = Color(0xFF06373E) //Таб меню
val color6E828C6B = Color(0x6B6E828C) // 42% прозрачности
val curtainPositionState = mutableStateOf(50f)
val color04353D = Color(0xFF04353D) // Новый цвет для кнопки
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

object CurtainPositionManager {
    private val positions = mutableMapOf<String, MutableState<Float>>()

    fun getPositionState(curtainId: String): MutableState<Float> {
        return positions.getOrPut(curtainId) { mutableStateOf(50f) }
    }

    fun updatePosition(curtainId: String, position: Float) {
        positions[curtainId]?.value = position
    }

    fun removePosition(curtainId: String) {
        positions.remove(curtainId)
    }

    fun clearAll() {
        positions.clear()
    }
}

data class Curtain(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val currentPosition: Float = 50f,
    val deviceType: String = "Умная штора",
    val isOnline: Boolean = true,
    val serialNumber: String = "SC-XXXX-XXXXXX", // ← ДОБАВИТЬ ЭТУ СТРОКУ
    val activeScenarios: List<String> = emptyList() // ← ДОБАВИТЬ ЭТУ СТРОКУ
)

val Context.dataStore by preferencesDataStore(name = "user_preferences")

// Обновляем UserPreferencesManager, чтобы сохранять предыдущие позиции
class UserPreferencesManager(private val context: Context) {
    private val dataStore = context.dataStore
    private val gson = Gson()

    companion object {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USERNAME = stringPreferencesKey("username")
        // Храним шторки по username, чтобы разделить данные пользователей
        val USER_CURTAINS_PREFIX = "user_curtains_"
        val USER_SCENARIOS_PREFIX = "user_scenarios_"
        // Храним предыдущие позиции для сценариев
        val PREVIOUS_POSITIONS_PREFIX = "prev_pos_"
    }

    private fun getPreviousPositionKey(scenarioName: String): String {
        return PREVIOUS_POSITIONS_PREFIX + scenarioName
    }


    // Удалить все готовые сценарии для шторки
    suspend fun clearExclusiveScenariosFromCurtain(curtainId: String) {
        val currentCurtains = userCurtains.first().toMutableList()
        val curtainIndex = currentCurtains.indexOfFirst { curtain -> curtain.id == curtainId }
        if (curtainIndex != -1) {
            val existingCurtain = currentCurtains[curtainIndex]
            val currentScenarios = existingCurtain.activeScenarios ?: emptyList()
            val exclusiveScenarios = listOf("Доброе утро", "Кинотеатр")
            val updatedScenarios = currentScenarios.toMutableList().apply {
                removeAll { scenario -> exclusiveScenarios.contains(scenario) }
            }
            val updatedCurtain = existingCurtain.copy(
                activeScenarios = updatedScenarios,
                serialNumber = existingCurtain.serialNumber ?: ""
            )
            currentCurtains[curtainIndex] = updatedCurtain
            saveUserCurtains(currentCurtains)
        }
    }

    private fun getUserScenariosKey(username: String?): String {
        return USER_SCENARIOS_PREFIX + (username ?: "default")
    }

    // Сохранение пользовательских сценариев
    suspend fun saveUserScenarios(scenarios: List<ScheduleItem>) {
        val currentUsername = username.first()
        // Используем this.getUserScenariosKey()
        val scenariosKey = stringPreferencesKey(this.getUserScenariosKey(currentUsername))
        dataStore.edit { preferences ->
            val scenariosJson = gson.toJson(scenarios)
            preferences[scenariosKey] = scenariosJson
        }
    }

    // Получение пользовательских сценариев
    val userScenarios: Flow<List<ScheduleItem>> = dataStore.data
        .map { preferences ->
            val currentUsername = preferences[USERNAME]
            // Используем this.getUserScenariosKey()
            val scenariosKey = stringPreferencesKey(this.getUserScenariosKey(currentUsername))
            val scenariosJson = preferences[scenariosKey]

            if (scenariosJson.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<ScheduleItem>>() {}.type
                    gson.fromJson<List<ScheduleItem>>(scenariosJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    fun savePreviousPositionForScenario(scenarioName: String, position: Float) {
        // Простое локальное хранение, можно улучшить
        val prefs = context.getSharedPreferences("scenario_positions", Context.MODE_PRIVATE)
        prefs.edit().putFloat(scenarioName, position).apply()
    }

    // Получаем предыдущее положение для сценария
    fun getPreviousPositionForScenario(scenarioName: String): Float {
        val prefs = context.getSharedPreferences("scenario_positions", Context.MODE_PRIVATE)
        return prefs.getFloat(scenarioName, 50f) // По умолчанию 50%
    }

    // Сохранение JWT токена
    suspend fun saveAuthData(token: String, username: String) {
        dataStore.edit { preferences ->
            preferences[JWT_TOKEN] = token
            preferences[USERNAME] = username
        }
    }

    // Получение JWT токена
    val jwtToken: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[JWT_TOKEN]
        }

    // Получение имени пользователя
    val username: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USERNAME]
        }

    // Выход из системы
    suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN)
            preferences.remove(USERNAME)
            // НЕ удаляем шторки при выходе, они остаются привязанными к username
        }
    }

    // Получение ключа для шторок текущего пользователя
    private suspend fun getCurrentUserCurtainsKey(): String {
        val currentUsername = username.first() ?: "default"
        return USER_CURTAINS_PREFIX + currentUsername
    }

    // Получить Flow конкретной шторки по ID
    fun getCurtainFlow(curtainId: String): Flow<Curtain?> {
        return userCurtains.map { curtains ->
            curtains.find { it.id == curtainId }?.let { curtain ->
                // Гарантируем, что все поля не null
                curtain.copy(
                    serialNumber = curtain.serialNumber ?: "",
                    activeScenarios = curtain.activeScenarios ?: emptyList()
                )
            }
        }
    }

    // Получение ключа для конкретного пользователя
    private fun getUserCurtainsKey(username: String?): String {
        return USER_CURTAINS_PREFIX + (username ?: "default")
    }

    // Сохранение списка шторок пользователя
    suspend fun saveUserCurtains(curtains: List<Curtain>) {
        val currentUsername = username.first()
        val curtainsKey = stringPreferencesKey(getUserCurtainsKey(currentUsername))
        dataStore.edit { preferences ->
            val curtainsJson = gson.toJson(curtains)
            preferences[curtainsKey] = curtainsJson
        }
    }

    val userCurtains: Flow<List<Curtain>> = dataStore.data
        .map { preferences ->
            val currentUsername = preferences[USERNAME]
            val curtainsKey = stringPreferencesKey(getUserCurtainsKey(currentUsername))
            val curtainsJson = preferences[curtainsKey]

            if (curtainsJson.isNullOrEmpty()) {
                if (currentUsername != null) {
                    emptyList()
                } else {
                    getDefaultCurtains()
                }
            } else {
                try {
                    val type = object : TypeToken<List<Curtain>>() {}.type
                    val curtains = gson.fromJson<List<Curtain>>(curtainsJson, type) ?: emptyList()
                    // Обрабатываем возможные null значения во всех полях
                    curtains.map { curtain ->
                        curtain.copy(
                            serialNumber = curtain.serialNumber ?: "",
                            activeScenarios = curtain.activeScenarios ?: emptyList()
                        )
                    }
                } catch (e: Exception) {
                    if (currentUsername != null) emptyList() else getDefaultCurtains()
                }
            }
        }

    // Добавить сценарий к шторке
    suspend fun addScenarioToCurtain(curtainId: String, scenarioName: String) {
        val currentCurtains = userCurtains.first().toMutableList()
        val curtainIndex = currentCurtains.indexOfFirst { curtain -> curtain.id == curtainId }
        if (curtainIndex != -1) {
            val existingCurtain = currentCurtains[curtainIndex]
            val currentScenarios = existingCurtain.activeScenarios ?: emptyList()
            val updatedScenarios = currentScenarios.toMutableList().apply {
                if (!contains(scenarioName)) {
                    add(scenarioName)
                }
            }
            val updatedCurtain = existingCurtain.copy(
                activeScenarios = updatedScenarios,
                serialNumber = existingCurtain.serialNumber ?: ""
            )
            currentCurtains[curtainIndex] = updatedCurtain
            saveUserCurtains(currentCurtains)
        }
    }

    // Удалить сценарий из шторки
    suspend fun removeScenarioFromCurtain(curtainId: String, scenarioName: String) {
        val currentCurtains = userCurtains.first().toMutableList()
        val curtainIndex = currentCurtains.indexOfFirst { curtain -> curtain.id == curtainId }
        if (curtainIndex != -1) {
            val existingCurtain = currentCurtains[curtainIndex]
            val currentScenarios = existingCurtain.activeScenarios ?: emptyList()
            val updatedScenarios = currentScenarios.toMutableList().apply {
                remove(scenarioName)
            }
            val updatedCurtain = existingCurtain.copy(
                activeScenarios = updatedScenarios,
                serialNumber = existingCurtain.serialNumber ?: ""
            )
            currentCurtains[curtainIndex] = updatedCurtain
            saveUserCurtains(currentCurtains)
        }
    }

    // Обновление позиции шторки
    // Функция для миграции существующих данных
    suspend fun migrateExistingCurtains() {
        val currentCurtains = userCurtains.first()
        val migratedCurtains = currentCurtains.map { curtain ->
            // Если serialNumber равен null, устанавливаем значение по умолчанию
            val fixedSerialNumber = if (curtain.serialNumber == null) "" else curtain.serialNumber
            // Если activeScenarios равен null, устанавливаем значение по умолчанию
            val fixedScenarios = if (curtain.activeScenarios == null) emptyList() else curtain.activeScenarios

            curtain.copy(
                serialNumber = fixedSerialNumber,
                activeScenarios = fixedScenarios
            )
        }

        // Сохраняем мигрированные данные только если есть изменения
        if (migratedCurtains != currentCurtains) {
            saveUserCurtains(migratedCurtains)
        }
    }

    // Обновите функцию получения списка шторок для обработки null


    // Обновите функцию обновления позиции для безопасной работы с serialNumber
    suspend fun updateCurtainPosition(curtainId: String, position: Float) {
        val currentCurtains = userCurtains.first().toMutableList()
        val curtainIndex = currentCurtains.indexOfFirst { curtain -> curtain.id == curtainId }
        if (curtainIndex != -1) {
            val existingCurtain = currentCurtains[curtainIndex]
            // Гарантируем, что все поля не будут null
            val updatedCurtain = existingCurtain.copy(
                currentPosition = position,
                serialNumber = existingCurtain.serialNumber ?: "",
                activeScenarios = existingCurtain.activeScenarios ?: emptyList() // ← ДОБАВИТЬ ЭТУ СТРОКУ
            )
            currentCurtains[curtainIndex] = updatedCurtain
            saveUserCurtains(currentCurtains)
        }
    }

    // Обновите функцию добавления новой шторки
    suspend fun addNewCurtain(curtain: Curtain) {
        val currentCurtains = userCurtains.first().toMutableList()
        // Гарантируем, что все поля не будут null
        val newCurtain = curtain.copy(
            serialNumber = curtain.serialNumber ?: "",
            activeScenarios = curtain.activeScenarios ?: emptyList() // ← ДОБАВИТЬ ЭТУ СТРОКУ
        )
        currentCurtains.add(newCurtain)
        saveUserCurtains(currentCurtains)
    }

    // Удаление шторки
    suspend fun deleteCurtain(curtainId: String) {
        val currentCurtains = userCurtains.first().toMutableList()
        currentCurtains.removeAll { curtain -> curtain.id == curtainId }
        saveUserCurtains(currentCurtains)
    }

    private fun getDefaultCurtains(): List<Curtain> {
        return listOf(
            Curtain(
                id = "default_1",
                name = "Гостевая штора",
                location = "Гостиная",
                currentPosition = 50f,
                serialNumber = "SC-1234-567890",
                activeScenarios = emptyList() // ← ЯВНО УКАЗАТЬ
            )
        )
    }
}

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
    var currentUsername: String by mutableStateOf("")
}
class MainActivity : ComponentActivity() {
    private lateinit var userPreferencesManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPreferencesManager = UserPreferencesManager(this)

        // Запускаем миграцию данных при создании активности
        lifecycleScope.launch {
            userPreferencesManager.migrateExistingCurtains()
        }

        setContent {
            ManageApplicationTheme() {
                Surface(modifier = Modifier.fillMaxSize(), color = color405B5E) {
                    MainApp(userPreferencesManager = userPreferencesManager)
                }
            }
        }
    }
}

@Composable
fun MainApp(userPreferencesManager: UserPreferencesManager) {
    var selectedTab by remember { mutableStateOf(1) }
    var currentScreen by remember { mutableStateOf("main") }
    var isLoggedIn by remember { mutableStateOf(TokenManager.token.isNotEmpty()) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedCurtainId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Загрузка сохраненных данных при запуске
    LaunchedEffect(Unit) {
        userPreferencesManager.jwtToken.collect { token ->
            token?.let {
                TokenManager.token = it
                isLoggedIn = true
            }
        }
        userPreferencesManager.username.collect { username ->
            username?.let {
                TokenManager.currentUsername = it
            }
        }
    }

    // Сбрасываем выбранную шторку при смене пользователя
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            selectedCurtainId = null
        }
    }

    // Отслеживаем изменение списка шторок при смене пользователя
    val userCurtains by userPreferencesManager.userCurtains.collectAsState(initial = emptyList())

    LaunchedEffect(userCurtains) {
        // Если текущая выбранная шторка не существует в новом списке, сбрасываем выбор
        selectedCurtainId?.let { curtainId ->
            if (userCurtains.none { it.id == curtainId }) {
                selectedCurtainId = null
            }
        }
    }

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
            "main" -> MainScreen(
                modifier = Modifier.padding(paddingValues),
                userPreferencesManager = userPreferencesManager,
                selectedCurtainId = selectedCurtainId,
                onCurtainSelected = { curtainId ->
                    selectedCurtainId = curtainId
                }
            )
            "login" -> LoginScreen(
                modifier = Modifier.padding(paddingValues),
                userPreferencesManager = userPreferencesManager,
                onRegisterClick = { currentScreen = "register" },
                onBackClick = { currentScreen = "main" },
                onLoginSuccess = { username ->
                    isLoggedIn = true
                    TokenManager.currentUsername = username
                    currentScreen = "main"
                }
            )
            "register" -> RegisterScreen(
                modifier = Modifier.padding(paddingValues),
                userPreferencesManager = userPreferencesManager,
                onLoginClick = { currentScreen = "login" },
                onBackClick = { currentScreen = "main" },
                onRegisterSuccess = {
                    currentScreen = "login"
                }
            )
            "manage" -> ManageScreen(
                modifier = Modifier.padding(paddingValues),
                userPreferencesManager = userPreferencesManager,
                selectedCurtainId = selectedCurtainId
            )
            "devices" -> DevicesScreen(
                modifier = Modifier.padding(paddingValues),
                userPreferencesManager = userPreferencesManager,
                onCurtainSelected = { curtainId ->
                    selectedCurtainId = curtainId
                    currentScreen = "main"
                }
            )
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
                        // Очистка данных
                        TokenManager.token = ""
                        TokenManager.currentUsername = ""
                        isLoggedIn = false
                        selectedCurtainId = null
                        currentScreen = "main"
                        showLogoutDialog = false

                        // Очистка локального хранилища
                        scope.launch {
                            userPreferencesManager.clearAuthData()
                        }
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
fun DevicesScreen(
    modifier: Modifier = Modifier,
    userPreferencesManager: UserPreferencesManager,
    onCurtainSelected: (String) -> Unit
) {
    var showAddCurtainDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) }
    var serialNumber by remember { mutableStateOf("") }
    var newCurtainName by remember { mutableStateOf("") }
    var newCurtainLocation by remember { mutableStateOf("") }

    val userCurtains by userPreferencesManager.userCurtains.collectAsState(initial = emptyList())
    val userScenarios by userPreferencesManager.userScenarios.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Вычисляем статистику напрямку
    val totalActiveScenarios = remember(userCurtains) {
        userCurtains.sumOf { it.activeScenarios.size }
    }

    val morningUsageCount = remember(userCurtains) {
        userCurtains.count { it.activeScenarios.contains("Доброе утро") }
    }

    val cinemaUsageCount = remember(userCurtains) {
        userCurtains.count { it.activeScenarios.contains("Кинотеатр") }
    }

    // ИСПРАВЛЕНИЕ: Используем Box с LazyColumn вместо Column с verticalScroll
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color405B5E)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Мои устройства",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                // Кнопка добавления устройства ПОД названием
                Button(
                    onClick = { showAddCurtainDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = color04353D),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "+ Добавить устройство",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            item {
                // Статистика
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = userCurtains.size.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Шторки",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalActiveScenarios.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Активных сценариев",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            if (userCurtains.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "У вас пока нет устройств",
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Нажмите кнопку \"+ Добавить устройство\" чтобы начать",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Мои устройства",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // Устройства
                items(userCurtains) { curtain ->
                    CurtainDeviceCard(
                        curtain = curtain,
                        onSelect = { onCurtainSelected(curtain.id) },
                        onDelete = {
                            scope.launch {
                                userPreferencesManager.deleteCurtain(curtain.id)
                            }
                        }
                    )
                }

                // Секция сценариев - ТОЛЬКО ЕСЛИ ЕСТЬ УСТРОЙСТВА
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "Мои сценарии",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                item {
                    // Готовые сценарии
                    ScenarioStatsCard(
                        title = "Доброе утро",
                        usageCount = morningUsageCount,
                        totalCurtains = userCurtains.size,
                        icon = R.drawable.ic_sunrise // Иконка восхода
                    )
                }

                item {
                    ScenarioStatsCard(
                        title = "Кинотеатр",
                        usageCount = cinemaUsageCount,
                        totalCurtains = userCurtains.size,
                        icon = R.drawable.kino // Иконка заката
                    )
                }

                // Пользовательские сценарии
                if (userScenarios.isNotEmpty()) {
                    items(userScenarios) { scenario ->
                        val usageCount = userCurtains.count { curtain ->
                            curtain.activeScenarios.contains(scenario.action)
                        }

                        UserScenarioStatsCard(
                            scenario = scenario,
                            usageCount = usageCount,
                            totalCurtains = userCurtains.size,
                            icon = R.drawable.ic_sunset // Иконка для пользовательских сценариев
                        )
                    }
                }

                // Если нет пользовательских сценариев - показываем подсказку
                if (userScenarios.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.plusik),
                                    contentDescription = "Добавить сценарий",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Нет пользовательских сценариев",
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Создайте свои сценарии в разделе \"Управление\"",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // ДИАЛОГ ДОБАВЛЕНИЯ УСТРОЙСТВА (без изменений)
    if (showAddCurtainDialog) {
        Dialog(
            onDismissRequest = {
                showAddCurtainDialog = false
                currentStep = 1
                serialNumber = ""
                newCurtainName = ""
                newCurtainLocation = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color718189)
                    .padding(24.dp)
            ) {
                // Кнопка назад в диалоге
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            showAddCurtainDialog = false
                            currentStep = 1
                            serialNumber = ""
                            newCurtainName = ""
                            newCurtainLocation = ""
                        }
                        .align(Alignment.TopStart)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Шаги
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Шаг 1
                        StepCircle(
                            step = 1,
                            currentStep = currentStep,
                            isCompleted = currentStep > 1
                        )

                        // Линия между шагами
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(2.dp)
                                .background(
                                    if (currentStep > 1) Color.Green else Color(0x8A04353D)
                                )
                        )

                        // Шаг 2
                        StepCircle(
                            step = 2,
                            currentStep = currentStep,
                            isCompleted = currentStep > 2
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    when (currentStep) {
                        1 -> Step1Content(
                            serialNumber = serialNumber,
                            onSerialNumberChange = { serialNumber = it },
                            onNext = {
                                if (serialNumber.length == 14) {
                                    currentStep = 2
                                }
                            }
                        )

                        2 -> Step2Content(
                            name = newCurtainName,
                            location = newCurtainLocation,
                            onNameChange = { newCurtainName = it },
                            onLocationChange = { newCurtainLocation = it },
                            onSave = {
                                if (newCurtainName.isNotBlank() && newCurtainLocation.isNotBlank()) {
                                    val newCurtain = Curtain(
                                        name = newCurtainName,
                                        location = newCurtainLocation,
                                        serialNumber = serialNumber
                                    )
                                    scope.launch {
                                        userPreferencesManager.addNewCurtain(newCurtain)
                                    }
                                    showAddCurtainDialog = false
                                    currentStep = 1
                                    serialNumber = ""
                                    newCurtainName = ""
                                    newCurtainLocation = ""
                                }
                            },
                            onBack = { currentStep = 1 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserScenarioStatsCard(
    scenario: ScheduleItem,
    usageCount: Int,
    totalCurtains: Int,
    icon: Int
) {
    val isActive = usageCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1A4A53) else Color(0xFF06373E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = scenario.action,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = scenario.action,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF81CA86) else Color.White
                    )
                    // ДОБАВЛЯЕМ ВРЕМЯ И ДНИ
                    Text(
                        text = "Время: ${scenario.time}",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Дни: ${scenario.days.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Используется на $usageCount из $totalCurtains шторок",
                        fontSize = 14.sp,
                        color = if (isActive) Color(0xFFA8D5BA) else Color.LightGray
                    )
                }
                // Индикатор использования
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isActive) Color(0xFF81CA86) else Color(0xFFC26767),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun ScenarioStatsCard(
    title: String,
    usageCount: Int,
    totalCurtains: Int,
    icon: Int
) {
    val isActive = usageCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1A4A53) else Color(0xFF06373E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF81CA86) else Color.White
                )
                Text(
                    text = "Используется на $usageCount из $totalCurtains шторок",
                    fontSize = 14.sp,
                    color = if (isActive) Color(0xFFA8D5BA) else Color.LightGray
                )
            }
            // Индикатор использования
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isActive) Color(0xFF81CA86) else Color(0xFFC26767),
                        shape = CircleShape
                    )
            )
        }
    }
}


@Composable
fun StepCircle(step: Int, currentStep: Int, isCompleted: Boolean) {
    val backgroundColor = when {
        isCompleted -> Color(0xFF81CA86) // Зеленый цвет
        currentStep == step -> colorFFF1E1
        else -> Color(0x8A04353D)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check), // Исправленная зеленая галочка
                contentDescription = "Завершено",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = step.toString(),
                color = if (currentStep == step) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun Step1Content(
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.wifi), // Исправленная иконка WiFi
            contentDescription = "WiFi",
            tint = Color.Unspecified,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Привязка устройства",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Введите серийный номер с наклейки на устройстве",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = serialNumber,
            onValueChange = {
                if (it.length <= 14 && it.all { char -> char.isLetterOrDigit() }) {
                    onSerialNumberChange(it.uppercase())
                }
            },
            label = { Text("Серийный номер") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Формат: 14 символов (буквы и цифры)",
            fontSize = 14.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x38D9D9D9), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Серийный номер находится на наклейке в нижней части устройства. Пример: A1B2C3D4E5F6G7",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            enabled = serialNumber.length == 14,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = color053740)
        ) {
            Text("Далее", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun Step2Content(
    name: String,
    location: String,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка назад в Step2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onBack() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = "Устройство найдено",
            tint = Color(0xFF81CA86),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Устройство найдено!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Дайте название вашему устройству",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Название устройства") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Комната") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Назад", color = Color.White)
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && location.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = color053740)
            ) {
                Text("Сохранить", color = Color.White)
            }
        }
    }
}

@Composable
fun CurtainDeviceCard(
    curtain: Curtain,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val hasActiveScenarios = curtain.activeScenarios.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) } // Добавляем состояние для диалога

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Название устройства и статус "Активен" В ОДНОЙ СТРОКЕ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = curtain.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Зеленая надпись "Активен" если есть сценарии
                        if (hasActiveScenarios) {
                            Text(
                                text = "Активен",
                                fontSize = 12.sp,
                                color = Color(0xFF81CA86), // Зеленый цвет
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF81CA86).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = curtain.location,
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Положение: ${curtain.currentPosition.toInt()}%",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "SN: ${curtain.serialNumber}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Онлайн статус текстом
                    Text(
                        text = if (curtain.isOnline) "Онлайн" else "Офлайн",
                        fontSize = 12.sp,
                        color = if (curtain.isOnline) Color(0xFF81CA86) else Color(0xFFC26767),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Статический ползунок для отображения текущего положения
            Slider(
                value = curtain.currentPosition,
                onValueChange = { /* Не меняем, так как это только для отображения */ },
                enabled = false,
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = color6E828C6B,
                    inactiveTrackColor = Color.Gray,
                    disabledThumbColor = Color.White,
                    disabledActiveTrackColor = color6E828C6B,
                    disabledInactiveTrackColor = Color.Gray
                )
            )

            // Подписи к ползунку
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Открыта",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Закрыта",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            // Отображение активных сценариев
            if (hasActiveScenarios) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Активные сценарии:",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    curtain.activeScenarios.forEach { scenario ->
                        Text(
                            text = "• $scenario",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // РАЗДЕЛИТЕЛЬ
            Spacer(modifier = Modifier.height(12.dp))
            Divider(
                color = color6E828C6B,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // КНОПКА УДАЛЕНИЯ В САМОМ НИЗУ КАРТОЧКИ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true } // Показываем диалог при клике
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить устройство",
                    tint = Color(0xFFC26767), // Красноватый цвет для корзины
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Удалить устройство",
                    fontSize = 14.sp,
                    color = Color(0xFFC26767),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ (по аналогии с диалогом выхода)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Удаление устройства",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите удалить устройство \"${curtain.name}\"? Это действие нельзя отменить.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete() // Вызываем функцию удаления
                        showDeleteDialog = false // Закрываем диалог
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)) // Красный цвет для опасного действия
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Отмена", color = Color.White)
                }
            }
        )
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
fun MainScreen(
    modifier: Modifier = Modifier,
    userPreferencesManager: UserPreferencesManager,
    selectedCurtainId: String?,
    onCurtainSelected: (String) -> Unit
) {
    val userCurtains by userPreferencesManager.userCurtains.collectAsState(initial = emptyList())

    // Находим выбранную шторку или используем первую по умолчанию
    val currentCurtain = remember(selectedCurtainId, userCurtains) {
        if (selectedCurtainId != null) {
            userCurtains.find { curtain -> curtain.id == selectedCurtainId }
        } else {
            userCurtains.firstOrNull()
        }
    }

    // Используем глобальное состояние для позиции
    val curtainPositionState = remember(currentCurtain?.id) {
        currentCurtain?.id?.let { CurtainPositionManager.getPositionState(it) } ?: mutableStateOf(50f)
    }
    var curtainPosition by curtainPositionState

    var showCurtainSelector by remember { mutableStateOf(false) }

    // Проверяем, есть ли активные ГОТОВЫЕ сценарии (только они блокируют)
    val hasActiveExclusiveScenario = remember(currentCurtain) {
        currentCurtain?.activeScenarios?.any {
            it == "Доброе утро" || it == "Кинотеатр"
        } == true
    }

    // Обновляем позицию при изменении текущей шторки из базы данных
    LaunchedEffect(currentCurtain) {
        currentCurtain?.let { curtain ->
            if (curtain.currentPosition != curtainPosition) {
                curtainPosition = curtain.currentPosition
            }
        }
    }

    // Сохраняем изменения позиции ТОЛЬКО если нет активных ГОТОВЫЕ сценариев
    LaunchedEffect(curtainPosition) {
        if (!hasActiveExclusiveScenario) {
            currentCurtain?.let { curtain ->
                if (curtainPosition != curtain.currentPosition) {
                    userPreferencesManager.updateCurtainPosition(curtain.id, curtainPosition)
                }
            }
        }
    }

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

        // ОСНОВНОЙ БЛОК УПРАВЛЕНИЯ С ЗАКРУГЛЕННЫМИ УГЛАМИ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color6E828C6B, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                // Выбор шторки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF06373E), RoundedCornerShape(8.dp))
                        .clickable {
                            showCurtainSelector = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentCurtain?.name ?: "Нет устройств",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                // Меню выбора шторки
                if (showCurtainSelector) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
                    ) {
                        Column {
                            userCurtains.forEach { curtain ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCurtainSelected(curtain.id)
                                            showCurtainSelector = false
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = curtain.name,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${curtain.currentPosition.toInt()}%",
                                        fontSize = 14.sp,
                                        color = Color.LightGray
                                    )
                                }
                                if (curtain != userCurtains.last()) {
                                    Divider(color = color6E828C6B, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Управление выбранной шторкой
                if (currentCurtain != null) {
                    // Ползунок - БЛОКИРУЕМ если есть активные ГОТОВЫЕ сценарии
                    Text(
                        text = "Положение: ${curtainPosition.toInt()}%",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )



                    Slider(
                        value = curtainPosition,
                        onValueChange = {
                            if (!hasActiveExclusiveScenario) {
                                curtainPosition = it
                            }
                        },
                        enabled = !hasActiveExclusiveScenario, // Блокируем если есть активные ГОТОВЫЕ сценарии
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = if (hasActiveExclusiveScenario) Color.Gray else Color.White,
                            activeTrackColor = if (hasActiveExclusiveScenario) Color.DarkGray else color6E828C6B,
                            inactiveTrackColor = Color.Gray
                        )
                    )

                    // Строка с надписями "Открыта" и "Закрыта"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Открыта",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Закрыта",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }

                    // Показываем сообщение о блокировке ТОЛЬКО для готовых сценариев
                    if (hasActiveExclusiveScenario) {
                        Text(
                            text = "Ползунок заблокирован - активен готовый сценарий: ${currentCurtain.activeScenarios.firstOrNull { it == "Доброе утро" || it == "Кинотеатр" } ?: ""}",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA500),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопки Открыть/Закрыть - ТОЖЕ БЛОКИРУЕМ ТОЛЬКО для готовых сценариев
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (!hasActiveExclusiveScenario) {
                                    curtainPosition = 0f
                                }
                            },
                            enabled = !hasActiveExclusiveScenario,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasActiveExclusiveScenario) Color.Gray else Color(0xFF81CA86)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text("Открыть",
                                color = if (hasActiveExclusiveScenario) Color.DarkGray else Color.White,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (!hasActiveExclusiveScenario) {
                                    curtainPosition = 100f
                                }
                            },
                            enabled = !hasActiveExclusiveScenario,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasActiveExclusiveScenario) Color.Gray else Color(0xFFC26767)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("Закрыть",
                                color = if (hasActiveExclusiveScenario) Color.DarkGray else Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Информация о шторке (если есть выбранная шторка)
        if (currentCurtain != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Информация об устройстве",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Местоположение:", color = Color.LightGray)
                        Text(currentCurtain.location, color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Статус:", color = Color.LightGray)
                        Text(
                            text = if (currentCurtain.isOnline) "Онлайн" else "Офлайн",
                            color = if (currentCurtain.isOnline) Color(0xFF81CA86) else Color(0xFFC26767)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Тип устройства:", color = Color.LightGray)
                        Text(currentCurtain.deviceType, color = Color.White)
                    }
                }
            }
        } else {
            // Сообщение, если нет устройств
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Нет доступных устройств",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Перейдите в раздел \"Мои устройства\" чтобы добавить шторку",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun isExclusiveScenario(scenarioName: String): Boolean {
    return scenarioName == "Доброе утро" || scenarioName == "Кинотеатр"
}

@Composable
fun SavedScheduleCard(
    schedule: ScheduleItem,
    userPreferencesManager: UserPreferencesManager,
    selectedCurtainId: String?,
    onDelete: () -> Unit
) {
    // Создаем УНИКАЛЬНЫЙ ключ для каждого сценария
    // Используем комбинацию времени, дней и действия для уникальности
    val uniqueKey = remember(schedule) {
        "${schedule.time}_${schedule.days.joinToString("_")}_${schedule.action}"
    }

    // Используем remember для конкретного сценария с уникальным ключом
    val (isActiveState, setIsActiveState) = remember(uniqueKey) {
        mutableStateOf(false)
    }

    // Используем Flow для отслеживания состояния шторки
    val currentCurtain by userPreferencesManager
        .getCurtainFlow(selectedCurtainId ?: "")
        .collectAsState(initial = null)

    val scope = rememberCoroutineScope()

    val isExclusiveScenario = remember(schedule.action) {
        // Пользовательские сценарии НЕ блокируют ползунок
        false // Все пользовательские сценарии не блокируют
    }

    // Обновляем локальное состояние при изменении текущей шторки
    LaunchedEffect(currentCurtain, uniqueKey) {
        val isActiveInDatabase = currentCurtain?.activeScenarios?.contains(schedule.action) == true
        if (isActiveState != isActiveInDatabase) {
            setIsActiveState(isActiveInDatabase)
        }
    }

    // Также обновляем состояние при изменении самого сценария
    LaunchedEffect(schedule.action) {
        val isActiveInDatabase = currentCurtain?.activeScenarios?.contains(schedule.action) == true
        if (isActiveState != isActiveInDatabase) {
            setIsActiveState(isActiveInDatabase)
        }
    }

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
                    text = schedule.action,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Время: ${schedule.time}",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Дни: ${schedule.days.joinToString(", ")}",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isActiveState,
                    onCheckedChange = { enabled ->
                        // Сначала обновляем локальное состояние для мгновенного отклика
                        setIsActiveState(enabled)

                        if (enabled) {
                            // Активируем сценарий на шторке
                            selectedCurtainId?.let { curtainId ->
                                scope.launch {
                                    userPreferencesManager.addScenarioToCurtain(curtainId, schedule.action)
                                }
                            }
                        } else {
                            // Деактивируем сценарий
                            selectedCurtainId?.let { curtainId ->
                                scope.launch {
                                    userPreferencesManager.removeScenarioFromCurtain(curtainId, schedule.action)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF81CA86),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
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
fun ManageScreen(
    modifier: Modifier = Modifier,
    userPreferencesManager: UserPreferencesManager,
    selectedCurtainId: String?
) {
    val userCurtains by userPreferencesManager.userCurtains.collectAsState(initial = emptyList())

    // Используем Flow для отслеживания выбранной шторки
    val currentCurtain by userPreferencesManager
        .getCurtainFlow(selectedCurtainId ?: "")
        .collectAsState(initial = null)

    // Используем глобальное состояние для позиции
    val curtainPositionState = remember(currentCurtain?.id) {
        currentCurtain?.id?.let { CurtainPositionManager.getPositionState(it) } ?: mutableStateOf(50f)
    }
    var curtainPosition by curtainPositionState

    var selectedCurtain by remember { mutableStateOf(currentCurtain?.name ?: "Не выбрана") }
    var showAddSchedule by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf("08:00") }
    var selectedDays by remember { mutableStateOf(emptySet<String>()) }
    var selectedAction by remember { mutableStateOf("открыть") } // Добавляем выбор действия
    val savedSchedules by userPreferencesManager.userScenarios.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val actions = listOf("открыть", "закрыть") // Возможные действия

    // Проверяем, есть ли активные сценарии
    val hasActiveScenario = remember(currentCurtain) {
        currentCurtain?.activeScenarios?.any {
            it == "Доброе утро" || it == "Кинотеатр"
        } == true
    }

    // Обновляем позицию при изменении текущей шторки
    LaunchedEffect(currentCurtain) {
        currentCurtain?.let { curtain ->
            curtainPosition = curtain.currentPosition
            selectedCurtain = curtain.name
        }
    }

    // Сохраняем изменения позиции ТОЛЬКО если нет активных сценариев
    LaunchedEffect(curtainPosition) {
        if (!hasActiveScenario) {
            currentCurtain?.let { curtain ->
                if (curtainPosition != curtain.currentPosition) {
                    userPreferencesManager.updateCurtainPosition(curtain.id, curtainPosition)
                }
            }
        }
    }

    // Восстанавливаем позицию из активного сценария при выборе шторки
    LaunchedEffect(currentCurtain?.activeScenarios) {
        currentCurtain?.let { curtain ->
            when {
                curtain.activeScenarios.contains("Доброе утро") -> curtainPosition = 20f
                curtain.activeScenarios.contains("Кинотеатр") -> curtainPosition = 100f
            }
        }
    }

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
                    .background(
                        color = if (currentCurtain != null) Color(0xFF06373E) else Color(0xFFC26767),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Показываем предупреждение, если шторка не выбрана
        if (currentCurtain == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC26767))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.wifi),
                        contentDescription = "Внимание",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Выберите шторку для управления на главном экране",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Остальной код управления (анимация, слайдер и т.д.)
        if (currentCurtain != null) {
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

            // Ползунок - БЛОКИРУЕМ если есть активные сценарии
            Text(
                text = "Положение: ${curtainPosition.toInt()}%",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = curtainPosition,
                onValueChange = {
                    if (!hasActiveScenario) {
                        curtainPosition = it
                    }
                },
                enabled = !hasActiveScenario, // Блокируем если есть активные сценарии
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                colors = SliderDefaults.colors(
                    thumbColor = if (hasActiveScenario) Color.Gray else Color.White,
                    activeTrackColor = if (hasActiveScenario) Color.DarkGray else color6E828C6B,
                    inactiveTrackColor = Color.Gray
                )
            )

            // Строка с надписями "Открыта" и "Закрыта"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Открыта",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Закрыта",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }

            // Показываем сообщение о блокировке
            if (hasActiveScenario) {
                Text(
                    text = "Ползунок заблокирован - активен сценарий: ${currentCurtain?.activeScenarios?.firstOrNull() ?: ""}",
                    fontSize = 12.sp,
                    color = Color(0xFFFFA500),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Быстрые проценты - ТОЖЕ БЛОКИРУЕМ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(0, 33, 66, 100).forEach { percent ->
                    Button(
                        onClick = {
                            if (!hasActiveScenario) {
                                curtainPosition = percent.toFloat()
                            }
                        },
                        enabled = !hasActiveScenario,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasActiveScenario) Color.Gray else
                                (if (curtainPosition == percent.toFloat()) color6E828C6B else Color(0xFF06373E))
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Text("$percent%",
                            color = if (hasActiveScenario) Color.DarkGray else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки Открыть/Закрыть - ТОЖЕ БЛОКИРУЕМ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (!hasActiveScenario) {
                            curtainPosition = 0f
                        }
                    },
                    enabled = !hasActiveScenario,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasActiveScenario) Color.Gray else Color(0xFF81CA86)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Открыть",
                        color = if (hasActiveScenario) Color.DarkGray else Color.White,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = {
                        if (!hasActiveScenario) {
                            curtainPosition = 100f
                        }
                    },
                    enabled = !hasActiveScenario,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasActiveScenario) Color.Gray else Color(0xFFC26767)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Закрыть",
                        color = if (hasActiveScenario) Color.DarkGray else Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Точная настройка - ТОЖЕ БЛОКИРУЕМ
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
                            if (!hasActiveScenario) {
                                curtainPosition = (curtainPosition - 10).coerceAtLeast(0f)
                            }
                        },
                        enabled = !hasActiveScenario,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasActiveScenario) Color.Gray else Color(0xFF06373E)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("-10%",
                            color = if (hasActiveScenario) Color.DarkGray else Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (!hasActiveScenario) {
                                curtainPosition = (curtainPosition + 10).coerceAtMost(100f)
                            }
                        },
                        enabled = !hasActiveScenario,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasActiveScenario) Color.Gray else Color(0xFF06373E)
                        )
                    ) {
                        Text("+10%",
                            color = if (hasActiveScenario) Color.DarkGray else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Информация о шторке
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06373E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Информация об устройстве",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    currentCurtain?.let { curtain ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Местоположение:", color = Color.LightGray)
                            Text(curtain.location, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Статус:", color = Color.LightGray)
                            Text(
                                text = if (curtain.isOnline) "Онлайн" else "Офлайн",
                                color = if (curtain.isOnline) Color(0xFF81CA86) else Color(0xFFC26767)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Тип устройства:", color = Color.LightGray)
                            Text(curtain.deviceType, color = Color.White)
                        }
                        // Отображение активных сценариев
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Активные сценарии:", color = Color.LightGray)
                            Text(
                                text = if (curtain.activeScenarios.isNotEmpty()) {
                                    curtain.activeScenarios.joinToString(", ")
                                } else {
                                    "Нет"
                                },
                                color = if (curtain.activeScenarios.isNotEmpty()) Color(0xFF81CA86) else Color.White
                            )
                        }
                        // ИСПРАВЛЕНИЕ: Показываем статус блокировки ТОЛЬКО для готовых сценариев
                        val hasExclusiveScenario = curtain.activeScenarios.any { scenario ->
                            scenario == "Доброе утро" || scenario == "Кинотеатр"
                        }

                        if (hasExclusiveScenario) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Управление заблокировано:", color = Color.LightGray)
                                Text(
                                    text = "Да",
                                    color = Color(0xFFFFA500)
                                )
                            }
                        }
                    } ?: run {
                        Text(
                            text = "Шторка не выбрана",
                            color = Color.LightGray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Сохраненные сценарии с тумблерами
        if (savedSchedules.isNotEmpty()) {
            Text(
                text = "Пользовательские сценарии",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            savedSchedules.forEach { schedule ->
                SavedScheduleCard(
                    schedule = schedule,
                    userPreferencesManager = userPreferencesManager,
                    selectedCurtainId = selectedCurtainId,
                    onDelete = {
                        scope.launch {
                            val updatedSchedules = savedSchedules.filter { it != schedule }
                            userPreferencesManager.saveUserScenarios(updatedSchedules)
                            // Также удаляем сценарий из активных, если он был активен
                            selectedCurtainId?.let { curtainId ->
                                userPreferencesManager.removeScenarioFromCurtain(curtainId, schedule.action)
                            }
                        }
                    }
                )
            }
        }

        // Карточка "Добавить сценарий"
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
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plusik),
                            contentDescription = "Добавить",
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF06373E)
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

                if (showAddSchedule) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Выбор действия (открыть/закрыть)
                        Text(
                            text = "Действие",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            actions.forEach { action ->
                                ActionChip(
                                    action = action,
                                    isSelected = selectedAction == action,
                                    onClick = { selectedAction = action }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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

                        Text(
                            text = "Дни недели",
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

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
                                if (selectedDays.isNotEmpty()) {
                                    // Генерируем УНИКАЛЬНОЕ имя сценария с учетом дней недели
                                    val timeFormatted = selectedTime.replace(":", ".")
                                    val daysAbbr = when {
                                        selectedDays.size == 7 -> "ежедневно"
                                        selectedDays.size == 2 && selectedDays.contains("Сб") && selectedDays.contains("Вс") -> "выходные"
                                        selectedDays.size == 5 && selectedDays.containsAll(listOf("Пн", "Вт", "Ср", "Чт", "Пт")) -> "будни"
                                        else -> selectedDays.sortedBy {
                                            daysOfWeek.indexOf(it)
                                        }.joinToString(",")
                                    }

                                    // Капитализуем первую букву действия
                                    val actionCapitalized = selectedAction.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    }

                                    val scenarioName = "$actionCapitalized в $timeFormatted ($daysAbbr)"

                                    val newSchedule = ScheduleItem(
                                        time = selectedTime,
                                        days = selectedDays.toList(),
                                        action = scenarioName // Уникальное имя с днями
                                    )
                                    val updatedSchedules = savedSchedules + newSchedule
                                    scope.launch {
                                        userPreferencesManager.saveUserScenarios(updatedSchedules)
                                    }
                                    showAddSchedule = false
                                    selectedTime = "08:00"
                                    selectedDays = emptySet()
                                    selectedAction = "открыть"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = color6E828C6B),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedDays.isNotEmpty() // Разрешаем только если выбраны дни
                        ) {
                            Text("Сохранить сценарий", color = Color.White)
                        }

                        // Подсказка, если не выбраны дни
                        if (selectedDays.isEmpty()) {
                            Text(
                                text = "Выберите хотя бы один день недели",
                                color = Color(0xFFFFA500),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Сценарии
        Text(
            text = "Готовые сценарии",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Сценарий 1 - Утренний подъем
        ScenarioCard(
            icon = R.drawable.ic_sunrise, // Иконка восхода солнца
            title = "Доброе утро",
            slogan = "Пробуждение с плавным подъемом шторки",
            description = listOf(
                "Открыть шторку на 80%",
                "Плавное пробуждение с естественным светом"
            ),
            userPreferencesManager = userPreferencesManager,
            selectedCurtainId = selectedCurtainId,
            isExclusive = true
        )

// Сценарий 2 - Вечерний отдых
        ScenarioCard(
            icon = R.drawable.kino, // Иконка заката солнца
            title = "Кинотеатр",
            slogan = "Создайте атмосферу для просмотра фильма",
            description = listOf(
                "Закрыть шторку полностью",
                "Идеальное затемнение для домашнего кинотеатра"
            ),
            userPreferencesManager = userPreferencesManager,
            selectedCurtainId = selectedCurtainId,
            isExclusive = true
        )
    }
}

@Composable
fun ActionChip(action: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = action.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        },
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun ScenarioCard(
    icon: Int,
    title: String,
    slogan: String,
    description: List<String>,
    userPreferencesManager: UserPreferencesManager,
    selectedCurtainId: String?,
    isExclusive: Boolean = false
) {
    // Используем Flow для отслеживания изменений шторки в реальном времени
    val currentCurtain by userPreferencesManager
        .getCurtainFlow(selectedCurtainId ?: "")
        .collectAsState(initial = null)

    // Используем локальное состояние для отслеживания активности
    var isActiveState by remember { mutableStateOf(false) }

    // Сохраняем предыдущее положение шторки ПЕРЕД активацией сценария
    var previousPosition by remember { mutableStateOf(50f) }

    // Обновляем локальное состояние при изменении текущей шторки
    LaunchedEffect(currentCurtain) {
        val isActiveInDatabase = currentCurtain?.activeScenarios?.contains(title) == true
        if (isActiveState != isActiveInDatabase) {
            isActiveState = isActiveInDatabase
        }

        // Если сценарий активен и мы его только что получили из базы,
        // нужно восстановить предыдущее положение из локального хранилища
        if (isActiveInDatabase) {
            // Загружаем предыдущее положение из базы данных
            previousPosition = userPreferencesManager.getPreviousPositionForScenario(title)
        }
    }

    // Для готовых сценариев: проверяем, есть ли другие активные готовые сценарии
    val hasOtherExclusiveActive = remember(currentCurtain) {
        if (isExclusive && currentCurtain != null) {
            val exclusiveScenarios = listOf("Доброе утро", "Кинотеатр")
            exclusiveScenarios.any { scenario ->
                scenario != title && currentCurtain!!.activeScenarios.contains(scenario)
            }
        } else {
            false
        }
    }

    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveState) Color(0xFF1A4A53) else Color(0xFF06373E)
        )
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
                // ИКОНКИ БУДУТ ВСЕГДА БЕЛЫМИ, независимо от состояния
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
                        color = if (isActiveState) Color(0xFF81CA86) else Color.White
                    )
                    Text(
                        text = slogan,
                        fontSize = 14.sp,
                        color = if (isActiveState) Color(0xFFA8D5BA) else Color.LightGray
                    )
                    // Показываем сообщение о блокировке
                    if (isActiveState) {
                        Text(
                            text = "Ползунок заблокирован",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA500),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // Предупреждение для готовых сценариев
                    if (isExclusive && hasOtherExclusiveActive && !isActiveState) {
                        Text(
                            text = "Только 1 готовый сценарий может быть активен одновременно",
                            fontSize = 12.sp,
                            color = Color(0xFFFFA500),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Индикатор активности
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isActiveState) Color(0xFF81CA86) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isActiveState) Color(0xFF81CA86) else Color.LightGray,
                            shape = CircleShape
                        )
                )
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

            if (isActiveState) {
                Button(
                    onClick = {
                        // Сначала обновляем локальное состояние для мгновенного отклика
                        isActiveState = false

                        // Восстанавливаем предыдущее положение (которое было ДО активации)
                        selectedCurtainId?.let { curtainId ->
                            // Обновляем глобальное положение
                            CurtainPositionManager.updatePosition(curtainId, previousPosition)

                            scope.launch {
                                // Сохраняем в базу данных
                                userPreferencesManager.updateCurtainPosition(
                                    curtainId,
                                    previousPosition
                                )

                                // Для готовых сценариев также очищаем другие готовые сценарии
                                if (isExclusive) {
                                    userPreferencesManager.clearExclusiveScenariosFromCurtain(
                                        curtainId
                                    )
                                }
                                userPreferencesManager.removeScenarioFromCurtain(curtainId, title)
                            }
                        }
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
                        // Сначала обновляем локальное состояние для мгновенного отклика
                        isActiveState = true

                        // Сохраняем ТЕКУЩЕЕ положение как предыдущее ПЕРЕД активацией
                        currentCurtain?.currentPosition?.let { position ->
                            previousPosition = position
                            // Сохраняем предыдущее положение в локальное хранилище
                            userPreferencesManager.savePreviousPositionForScenario(title, position)
                        }

                        // Устанавливаем новое положение в зависимости от сценария
                        val newPosition = when (title) {
                            "Доброе утро" -> 20f // Открыть на 80% (20% закрыто)
                            "Кинотеатр" -> 100f // Закрыть полностью
                            else -> 50f
                        }

                        // Затем добавляем сценарий к шторке
                        selectedCurtainId?.let { curtainId ->
                            // Обновляем глобальное положение
                            CurtainPositionManager.updatePosition(curtainId, newPosition)

                            scope.launch {
                                // Сохраняем в базу данных
                                userPreferencesManager.updateCurtainPosition(curtainId, newPosition)

                                // Для готовых сценариев: сначала деактивируем другие готовые сценарии
                                if (isExclusive) {
                                    userPreferencesManager.clearExclusiveScenariosFromCurtain(
                                        curtainId
                                    )
                                }
                                // Добавляем текущий сценарий
                                userPreferencesManager.addScenarioToCurtain(curtainId, title)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCurtainId != null && (!isExclusive || !hasOtherExclusiveActive))
                            color6E828C6B else Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = selectedCurtainId != null && (!isExclusive || !hasOtherExclusiveActive)
                ) {
                    Text(
                        text = when {
                            selectedCurtainId == null -> "Выберите шторку"
                            isExclusive && hasOtherExclusiveActive -> "Деактивируйте другие сценарии"
                            else -> "Активировать"
                        },
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
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
    userPreferencesManager: UserPreferencesManager,
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
                                val response = RetrofitClient.instance.login(
                                    LoginRequest(username, password)
                                )

                                if (response.contains("Bad credentials", ignoreCase = true) ||
                                    response.contains("error", ignoreCase = true) ||
                                    response.length < 20
                                ) {
                                    errorMessage = "Неверный логин или пароль"
                                    isLoading = false
                                    return@launch
                                }

                                TokenManager.token = response
                                // Сохраняем данные в локальное хранилище
                                userPreferencesManager.saveAuthData(response, username)
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
    userPreferencesManager: UserPreferencesManager,
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
            !isValidUsername(username) -> "Только буквы, цифры и подчеркивание"
            username.length < 3 -> "Минимум 3 символа"
            else -> null
        }
        if (usernameError != null) isValid = false

        // Валидация имени
        firstNameError = when {
            firstName.isBlank() -> "Обязательное поле"
            !isValidName(firstName) -> "Только буквы и дефисы"
            firstName.length < 2 -> "Минимум 2 символа"
            else -> null
        }
        if (firstNameError != null) isValid = false

        // Валидация фамилии
        lastNameError = when {
            lastName.isBlank() -> "Обязательное поле"
            !isValidName(lastName) -> "Только буквы и дефисы"
            lastName.length < 4 -> "Минимум 4 символа"
            else -> null
        }
        if (lastNameError != null) isValid = false

        // Валидация email
        emailError = when {
            email.isBlank() -> "Обязательное поле"
            !isValidEmail(email) -> "Неверный формат email"
            else -> null
        }
        if (emailError != null) isValid = false

        // Валидация телефона
        phoneError = when {
            phoneNumber.isBlank() -> "Обязательное поле"
            !isValidPhone(phoneNumber) -> "Только цифры и символы +-()"
            phoneNumber.length < 11 -> "Минимум 11 символов"
            else -> null
        }
        if (phoneError != null) isValid = false

        // Валидация пароля
        passwordError = when {
            password.isBlank() -> "Обязательное поле"
            password.length < 8 -> "Минимум 8 символов"
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
        BackButton(onBackClick = onBackClick)

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
                                val token = RetrofitClient.instance.register(
                                    RegisterRequest(
                                        username = username,
                                        firstName = firstName,
                                        lastName = lastName,
                                        email = email,
                                        phoneNumber = phoneNumber,
                                        password = password
                                    )
                                )
                                TokenManager.token = token
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