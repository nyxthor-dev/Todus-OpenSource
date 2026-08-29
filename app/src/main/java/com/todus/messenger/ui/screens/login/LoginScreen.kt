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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todus.messenger.data.remote.auth.ToDusAuthService
import com.todus.messenger.data.remote.xmpp.ToDusXmppClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TodusBlue = Color(0xFF0066CC)

/**
 * Estado del login. Solo requiere teléfono.
 * El JWT se obtiene automáticamente via auth.todus.cu.
 */
data class LoginUiState(
    val phone: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false
)

/**
 * ViewModel de login: auth/token → JWT → XMPP connect.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: ToDusAuthService,
    private val xmppClient: ToDusXmppClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChanged(phone: String) {
        val filtered = phone.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phone = filtered, error = null) }
    }

    /**
     * 1. Obtener JWT via auth/todus.cu/v2/auth/token (protobuf)
     * 2. Conectar XMPP con phone + JWT como password
     */
    fun connect() {
        val state = _uiState.value

        if (state.phone.length != 10) {
            _uiState.update { it.copy(error = "El número debe tener 10 dígitos (53XXXXXXXX)") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Paso 1: Obtener JWT
            val authResult = authService.authenticate(state.phone)
            if (authResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = authResult.exceptionOrNull()?.message
                            ?: "Error al obtener token"
                    )
                }
                return@launch
            }

            val jwt = authResult.getOrThrow().jwt

            // Paso 2: Conectar XMPP con JWT
            val xmppResult = xmppClient.connect(
                phoneNumber = state.phone,
                password = jwt
            )

            if (xmppResult.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isConnected = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = xmppResult.exceptionOrNull()?.message
                            ?: "Error al conectar"
                    )
                }
            }
        }
    }
}

/**
 * Pantalla de login - solo número de teléfono.
 * El token JWT se obtiene automáticamente (auth/token protobuf).
 */
@Composable
fun LoginScreen(
    onConnected: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TodusBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ingresa tu número ToDus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val phoneHasError = state.error != null && state.phone.length < 10

                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Número ToDus") },
                        placeholder = { Text("53XXXXXXXX") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Teléfono"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        isError = phoneHasError,
                        supportingText = if (phoneHasError) {{
                            Text("10 dígitos (53 + tu número)")
                        }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TodusBlue,
                            focusedLabelColor = TodusBlue,
                            cursorColor = TodusBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !state.isLoading && state.phone.length >= 10,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TodusBlue,
                            disabledContainerColor = TodusBlue.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }

                        Text(
                            text = if (state.isLoading) "Conectando..." else "Conectar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = state.error!!,
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