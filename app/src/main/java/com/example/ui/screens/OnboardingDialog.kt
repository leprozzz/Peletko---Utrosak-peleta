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
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var currentStep by rememberSaveable { mutableIntStateOf(1) } // 1, 2, 3
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    // Validacija i greške
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var cityHasError by rememberSaveable { mutableStateOf(false) }
    var powerHasError by rememberSaveable { mutableStateOf(false) }
    var areaHasError by rememberSaveable { mutableStateOf(false) }
    var brandHasError by rememberSaveable { mutableStateOf(false) }
    var stockHasError by rememberSaveable { mutableStateOf(false) }
    var priceHasError by rememberSaveable { mutableStateOf(false) }

    // Korak 1: Jezik (SR, BA, HR, ENG), Valuta, Grad
    var selectedLanguage by rememberSaveable { mutableStateOf("sr") }
    var selectedCurrency by rememberSaveable { mutableStateOf("BAM") }
    var cityText by rememberSaveable { mutableStateOf("Sarajevo") }

    // Korak 2: Snaga peći, kvadratura, izolacija
    var areaText by rememberSaveable { mutableStateOf("120") }
    var powerText by rememberSaveable { mutableStateOf("20") }
    var selectedInsulation by rememberSaveable { mutableStateOf("Dobra (10cm)") }

    // Korak 3: Stanje magacina (Proizvođač, Klasa, Dvostrani unos paleta/vreća, cijena po paleti)
    var brandText by rememberSaveable { mutableStateOf("Medex") }
    var selectedClass by rememberSaveable { mutableStateOf("A1") } // "A1", "A2", "Bez klase"
    var palletsText by rememberSaveable { mutableStateOf("2.0") }
    var bagsText by rememberSaveable { mutableStateOf("140") }
    var palletPriceText by rememberSaveable { mutableStateOf("") } // Cijena po paleti starta prazna

    val insulationOptions = listOf(
        "Bez izolacije",
        "Djelimična (5cm)",
        "Dobra (10cm)",
        "Odlična (15cm+)"
    )

    val classOptions = listOf("A1", "A2", "Bez klase")
    // Omo-Prom obrisan iz brzog biranja prema zahtjevu
    val popularManufacturers = listOf("Medex", "Drvoprodex", "Fagus", "Kovan", "Šišarka", "Ensa", "Foresta", "Moj Pelet")

    val currencies = listOf("BAM", "EUR", "DIN")
    // Redoslijed jezika prema zahtjevu: SR, BA, HR, ENG
    val languages = listOf("sr" to "SR", "bs" to "BA", "hr" to "HR", "en" to "ENG")
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
                            onClick = {
                                errorMessage = null
                                currentStep -= 1
                            },
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
                            when (currentStep) {
                                1 -> {
                                    // Validacija Koraka 1: Grad ne smije biti prazan
                                    if (cityText.trim().isBlank()) {
                                        cityHasError = true
                                        errorMessage = AppStrings.get("fill_all_fields_error", selectedLanguage)
                                        return@Button
                                    }
                                    cityHasError = false
                                    errorMessage = null
                                    currentStep = 2
                                }
                                2 -> {
                                    // Validacija Koraka 2: Kvadratura i Snaga peći moraju biti popunjene i > 0
                                    val areaVal = areaText.replace(",", ".").toDoubleOrNull()
                                    val powerVal = powerText.replace(",", ".").toDoubleOrNull()
                                    var hasErr = false

                                    if (areaText.trim().isBlank() || areaVal == null || areaVal <= 0.0) {
                                        areaHasError = true
                                        hasErr = true
                                    } else {
                                        areaHasError = false
                                    }

                                    if (powerText.trim().isBlank() || powerVal == null || powerVal <= 0.0) {
                                        powerHasError = true
                                        hasErr = true
                                    } else {
                                        powerHasError = false
                                    }

                                    if (hasErr) {
                                        errorMessage = AppStrings.get("fill_all_fields_error", selectedLanguage)
                                        return@Button
                                    }

                                    errorMessage = null
                                    currentStep = 3
                                }
                                3 -> {
                                    // Validacija Koraka 3: Proizvođač, Količina i Cijena po paleti moraju biti popunjeni
                                    val palletsVal = palletsText.replace(",", ".").toDoubleOrNull()
                                    val priceVal = palletPriceText.replace(",", ".").toDoubleOrNull()
                                    var hasErr = false

                                    if (brandText.trim().isBlank()) {
                                        brandHasError = true
                                        hasErr = true
                                    } else {
                                        brandHasError = false
                                    }

                                    if (palletsText.trim().isBlank() || palletsVal == null || palletsVal <= 0.0) {
                                        stockHasError = true
                                        hasErr = true
                                    } else {
                                        stockHasError = false
                                    }

                                    if (palletPriceText.trim().isBlank() || priceVal == null || priceVal <= 0.0) {
                                        priceHasError = true
                                        hasErr = true
                                    } else {
                                        priceHasError = false
                                    }

                                    if (hasErr) {
                                        errorMessage = AppStrings.get("fill_all_fields_error", selectedLanguage)
                                        return@Button
                                    }

                                    if (isSubmitting) return@Button
                                    isSubmitting = true
                                    errorMessage = null

                                    val finalArea = areaText.replace(",", ".").toDoubleOrNull() ?: 120.0
                                    val finalPower = powerText.replace(",", ".").toDoubleOrNull() ?: 20.0
                                    val finalPallets = palletsVal ?: 2.0
                                    val finalPrice = priceVal ?: 525.0

                                    onComplete(
                                        finalArea,
                                        finalPower,
                                        selectedInsulation,
                                        cityText.trim(),
                                        brandText.trim(),
                                        selectedClass,
                                        finalPallets,
                                        finalPrice,
                                        selectedCurrency,
                                        selectedLanguage
                                    )
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(if (currentStep > 1) 0.62f else 1f)
                            .height(54.dp)
                            .testTag("onboarding_finish_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
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
            // Prikaz greške ako neko polje nije popunjeno
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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

                    // Jezik (SR, BA, HR, ENG)
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

                    // Valuta (BAM, EUR, DIN)
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

                    // Grad / Klima zona -> Unesi grad (Klimatska zona)
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("city", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = cityText,
                                onValueChange = {
                                    cityText = it
                                    if (it.trim().isNotBlank()) {
                                        cityHasError = false
                                        errorMessage = null
                                    }
                                },
                                label = { Text(AppStrings.get("city", selectedLanguage)) },
                                isError = cityHasError,
                                supportingText = if (cityHasError) {
                                    { Text(AppStrings.get("field_required", selectedLanguage), color = MaterialTheme.colorScheme.error) }
                                } else null,
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
                                            .clickable {
                                                cityText = city
                                                cityHasError = false
                                                errorMessage = null
                                            }
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
                                    onValueChange = {
                                        powerText = it
                                        if (it.trim().isNotBlank()) {
                                            powerHasError = false
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(AppStrings.get("boiler_power_kw", selectedLanguage)) },
                                    isError = powerHasError,
                                    supportingText = if (powerHasError) {
                                        { Text(AppStrings.get("field_required", selectedLanguage), color = MaterialTheme.colorScheme.error) }
                                    } else null,
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
                                    onValueChange = {
                                        areaText = it
                                        if (it.trim().isNotBlank()) {
                                            areaHasError = false
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(AppStrings.get("heating_area_m2", selectedLanguage)) },
                                    isError = areaHasError,
                                    supportingText = if (areaHasError) {
                                        { Text(AppStrings.get("field_required", selectedLanguage), color = MaterialTheme.colorScheme.error) }
                                    } else null,
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

                    // 1. Proizvođač -> Unesi proizvodjača peleta
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("brand", selectedLanguage), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = brandText,
                                onValueChange = {
                                    brandText = it
                                    if (it.trim().isNotBlank()) {
                                        brandHasError = false
                                        errorMessage = null
                                    }
                                },
                                label = { Text(AppStrings.get("brand", selectedLanguage)) },
                                isError = brandHasError,
                                supportingText = if (brandHasError) {
                                    { Text(AppStrings.get("field_required", selectedLanguage), color = MaterialTheme.colorScheme.error) }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_brand_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Brzi odabir popularnih proizvođača (bez Omo-Prom)
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
                                            .clickable {
                                                brandText = man
                                                brandHasError = false
                                                errorMessage = null
                                            }
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

                    // 3. Dvostrani unos: Količina na stanju (unesi palete ili vreće) + Cijena po paleti
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = AppStrings.get("stock_quantity_pallets_bags", selectedLanguage),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Unos paleta
                                OutlinedTextField(
                                    value = palletsText,
                                    onValueChange = { input ->
                                        palletsText = input
                                        if (input.trim().isNotBlank()) {
                                            stockHasError = false
                                            errorMessage = null
                                        }
                                        val p = input.replace(",", ".").toDoubleOrNull()
                                        if (p != null) {
                                            val calculatedBags = (p * 70).toInt()
                                            bagsText = calculatedBags.toString()
                                        }
                                    },
                                    label = { Text(AppStrings.get("num_pallets", selectedLanguage)) },
                                    isError = stockHasError,
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
                                        if (input.trim().isNotBlank()) {
                                            stockHasError = false
                                            errorMessage = null
                                        }
                                        val b = input.toIntOrNull()
                                        if (b != null) {
                                            val calculatedPallets = (b / 70.0 * 10).toInt() / 10.0
                                            palletsText = calculatedPallets.toString()
                                        }
                                    },
                                    label = { Text(AppStrings.get("exact_bags", selectedLanguage)) },
                                    isError = stockHasError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("onboarding_bags_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            if (stockHasError) {
                                Text(
                                    text = AppStrings.get("field_required", selectedLanguage),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            // 4. Cijena po paleti - ostavljena prazna sa obaveznim unosom i traženom napomenom
                            val palletPriceNote = "${AppStrings.get("pallet_price_note", selectedLanguage)} ($selectedCurrency)"
                            OutlinedTextField(
                                value = palletPriceText,
                                onValueChange = {
                                    palletPriceText = it
                                    if (it.trim().isNotBlank()) {
                                        priceHasError = false
                                        errorMessage = null
                                    }
                                },
                                label = { Text(palletPriceNote) },
                                placeholder = { Text("Unesi cijenu po paleti (1 paleta = 70 vreća)") },
                                isError = priceHasError,
                                supportingText = if (priceHasError) {
                                    { Text(AppStrings.get("field_required", selectedLanguage), color = MaterialTheme.colorScheme.error) }
                                } else {
                                    { Text(palletPriceNote, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                },
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
