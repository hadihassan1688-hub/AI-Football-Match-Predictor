package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class FootballMatch(val id: Int, val homeTeam: String, val awayTeam: String, val date: String, val league: String)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = Background
        ) { innerPadding ->
          MatchPredictorScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun MatchPredictorScreen(modifier: Modifier = Modifier) {
    var matches by remember { mutableStateOf<List<FootballMatch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = fetchMatches()
        if (result.isSuccess) {
            matches = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
        }
        isLoading = false
    }
    
    val today = java.time.LocalDate.now()
    val dateRange = (-7..23).map { today.plusDays(it.toLong()) }
    var selectedDate by remember { mutableStateOf(today) }

    val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
    val filteredMatches = matches.filter { it.date == selectedDate.format(formatter) }
    
    val groupedMatches = filteredMatches.groupBy { it.league }
    
    Column(modifier = modifier.fillMaxSize().background(Background)) {
        Text("AI Match Predictor", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.padding(16.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dateRange) { date ->
                val isSelected = date == selectedDate
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDate = date },
                    label = { Text(date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CardBackground,
                        selectedContainerColor = Accent,
                        labelColor = Color.White,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $errorMessage", color = Color.Red)
            }
        } else if (filteredMatches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches for ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}.", color = TextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groupedMatches.forEach { (league, leagueMatches) ->
                    item {
                        Text(league, style = MaterialTheme.typography.titleSmall, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(leagueMatches) { match ->
                        MatchItem(match)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchItem(match: FootballMatch) {
    var prediction by remember { mutableStateOf<String?>(null) }
    var isPredicting by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = match.homeTeam, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(text = "vs", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(text = match.awayTeam, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text(text = "Date: ${match.date}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                if (isPredicting) {
                    Text("Predicting...", color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (prediction != null) {
                    Text("AI Prediction:", color = Accent, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = prediction!!, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Button(onClick = {
                        isPredicting = true
                        scope.launch {
                            prediction = predictMatch(match.homeTeam, match.awayTeam)
                            isPredicting = false
                        }
                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Get AI Insight")
                    }
                }
            }
        }
    }
}
