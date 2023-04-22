package coffee.cypher.hexapi_extractor

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.io.FileNotFoundException
import java.lang.instrument.Instrumentation
import java.util.Properties

//TODO cause of class loading bullshit I can only pass values via hook args

fun premain(args: String, inst: Instrumentation) {
    try {
        val props = Properties().apply { load(File(args).reader()) }

        val buildDir = props.getProperty("buildDir")

        log("Executing premain")

        val params = when {
            File(buildDir, "resources/main/fabric.mod.json").exists() -> getFabricHandle(
                File(
                    buildDir,
                    "resources/main/fabric.mod.json"
                )
            )

            File(buildDir, "resources/main/quilt.mod.json").exists() -> getQuiltHandle(
                File(
                    buildDir,
                    "resources/main/quilt.mod.json"
                )
            )

            else -> throw FileNotFoundException("Could not find fabric.mod.json or quilt.mod.json")
        }

        log("Scan parameters set to $params")

        inst.addTransformer(RegistryTransformer(params.initClass, args, params.patternRegex), true)
    } catch (e: Throwable) {
        log("Agent error: $e")
    }
}

fun log(msg: Any?) {
    println("[HexAPI Extractor] $msg")
}

fun getQuiltHandle(qmjFile: File): ScanParameters {
    val qmj = JSONParser().parse(qmjFile.reader()) as JSONObject
    val id = qmj["id"] as String
    val init = when (val entrypoint = (qmj["entrypoints"] as JSONObject)["init"]) {
        is JSONObject -> entrypoint["value"] as String
        is JSONArray -> when (val actualEntrypoint = entrypoint[0]) {
            is JSONObject -> actualEntrypoint["value"] as String
            else -> actualEntrypoint as String
        }
        else -> entrypoint as String
    }

    return ScanParameters(Regex("^$id"), init)
}

fun getFabricHandle(fmjFile: File): ScanParameters {
    val qmj = JSONParser().parse(fmjFile.reader()) as JSONObject
    val id = qmj["id"] as String
    val init = when (val entrypoint = (qmj["entrypoints"] as JSONObject)["main"]) {
        is JSONObject -> entrypoint["value"] as String
        is JSONArray -> when (val actualEntrypoint = entrypoint[0]) {
            is JSONObject -> actualEntrypoint["value"] as String
            else -> actualEntrypoint as String
        }
        else -> entrypoint as String
    }

    return ScanParameters(Regex("^$id"), init)
}

data class ScanParameters(val patternRegex: Regex, val initClass: String)