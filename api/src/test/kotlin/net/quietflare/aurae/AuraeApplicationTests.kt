package net.quietflare.aurae

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals

@IntegrationTest
class AuraeApplicationTests {

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Test
    fun contextLoads() {
    }

    @Test
    fun `the application talks to postgres`() {
        assertEquals(1, jdbc.queryForObject("select 1", Int::class.java))
    }
}
