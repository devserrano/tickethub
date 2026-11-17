package com.tickethub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tickethub.config.CorsProperties;
import com.tickethub.config.JwtProperties;
import com.tickethub.entity.User;
import com.tickethub.enums.Role;
import com.tickethub.exception.GlobalExceptionHandler;
import com.tickethub.repository.UserRepository;
import com.tickethub.security.JwtService;
import com.tickethub.security.SecurityConfig;
import com.tickethub.service.impl.AuthServiceImpl;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Flujo de autenticación completo (controller + seguridad + service) sin base de datos:
 * el repositorio se simula, así corre igual en CI sin MySQL.
 */
@WebMvcTest(AuthController.class)
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class}) // el slice no corre @ConfigurationPropertiesScan
@Import({SecurityConfig.class, JwtService.class, AuthServiceImpl.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository users;

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private final Map<String, User> db = new java.util.HashMap<>();

    @BeforeEach
    void seed() {
        // Mismo formato que DataSeeder: hash BCrypt guardado tal cual
        add(1L, "admin@tickethub.local", "Administrador", Role.ADMIN, "Admin123", true);
        add(2L, "usuario@tickethub.local", "Usuario Demo", Role.USER, "Usuario123", true);
        add(3L, "agente@tickethub.local", "Agente Demo", Role.AGENT, "Agente123", true);
        add(4L, "baja@tickethub.local", "Inactivo", Role.USER, "Baja1234", false);

        when(users.findByEmailIgnoreCase(anyString()))
                .thenAnswer(i -> Optional.ofNullable(db.get(((String) i.getArgument(0)).toLowerCase())));
        when(users.existsByEmailIgnoreCase(anyString()))
                .thenAnswer(i -> db.containsKey(((String) i.getArgument(0)).toLowerCase()));
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId((long) db.size() + 10);
            db.put(u.getEmail(), u);
            return u;
        });
    }

    private void add(Long id, String email, String name, Role role, String password, boolean active) {
        User u = new User(name, email, BCRYPT.encode(password), role);
        u.setId(id);
        u.setActive(active);
        db.put(email, u);
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    private static Cookie session(MvcResult r) {
        return r.getResponse().getCookie("access_token");
    }

    @Test
    void loginConUsuarioSemillaBcryptDejaCookieHttpOnlyYNoDevuelveElToken() throws Exception {
        MvcResult r = login("Admin@TicketHub.local", "Admin123");
        assertThat(r.getResponse().getStatus()).isEqualTo(200);

        String setCookie = r.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith("access_token=")).findFirst().orElseThrow();
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Max-Age=3600");
        assertThat(r.getResponse().getContentAsString())
                .contains("\"role\":\"ADMIN\"").contains("\"name\":\"Administrador\"").doesNotContain("eyJ");
    }

    @Test
    void contrasenaIncorrectaCorreoInexistenteEInactivoDanElMismo401() throws Exception {
        MvcResult wrongPass = login("admin@tickethub.local", "Incorrecta1");
        MvcResult noUser = login("nadie@tickethub.local", "Incorrecta1");
        MvcResult inactive = login("baja@tickethub.local", "Baja1234");
        for (MvcResult r : new MvcResult[] {wrongPass, noUser, inactive}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(401);
            assertThat(session(r)).isNull();
        }
        assertThat(wrongPass.getResponse().getContentAsString()).isEqualTo(noUser.getResponse().getContentAsString());
    }

    @Test
    void datosInvalidosDan400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-correo\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void meSinCookieDa401YConCookieDevuelveElUsuario() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/auth/me").cookie(session(login("agente@tickethub.local", "Agente123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.email").value("agente@tickethub.local"));
    }

    @Test
    void tokenAlteradoDa401() throws Exception {
        // Se cambia el payload (p. ej. alguien intenta subirse de rol): la firma ya no coincide
        String[] parts = session(login("usuario@tickethub.local", "Usuario123")).getValue().split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"usuario@tickethub.local\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("access_token", forged)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users").cookie(new Cookie("access_token", forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaDeAdminConRolUserDa403() throws Exception {
        Cookie s = session(login("usuario@tickethub.local", "Usuario123"));
        mvc.perform(get("/api/v1/admin/users").cookie(s)).andExpect(status().isForbidden());
    }

    @Test
    void registroCreaUsuarioUserYRechazaDuplicado() throws Exception {
        String json = "{\"name\":\"Prueba Registro\",\"email\":\"nuevo@tickethub.local\",\"password\":\"Segura123\"}";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
        assertThat(login("nuevo@tickethub.local", "Segura123").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void registroRechazaContrasenaSinMayusculaONumero() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@tickethub.local\",\"password\":\"sinmayuscula\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void logoutSinCsrfDa403YConCsrfBorraLaCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("XSRF-TOKEN", "abc")).header("X-XSRF-TOKEN", "abc"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")));
    }

    @Test
    void corsPermiteAlFrontEnLiveServerConCookies() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5500")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://sitio-malo.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
