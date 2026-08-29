package com.todus.messenger.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ----------------------------------------------------------------------------
// Color de marca ToDus (azul institucional)
// ----------------------------------------------------------------------------
private val TodusBlue = Color(0xFF0066CC)

// ----------------------------------------------------------------------------
// Estado de la interfaz de usuario del login
// ----------------------------------------------------------------------------
/**
 * Estado reactivo que representa la pantalla de inicio de sesión.
 *
 * @param phone Número de teléfono ingresado por el usuario (formato 53XXXXXXXX).
 * @param password Contraseña ingresada por el usuario.
 * @param isLoading Indica si se está realizando la conexión con el servidor.
 * @param error Mensaje de error visible en pantalla, o null si no hay error.
 * @param isConnected true cuando la conexión XMPP fue exitosa.
 */
data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false
)

// ----------------------------------------------------------------------------
// ViewModel del login (inyectado con Hilt)
// ----------------------------------------------------------------------------
/**
 * ViewModel encargado de gestionar la lógica de inicio de sesión.
 *
 * Recibe el [ToDusXmppClient] inyectado por Hilt y expone un
 * [StateFlow] de [LoginUiState] para que la UI reaccione a los cambios.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val xmppClient: ToDusXmppClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    /** Estado de la UI expuesto como flujo de solo lectura. */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Actualiza el número de teléfono en el estado. */
    fun onPhoneChanged(phone: String) {
        // Solo permitimos dígitos, máximo 10 caracteres
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phone = filtered, error = null) }
    }

    /** Actualiza la contraseña en el estado. */
    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    /**
     * Intenta conectar al servidor XMPP de ToDus con los datos actuales.
     * Actualiza el estado con isLoading, error o isConnected según el resultado.
     */
    fun connect() {
        val currentState = _uiState.value

        // Validar que el teléfono tenga exactamente 10 dígitos
        if (currentState.phone.length != 10) {
            _uiState.update { it.copy(error = "El número debe tener 10 dígitos (53XXXXXXXX)") }
            return
        }

        // Validar que no esté vacía la contraseña
        if (currentState.password.isEmpty()) {
            _uiState.update { it.copy(error = "Ingresa tu contraseña") }
            return
        }

        // Iniciar conexión asíncrona
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = xmppClient.connect(
                phoneNumber = currentState.phone,
                password = currentState.password
            )

            if (result.isSuccess) {
                // Conexión exitosa: marcar como conectado
                _uiState.update { it.copy(isLoading = false, isConnected = true) }
            } else {
                // Error de conexión: mostrar mensaje al usuario
                val errorMessage = result.exceptionOrNull()?.message
                    ?: "Error desconocido al conectar"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Composable principal de la pantalla de login
// ----------------------------------------------------------------------------
/**
 * Pantalla de inicio de sesión / conexión de ToDus Messenger.
 *
 * Es la primera pantalla que ve el usuario. Permite ingresar su número
 * de teléfono ToDus (formato cubano 53XXXXXXXX, 10 dígitos) y su
 * contraseña para autenticarse contra el servidor XMPP.
 *
 * Al conectarse exitosamente, invoca el callback [onConnected] para
 * que la navegación avance a la pantalla principal de chats.
 *
 * @param onConnected Callback que se ejecuta cuando la conexión es exitosa.
 * @param viewModel Instancia de [LoginViewModel] inyectada por Hilt.
 */
@Composable
fun LoginScreen(
    onConnected: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    // Observar el estado reactivo del ViewModel
    val state by viewModel.uiState.collectAsState()

    // Cuando la conexión sea exitosa, navegar a la siguiente pantalla
    LaunchedEffect(state.isConnected) {
        if (state.isConnected) {
            onConnected()
        }
    }

    // Fondo completo con el color ToDus azul
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TodusBlue),
        contentAlignment = Alignment.Center
    ) {
        // Columna centrada verticalmente con padding horizontal
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Logo de la aplicación ---
            Text(
                text = "ToDus",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Messenger",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- Tarjeta blanca con el formulario ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título del formulario
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Campo de número de teléfono ---
                    val phoneHasError = state.error != null && state.phone.length < 10

                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Número ToDus") },
                        placeholder = { Text("53XXXXXXXX") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Número de teléfono"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        isError = phoneHasError,
                        supportingText = if (phoneHasError) {{
                            Text("Ingresa un número válido de 10 dígitos")
                        }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TodusBlue,
                            focusedLabelColor = TodusBlue,
                            cursorColor = TodusBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Campo de contraseña ---
                    val passwordHasError = state.error != null && state.password.isEmpty()

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Contraseña"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordHasError,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TodusBlue,
                            focusedLabelColor = TodusBlue,
                            cursorColor = TodusBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Botón de conectar ---
                    val isFormValid = state.phone.length >= 10 && state.password.isNotEmpty()

                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !state.isLoading && isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TodusBlue,
                            disabledContainerColor = TodusBlue.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            // Indicador de progreso mientras se conecta
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }

                        Text(
                            text = "Conectar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // --- Mensaje de error general ---
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = state.error,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
