package coffee.cypher.hexapi_extractor

import javassist.ClassPool
import javassist.CtNewMethod
import javassist.LoaderClassPath
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.system.exitProcess

class RegistryTransformer(val targetName: String, val propFile: String, val patternRegex: Regex) : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        try {
            if (className != targetName.replace('.', '/')) {
                return super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
            }

            log("Running registry transformer on: $className")

            val pool = ClassPool.getDefault()
            pool.appendClassPath(LoaderClassPath(loader))
            pool.appendClassPath(LoaderClassPath(this::class.java.classLoader))
            val cl = pool[targetName]

            val initMethod = cl.declaredMethods.first { it.name == "onInitialize" }
            initMethod.insertAfter(
                "coffee.cypher.hexapi_extractor.Hooks.initDone();",
                false,
                cl.isKotlin
            )

            log("Injected hook into ${cl.name}::${initMethod.longName}")

            return cl.toBytecode().also { cl.detach() }
        } catch (e: Throwable) {
            log("Transformer error: $e")
            exitProcess(-1)
        }
    }

}