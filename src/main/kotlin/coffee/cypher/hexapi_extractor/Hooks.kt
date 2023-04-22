package coffee.cypher.hexapi_extractor

import at.petrak.hexcasting.api.PatternRegistry
import at.petrak.hexcasting.api.spell.math.HexPattern
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.system.exitProcess

object Hooks {
    @JvmStatic
    fun initDone() {
        try {
            val props = Properties()
            props["outFile"] = "pattern_dump.json"

            val scanParams = object {
                val patternRegex = Regex("^hexal:")
            }

            val outFile = File(props.getProperty("outFile"))
            val outJson = JsonArray()

            val patternMapField = PatternRegistry::class.java.getDeclaredField("regularPatternLookup")
            patternMapField.isAccessible = true
            val patternMap = patternMapField[null] as Map<String, Any>

            val actionMapField = PatternRegistry::class.java.getDeclaredField("actionLookup")
            actionMapField.isAccessible = true
            val actionMap = actionMapField[null] as Map<Any, Any>

            val perWorldMapField = PatternRegistry::class.java.getDeclaredField("perWorldPatternLookup")
            perWorldMapField.isAccessible = true
            val perWorldMap = perWorldMapField[null] as Map<Any, Any>

            val regularPatternClass = Class.forName("at.petrak.hexcasting.api.PatternRegistry\$RegularEntry")
            val regularPatternDirField = regularPatternClass.getDeclaredField("preferredStart")
            regularPatternDirField.isAccessible = true

            val regularPatternIdField = regularPatternClass.getDeclaredField("opId")
            regularPatternIdField.isAccessible = true

            val perWorldPatternClass = Class.forName("at.petrak.hexcasting.api.PatternRegistry\$PerWorldEntry")
            val perWorldPrototypeField = perWorldPatternClass.getDeclaredField("prototype")
            perWorldPrototypeField.isAccessible = true

            for ((k, v) in patternMap) {
                val patternId = regularPatternIdField[v]
                if (!patternId.toString().contains(scanParams.patternRegex)) {
                    continue
                }
                val patternObj = JsonObject()
                patternObj.add("id", JsonPrimitive(patternId.toString()))
                patternObj.add("angleSignature", JsonPrimitive(k))
                patternObj.add("isPerWorld", JsonPrimitive(false))
                patternObj.add("defaultStartDir", JsonPrimitive(regularPatternDirField[v].toString()))
                val action = actionMap.getValue(patternId)
                patternObj.add("className", JsonPrimitive(action.javaClass.canonicalName))
                val classReader = ClassReader(action.javaClass.canonicalName)
                var sourceFile: String? = null

                classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitSource(source: String?, debug: String?) {
                        sourceFile = source
                    }
                }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)

                patternObj.add("rawSource", JsonPrimitive(sourceFile))
                outJson.add(patternObj)
            }
            for ((k, v) in perWorldMap) {
                if (!k.toString().contains(scanParams.patternRegex)) {
                    continue
                }
                val patternObj = JsonObject()
                patternObj.add("isPerWorld", JsonPrimitive(true))
                patternObj.add("id", JsonPrimitive(k.toString()))
                val prototypePattern = perWorldPrototypeField[v] as HexPattern
                patternObj.add("angleSignature", JsonPrimitive(prototypePattern.anglesSignature()))
                patternObj.add("defaultStartDir", JsonPrimitive(prototypePattern.startDir.toString()))
                val action = actionMap.getValue(k)
                patternObj.add("className", JsonPrimitive(action.javaClass.canonicalName))
                val classReader = ClassReader(action.javaClass.canonicalName)
                var sourceFile: String? = null

                classReader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitSource(source: String?, debug: String?) {
                        sourceFile = source
                    }
                }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)

                patternObj.add("rawSource", JsonPrimitive(sourceFile))
            }
            Files.writeString(outFile.toPath(), outJson.toString())
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

        exitProcess(0)
    }
}