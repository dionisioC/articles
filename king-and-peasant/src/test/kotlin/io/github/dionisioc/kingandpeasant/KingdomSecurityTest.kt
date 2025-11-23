package io.github.dionisioc.kingandpeasant

import io.github.dionisioc.kingandpeasant.config.SecurityConfiguration
import io.github.dionisioc.kingandpeasant.controller.KingdomController
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

@WebMvcTest(KingdomController::class)
@Import(SecurityConfiguration::class)
class KingdomSecurityTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    fun `Barbarian (No Cert) is rejected at the gate`() {
        mvc.perform(get("/peasant"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `Citizen (Cert Only) can talk to Peasant`() {
        mvc.perform(
            get("/peasant")
                .with(mockX509("CN=Bob"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Greetings")))
    }

    @Test
    fun `Citizen (Cert Only) is Forbidden from the Castle`() {
        mvc.perform(
            get("/king")
                .with(mockX509("CN=Bob"))
        )
            .andExpect(status().isForbidden) // 403: Identity known, permissions denied
    }

    @Test
    fun `King (Cert + Token) is welcomed`() {
        mvc.perform(
            get("/king")
                .with(mockX509("CN=Arthur")) // 1. Identity
                .header("Authorization", "Bearer MyRoyalSecret") // 2. Permissions
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Your Majesty")))
    }

    private fun mockX509(dn: String): RequestPostProcessor {
        val cert = mock<X509Certificate>()
        val principal = X500Principal(dn)

        whenever(cert.subjectX500Principal).thenReturn(principal)
        whenever(cert.subjectDN).thenReturn(principal)

        return SecurityMockMvcRequestPostProcessors.x509(cert)
    }
}