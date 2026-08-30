package com.example.ui.dashboard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ExpenseModel
import com.example.MainViewModel
import com.example.StorageManager
import com.example.ui.theme.DailyCashNotebookTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun <T> LiveData<T>.observeAsState(initial: T): State<T> {
    val state = remember { mutableStateOf(this.value ?: initial) }
    DisposableEffect(this) {
        val observer = Observer<T> { value ->
            state.value = value ?: initial
        }
        observeForever(observer)
        onDispose {
            removeObserver(observer)
        }
    }
    return state
}

class ComposeDashboardActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            DailyCashNotebookTheme {
                val activeDateString by viewModel.activeDateString.observeAsState("")
                val activeDayOfWeek by viewModel.activeDayOfWeek.observeAsState("")
                val sabekCash by viewModel.sabekCash.observeAsState(0.0)
                val cashSales by viewModel.cashSales.observeAsState(0.0)
                val bakiCollection by viewModel.bakiCollection.observeAsState(0.0)
                val shopExpenses by viewModel.totalOperatingExpenses.observeAsState(0.0)
                val expensesList by viewModel.expenses.observeAsState(emptyList())

                val storage = remember { StorageManager.getInstance(this@ComposeDashboardActivity) }
                var homeExpensesSum by remember(activeDateString) {
                    val allHome = storage.loadHomeExpenseRecords()
                    val sum = allHome.filter { it.date == activeDateString }.sumOf { it.amount }
                    mutableDoubleStateOf(sum)
                }

                DashboardScreen(
                    currentDateString = if (activeDateString.isNotEmpty()) activeDateString else SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()),
                    dayOfWeek = activeDayOfWeek,
                    openingBalance = sabekCash,
                    dailyCashSales = cashSales,
                    dailyBakiCollection = bakiCollection,
                    dailyShopExpenses = shopExpenses,
                    dailyHomeExpenses = homeExpensesSum,
                    expenseList = expensesList,
                    onPrevDayClick = {
                        viewModel.moveToPreviousDay()
                    },
                    onNextDayClick = {
                        viewModel.moveToNextDay()
                    },
                    onDateSelect = { newDate ->
                        viewModel.setActiveDate(newDate)
                    },
                    onAddIncome = { amount, _, _ ->
                        val currentAvail = viewModel.availableCash.value ?: 0.0
                        viewModel.setAvailableCash(currentAvail + amount)
                        Toast.makeText(this@ComposeDashboardActivity, "আয় সফলভাবে যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    onAddExpense = { amount, category, isHome, note ->
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                        val title = if (note.isNotBlank()) "$category: $note" else category
                        if (isHome) {
                            val exp = ExpenseModel(
                                UUID.randomUUID().toString(),
                                title,
                                amount,
                                timeStr,
                                activeDateString,
                                ExpenseModel.TYPE_HOME
                            )
                            storage.addHomeExpense(exp)
                            val allHome = storage.loadHomeExpenseRecords()
                            homeExpensesSum = allHome.filter { it.date == activeDateString }.sumOf { it.amount }
                        } else {
                            viewModel.addExpense(title, amount, ExpenseModel.TYPE_SHOP)
                        }
                        Toast.makeText(this@ComposeDashboardActivity, "খরচ সফলভাবে যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateOpeningBalance = { newBalance ->
                        viewModel.setSabekCash(newBalance)
                        Toast.makeText(this@ComposeDashboardActivity, "প্রারম্ভিক ক্যাশ হালনাগাদ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteExpense = { expId ->
                        viewModel.deleteExpense(expId)
                        Toast.makeText(this@ComposeDashboardActivity, "খরচ মোছা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxSize().statusBarsPadding()
                )
            }
        }
    }
}
