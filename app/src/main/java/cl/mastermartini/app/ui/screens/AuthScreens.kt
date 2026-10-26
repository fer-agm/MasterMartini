@file:OptIn(ExperimentalMaterial3Api::class)

package cl.mastermartini.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.mastermartini.app.domain.Categories
import cl.mastermartini.app.domain.UserRole
import cl.mastermartini.app.nativeaccess.buzz
import cl.mastermartini.app.ui.components.*
import cl.mastermartini.app.viewmodel.AuthViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cl.mastermartini.app.R

@Composable
fun LoginScreen(vm: AuthViewModel, onRegister: () -> Unit) {
    val f = vm.loginForm
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo de Master Martini",
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentScale = ContentScale.Fit
        )
        Text("La mejor materia grasa vegetal para elaborar todo tipo de bollería",
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        ValidatedField("Correo electrónico", f.value("email"),

            { newValue ->
                val processedValue = newValue.replace(' ','@')
                vm.clearError(); f.set("email", processedValue) },
            f.error("email"), f.isValid("email"), Icons.Filled.Email, keyboard = KeyboardType.Email
        )
        ValidatedField("Contraseña", f.value("password"), { vm.clearError(); f.set("password", it) },
            f.error("password"), false, Icons.Filled.Lock, password = true, imeAction = ImeAction.Done)

        AnimatedVisibility(vm.error != null) {
            Text(vm.error.orEmpty(), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = { vm.login { buzz(ctx, haptics) } },
            enabled = !vm.loading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (vm.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Ingresar")
        }
        TextButton(onClick = onRegister) { Text("¿No tienes cuenta? Regístrate") }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cuentas de prueba (datos ficticios)", fontWeight = FontWeight.Bold)
                Text("Cliente: cliente@demo.cl / Cliente123")
                Text("Chef: chef@demo.cl / Chef1234")
                Text("Marketing: mkt@demo.cl / Mkt12345")
            }
        }
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val f = vm.registerForm
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    val role = f.value("role")
    val isClient = role == UserRole.CLIENT.name

    Scaffold(topBar = {
        TopAppBar(title = { Text("Crear cuenta") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("¿Cómo vas a usar la app?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.values().forEach { r ->
                    FilterChip(selected = role == r.name, onClick = { f.set("role", r.name) }, label = { Text(r.label) })
                }
            }
            AnimatedVisibility(!isClient) {
                ValidatedField("Código de acceso interno", f.value("code"), { f.set("code", it) },
                    f.error("code"), f.isValid("code"), Icons.Filled.Key)
            }

            ValidatedField("Nombre completo", f.value("name"), { f.set("name", it) },
                f.error("name"), f.isValid("name"), Icons.Filled.Person)
            ValidatedField("Correo electrónico", f.value("email"),
                { newValue ->
                    val processedValue = newValue.replace(' ','@')
                    vm.clearError(); f.set("email", processedValue) },

                f.error("email"), f.isValid("email"), Icons.Filled.Email, keyboard = KeyboardType.Email)
            ValidatedField("Teléfono", f.value("phone"), { f.set("phone", it) },
                f.error("phone"), f.isValid("phone"), Icons.Filled.Phone, keyboard = KeyboardType.Phone)

            AnimatedVisibility(isClient) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValidatedField("RUT de empresa (ficticio)", f.value("rut"), { f.set("rut", it) },
                        f.error("rut"), f.isValid("rut"), Icons.Filled.Badge)
                    DropdownField("Tipo de negocio", f.value("businessType"), Categories.businessTypes, Icons.Filled.Store) { f.set("businessType", it) }
                    DropdownField("Tamaño del negocio", f.value("businessSize"), Categories.businessSizes, Icons.Filled.Group) { f.set("businessSize", it) }
                }
            }

            ValidatedField("Contraseña", f.value("password"), { f.set("password", it) },
                f.error("password"), f.isValid("password"), Icons.Filled.Lock, password = true)
            ValidatedField("Repetir contraseña", f.value("confirm"), { f.set("confirm", it) },
                f.error("confirm"), f.isValid("confirm"), Icons.Filled.Lock, password = true, imeAction = ImeAction.Done)

            AnimatedVisibility(vm.error != null) {
                Text(vm.error.orEmpty(), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = { vm.register { buzz(ctx, haptics) } },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Crear cuenta") }
            Text("Usa solo datos ficticios: este es un MVP académico.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
