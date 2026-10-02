package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AppleCard
import com.example.util.AppStrings
import com.example.util.PelletCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        areaM2: Double,
        powerKw: Double,
        insulation: String,
        city: String,
        brand: String,
        pelletClass: String,
        initialPallets: Double,
        palletPriceKM: Double,
        currency: String,
        language: String
    ) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3

    // Korak 1: Jezik, Valuta, Grad
    var selectedLanguage by remember { mutableStateOf("bs") }
    var selectedCurrency by remember { mutableStateOf("BAM") }
    var cityText by remember { mutableStateOf("Sarajevo") }

    // Korak 2: Snaga peći, kvadratura, izolacija (fokus isključivo na peć i površinu)
    var areaText by remember { mutableStateOf("120") }
    var powerText by remember { mutableStateOf("20") }
    var selectedInsulation by remember { mutableStateOf("Dobra (10cm)") }

    // Korak 3: Stanje magacina (Proizvođač, Klasa, Dvostrani unos paleta/vreća, cijena)
    var brandText by remember { mutableStateOf("Medex") }
    var selectedClass by remember { mutableStateOf("A1") } // "A1", "A2", "Bez klase"
    var palletsText by remember { mutableStateOf("2.0") }
    var bagsText by remember { mutableStateOf("140") }
    var palletPriceText by remember { mutableStateOf("520") }

    val insulationOptions = listOf(
        "Bez izolacije",
        "Djelimična (5cm)",
        "Dobra (10cm)",
        "Odlična (15cm+)"
    )

    val classOptions = listOf("A1", "A2", "Bez klase")
    val popularManufacturers = listOf("Medex", "Drvoprodex", "Fagus", "Kovan", "Šišarka", "Ensa", "Foresta", "Moj Pelet", "Omo-Prom")

    val currencies = listOf("BAM", "EUR", "DIN")
    val languages = listOf("bs" to "BiH", "sr" to "Srp", "hr" to "Hrv", "en" to "Eng")
    val popularCities = listOf("Sarajevo", "Banja Luka", "Tuzla", "Mostar", "Zenica", "Beograd", "Novi Sad", "Zagreb")

    val todaySeason = PelletCalculator.computeSeason(PelletCalculator.getTodayISO())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_peletko_logo),
                                contentDescription = "Peletko",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = AppStrings.get("app_title", selectedLanguage),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = AppStrings.get("initial_setup", selectedLanguage),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = String.format(AppStrings.get("step_x_of_y", selectedLanguage), currentStep, 3),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { currentStep / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .weight(0.38f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("back", selectedLanguage), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                val area = areaText.replace(",", ".").toDoubleOrNull() ?: 120.0
                                val p = powerText.replace(",", ".").toDoubleOrNull() ?: 20.0
                                val pallets = palletsText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val price = palletPriceText.replace(",", ".").toDoubleOrNull() ?: 520.0

                                onComplete(
                                    area,
                                    p,
                                    selectedInsulation,
                                    cityText.ifBlank { "Sarajevo" },
                                    brandText.ifBlank { "Medex" },
                                    selectedClass,
                                    pallets,
                                    price,
                                    selectedCurrency,
                                    selectedLanguage
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentStep > 1) 0.62f else 1f)
                            .height(54.dp)
                            .testTag("onboarding_finish_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (currentStep < 3) AppStrings.get("next", selectedLanguage) else AppStrings.get("start_tracking", selectedLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            when (currentStep) {
                // KORAK 1: Jezik, Valuta i Grad
                1 -> {
                    Text(
                        text = AppStrings.get("onboarding_step1_title", selectedLanguage),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = AppStrings.get("onboarding_step1_sub", selectedLanguage),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Jezik
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("language", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                languages.forEach { (code, label) ->
                                    val isSelected = (selectedLanguage == code)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .clickable { selectedLanguage = code }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Valuta
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("currency", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currencies.forEach { curr ->
                                    val isSelected = (selectedCurrency == curr)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .clickable { selectedCurrency = curr }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = curr,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grad
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("city", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = cityText,
                                onValueChange = { cityText = it },
                                label = { Text(AppStrings.get("city_name", selectedLanguage)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_city_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Brzi odabir popularnih gradova
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                popularCities.forEach { city ->
                                    val isMatch = cityText.equals(city, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { cityText = city }
                                    ) {
                                        Text(
                                            text = city,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // KORAK 2: Snaga peći, kvadratura i izolacija
                2 -> {
                    Text(
                        text = AppStrings.get("onboarding_step2_title", selectedLanguage),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = AppStrings.get("onboarding_step2_sub", selectedLanguage),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Snaga peći i kvadratura
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("settings_sec3", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = powerText,
                                    onValueChange = { powerText = it },
                                    label = { Text(AppStrings.get("boiler_power_kw", selectedLanguage)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("onboarding_power_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = areaText,
                                    onValueChange = { areaText = it },
                                    label = { Text(AppStrings.get("heating_area_m2", selectedLanguage)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("onboarding_area_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Izolacija
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(AppStrings.get("insulation", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                insulationOptions.forEach { opt ->
                                    val selected = (selectedInsulation == opt)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedInsulation = opt }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = PelletCalculator.getInsulationLabel(opt, selectedLanguage), fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                // KORAK 3: Unesi stanje magacina
                3 -> {
                    Text(
                        text = AppStrings.get("onboarding_step3_title", selectedLanguage),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = AppStrings.get("onboarding_step3_sub", selectedLanguage),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // 1. Proizvođač
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("brand", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = brandText,
                                onValueChange = { brandText = it },
                                label = { Text(AppStrings.get("supplier_placeholder", selectedLanguage)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_brand_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Brzi odabir popularnih proizvođača
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                popularManufacturers.forEach { man ->
                                    val isSel = brandText.equals(man, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { brandText = man }
                                    ) {
                                        Text(
                                            text = man,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Klasa peleta (A1, A2, Bez klase)
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Grade, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("pellet_class", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                classOptions.forEach { opt ->
                                    val isSelected = (selectedClass == opt)
                                    val classLabel = if (selectedLanguage.lowercase() == "en" && opt == "Bez klase") "Standard" else opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .clickable { selectedClass = opt }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = classLabel,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Dvostrani unos: Palete ili Vreće
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(AppStrings.get("stock_quantity_pallets_bags", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Unos paleta
                                OutlinedTextField(
                                    value = palletsText,
                                    onValueChange = { input ->
                                        palletsText = input
                                        val p = input.replace(",", ".").toDoubleOrNull()
                                        if (p != null) {
                                            val calculatedBags = (p * 70).toInt()
                                            bagsText = calculatedBags.toString()
                                        }
                                    },
                                    label = { Text(AppStrings.get("num_pallets", selectedLanguage)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("onboarding_pallets_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Unos vreća
                                OutlinedTextField(
                                    value = bagsText,
                                    onValueChange = { input ->
                                        bagsText = input
                                        val b = input.toIntOrNull()
                                        if (b != null) {
                                            val calculatedPallets = (b / 70.0 * 10).toInt() / 10.0
                                            palletsText = calculatedPallets.toString()
                                        }
                                    },
                                    label = { Text(AppStrings.get("exact_bags", selectedLanguage)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("onboarding_bags_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            // 4. Cijena po paleti
                            val palletPriceLabel = if (selectedLanguage.lowercase() == "en")
                                "Price per pallet ( 70 bags - 1050kg ) ($selectedCurrency)"
                            else
                                "Cijena po paleti ( 70 vreća - 1050kg ) ($selectedCurrency)"
                            OutlinedTextField(
                                value = palletPriceText,
                                onValueChange = { palletPriceText = it },
                                label = { Text(palletPriceLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_price_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Tekuća sezona - informativno deklarisana
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Deklarisana sezona: ",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = todaySeason,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(informativno)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
