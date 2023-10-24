package com.example.summaryyoupython

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import com.chaquo.python.*
import com.chaquo.python.android.AndroidPlatform
import com.example.summaryyoupython.ui.theme.SummaryYouPythonTheme
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavHostController
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) { //If not started, python will start here
            Python.start(AndroidPlatform(this))
        }
        // Access the applicationContext and pass it to ViewModel class
        val viewModel = TextSummaryViewModel(applicationContext)

        // Lay app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if a link has been shared
        val intent: Intent? = intent
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        setContent {
            SummaryYouPythonTheme(OledModeEnabled = true) {
                val navController = rememberNavController()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController, applicationContext, sharedUrl)
                }
            }
        }
    }
}


class TextSummaryViewModel(private val context: Context) : ViewModel() {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("TextSummaries", Context.MODE_PRIVATE)

    val textSummaries = mutableStateListOf<TextSummary>()

    init {
        // When creating the ViewModel, retrieve the saved data (if any)
        val savedTextSummaries = sharedPreferences.getString("textSummaries", null)
        savedTextSummaries?.let {
            val type = object : TypeToken<List<TextSummary>>() {}.type
            val textSummariesFromJson = Gson().fromJson<List<TextSummary>>(it, type)
            textSummaries.addAll(textSummariesFromJson)
        }
    }

    fun addTextSummary(title: String?, author: String?, text: String?) {
        val nonNullTitle = title ?: ""
        val nonNullAuthor = author ?: ""

        if (text != null && text.isNotBlank() && text != "invalid link") {
            val newTextSummary = TextSummary(nonNullTitle, nonNullAuthor, text)
            textSummaries.add(newTextSummary)
            // Save text data in SharedPreferences
            saveTextSummaries()
        }
    }

    private fun saveTextSummaries() {
        val textSummariesJson = Gson().toJson(textSummaries)
        sharedPreferences.edit().putString("textSummaries", textSummariesJson).apply()
    }

    fun removeTextSummary(title: String?, author: String?, text: String?) {
        // Find the TextSummary object to remove based on title, author, and text
        val textSummaryToRemove = textSummaries.firstOrNull { it.title == title && it.author == author && it.text == text }

        // Check whether a matching TextSummary object was found and remove it
        textSummaryToRemove?.let {
            textSummaries.remove(it)

            // After removing, save the updated ViewModel (if necessary)
            saveTextSummaries()
        }
    }
    var useOriginalLanguage by mutableStateOf(sharedPreferences.getBoolean("useOriginalLanguage", false))

    // Funktion, um den Wert der Boolean-Variable festzulegen und in den SharedPreferences zu speichern
    fun setUseOriginalLanguageValue(newValue: Boolean) {
        useOriginalLanguage = newValue
        sharedPreferences.edit().putBoolean("useOriginalLanguage", newValue).apply()
    }

    // Funktion, um den Wert der Boolean-Variable aus den SharedPreferences abzurufen
    fun getUseOriginalLanguageValue(): Boolean {
        return useOriginalLanguage
    }

}


data class TextSummary(val title: String, val author: String, val text: String)

@Composable
fun AppNavigation(navController: NavHostController, applicationContext: Context, initialUrl: String? = null) {
    val viewModel = TextSummaryViewModel(applicationContext) //For History
    NavHost(navController, startDestination = "home") {
        composable("home") {
            homeScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel,
                initialUrl
            )
        }
        composable("settings") {
            settingsScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
        composable("history") {
            historyScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun homeScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: TextSummaryViewModel, initialUrl: String? = null) {
    var transcriptResult by remember { mutableStateOf<String?>(null) } // State for the transcript retrieval result
    var title by remember { mutableStateOf<String?>(null) }
    var author by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // For Loading-Animation
    var url by remember { mutableStateOf(initialUrl ?: "") }
    val scope = rememberCoroutineScope() // Python needs asynchronous call
    val context = LocalContext.current // Clipboard
    val haptics = LocalHapticFeedback.current // Vibrations
    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing
    var selectedIndex by remember { mutableStateOf(0) } // Summary length index
    val options = listOf(stringResource(id = R.string.short_length), stringResource(id = R.string.middle_length), stringResource(id = R.string.long_length)) // Lengths
    val showCancelIcon by remember { derivedStateOf { url.isNotBlank() } }
    var isError by remember { mutableStateOf(false) }

    val clipboardManager = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java
    ) as ClipboardManager

    Box() {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {navController.navigate("settings")},
                    modifier = modifier.padding(start = 8.dp, top = 40.dp)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
                IconButton(
                    onClick = {navController.navigate("history")},
                    modifier = modifier.padding(end = 8.dp, top = 40.dp)
                ) {
                    Icon( painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_library_books_24), contentDescription = "History")
                }
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Summary You",
                    style = MaterialTheme.typography.headlineLarge
                )
                // Loading-Animation
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                } else {
                    Spacer(modifier = modifier.height(height = 9.dp))
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = when (transcriptResult){
                                    "Exception: no internet" -> stringResource(id = R.string.noInternet)
                                    "Exception: invalid link" -> stringResource(id = R.string.invalidURL)
                                    "Exception: no transcript" -> stringResource(id = R.string.noTranscript)
                                    "Exception: no content" -> stringResource(id = R.string.noContent)
                                    "Exception: paywall detected" -> stringResource(id = R.string.paywallDetected)
                                    else -> transcriptResult ?: "unknown error 3" },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (showCancelIcon) {
                            IconButton(
                                onClick = {
                                    url = ""
                                    transcriptResult = null
                                    isError = false // No error
                                    focusRequester.requestFocus()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_cancel_24),
                                    contentDescription = "Cancel"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .focusRequester(focusRequester)
                )
                Box(
                    modifier = if (isError) {
                        Modifier.padding(top = 11.dp)
                    } else {
                        Modifier.padding(top = 15.dp)
                    }
                ) {
                    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.shape(position = index, count = options.size),
                                onClick = { selectedIndex = index },
                                selected = index == selectedIndex
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                if (!transcriptResult.isNullOrEmpty() && isError == false) {
                    Card(
                        modifier = modifier
                            .padding(top = 15.dp, bottom = 15.dp)
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Copy the contents of the box to the clipboard
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText(null, transcriptResult)
                                    )
                                }
                            )
                    ) {
                            if(!title.isNullOrEmpty()) {
                                Text(
                                    text = title ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = modifier
                                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                                )
                                if(!author.isNullOrEmpty()) {
                                    Row {
                                        Text(
                                            text = author ?: "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = modifier
                                                .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                                        )
                                        if (isYouTubeLink(url)) {
                                            Icon(
                                                painter = painterResource(id = com.example.summaryyoupython.R.drawable.youtube),
                                                contentDescription = null,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        Text(
                            text = transcriptResult ?: stringResource(id = R.string.noTranscript),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = modifier
                                .padding(start=12.dp, end=12.dp, top=10.dp, bottom=12.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(top = 15.dp, bottom = 90.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                isLoading = true // Start Loading-Animation
                                isError = false // No error
                                scope.launch {
                                    title = getTitel(url)
                                    author = getAuthor(url)
                                    val (result, error) = summarize(url, selectedIndex, viewModel)
                                    transcriptResult = result
                                    isError = error
                                    isLoading = false // Stop Loading-Animation
                                    if(!isError){
                                        viewModel.addTextSummary(title, author, transcriptResult) // Add to history
                                    }
                                }
                            },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(id = R.string.regenerate))
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .imePadding()
            .imeNestedScroll()
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // Check if there is anything on the clipboard
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val clipItem = clipData.getItemAt(0)
                            url = clipItem.text.toString()
                        }
                    }
                },
                modifier = modifier.padding(bottom = 20.dp, end = 15.dp)
            ) {
                Icon(painter = painterResource(id = com.example.summaryyoupython.R.drawable.outline_content_paste_24), "Paste")
            }
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    isLoading = true // Start Loading-Animation
                    if(isError){transcriptResult = ""}
                    isError = false // No error
                    scope.launch {
                        title = getTitel(url)
                        author = getAuthor(url)
                        val (result, error) = summarize(url, selectedIndex, viewModel)
                        transcriptResult = result
                        isError = error
                        isLoading = false // Stop Loading-Animation
                        if(!isError){
                            viewModel.addTextSummary(title, author, transcriptResult) // Add to history
                        }
                    }
                },
                modifier = modifier.padding(bottom = 60.dp, end = 15.dp)
            ) {
                Icon(Icons.Filled.Check, "Check")
            }
        }
    }
}

suspend fun summarize(url: String, length: Int, viewModel: TextSummaryViewModel): Pair<String, Boolean> {
    val py = Python.getInstance()
    val module = py.getModule("youtube")
    val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    val key = dotenv["OPEN_AI_KEY"]

    // Get the currently set language
    val currentLocale: Locale = Resources.getSystem().configuration.locale

    val language: String = if (viewModel.getUseOriginalLanguageValue()) {
        "the same language as the "
    } else {
        currentLocale.getDisplayLanguage(Locale.ENGLISH)
    }

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("summarize", url, key, length, language).toString()
        }
        return Pair(result, false)
    } catch (e: PyException) {
        return Pair(e.message ?: "unknown error 2", true)
    } catch (e: Exception) {
        return Pair(e.message ?: "unknown error 3", true)
    }
}



suspend fun getAuthor(url: String): String? {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_author", url).toString()
        }
        return result
    } catch (e: Exception) {
        //return "Error getting author"
        return null
    }
}

suspend fun getTitel(url: String): String? {
    val py = Python.getInstance()
    val module = py.getModule("youtube")

    try {
        val result = withContext(Dispatchers.IO) {
            module.callAttr("get_title", url).toString()
        }
        return result
    } catch (e: Exception) {
        //return "Error getting title"
        return null
    }
}

fun isYouTubeLink(input: String): Boolean {
    val youtubePattern = Regex("""^(https?://)?(www\.)?(youtube\.com|youtu\.be)/.*$""")
    return youtubePattern.matches(input)
}

