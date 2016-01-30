package ninja.mpnguyen

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Paths

fun TestData() : TestData {

    val bytes = Files.readAllBytes(Paths.get("credentials.json"))
    val json = String(bytes,"UTF-8")
    val mapper = ObjectMapper().registerModule(KotlinModule())
    val type = object : TypeReference<TestData>() {}
    return mapper.readValue<TestData>(json, type)
}

data class TestData(
        val username : String,
        val password : String,
        val script_id : String,
        val script_secret : String,
        val webapp_id : String,
        val webapp_secret : String,
        val installed_id : String,
        val callback : String
)
