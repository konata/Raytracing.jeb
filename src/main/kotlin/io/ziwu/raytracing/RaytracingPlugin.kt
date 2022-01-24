package io.ziwu.raytracing

import com.pnfsoftware.jeb.core.*
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit
import com.pnfsoftware.jeb.util.logging.GlobalLog
import proguard.retrace.FrameInfo
import java.io.File
import kotlin.system.measureTimeMillis

data class Rebasing(val from: String, val to: String, val id: Long)

class RaytracingPlugin : AbstractEnginesPlugin() {
    companion object {
        private val logger = GlobalLog.getLogger(RaytracingPlugin::class.java).also {
            it.info(" *** Raytracing *** ")
        }

        fun fromQualified(name: String): Pair<String?, String>? {
            if (name.isBlank()) return null
            logger.info("from qualified: $name")
            val components = name.split("""\.(?!.*?\.)""".toRegex())
            val (clazzName, packageName) = (listOf(null) + components).reversed().take(2)
            return packageName to (clazzName ?: "")
        }

        fun fromShorty(shortyRepr: String): String {
            val shorty = shortyRepr.trim()
            return when {
                shorty == "Z" -> "boolean"
                shorty == "B" -> "byte"
                shorty == "C" -> "char"
                shorty == "S" -> "short"
                shorty == "I" -> "int"
                shorty == "J" -> "long"
                shorty == "F" -> "float"
                shorty == "D" -> "double"
                shorty == "V" -> "void"
                shorty.length <= 1 -> "???".also { logger.warning("what the fuck is that type: $shorty") }
                else -> {
                    val head = shorty.trim().first()
                    val trailing = shorty.trim().substring(1)
                    when (head) {
                        '[' -> "${fromShorty(trailing)}[]"
                        'L' -> trailing.replace("""^L|;*$""".toRegex(), "").replace("/", ".")
                        else -> "???".also { logger.warning("wtf is that type: $shorty") }
                    }
                }
            }
        }
    }

    override fun getPluginInformation() = PluginInformation(
        "Raytracing", "Retracing dex by supplying mapping.txt", "Ziwu Security Lab", Version.create(0, 0, 1)
    )

    override fun getExecutionOptionDefinitions() = mutableListOf(
        OptionDefinition("mapping", "specify mapping file location")
    )

    override fun execute(context: IEnginesContext?) = execute(context, null)
    override fun execute(context: IEnginesContext?, option: MutableMap<String, String>?) = kotlin.runCatching {
        measureTimeMillis {
            option?.get("mapping")?.trim()?.takeIf { File(it).exists() }?.let {
                val mapper = Retracer.feed(File(it))
                context?.getProject(0)?.let { prj ->
                    val codes = RuntimeProjectUtil.findUnitsByType(prj, IDexUnit::class.java, false)
                    codes?.forEach { dex ->
                        val records = dex.classes.mapNotNull { clazz ->
                            val qualifiedName = fromShorty(clazz.signature) // TODO
                            mapper(FrameInfo(qualifiedName, null, 0, null, null, null, null)).firstOrNull()
                                ?.let(FrameInfo::getClassName)?.let { moved ->
                                    Rebasing(qualifiedName, moved, clazz.itemId) to clazz.methods.mapNotNull { method ->
                                        // method
                                        mapper(
                                            FrameInfo(qualifiedName,
                                                null,
                                                0,
                                                fromShorty(method.returnType.address),
                                                null,
                                                method.name,
                                                method.parameterTypes.joinToString(
                                                    ",",
                                                ) { argument ->
                                                    fromShorty(argument.address)
                                                })
                                        ).firstOrNull()?.methodName?.let { name ->
                                            Rebasing(
                                                method.name, name, method.itemId
                                            )
                                        }
                                    } + clazz.fields.mapNotNull { field ->
                                        mapper(
                                            FrameInfo(
                                                qualifiedName,
                                                null,
                                                0,
                                                fromShorty(field.fieldType.address.toString()),
                                                field.name,
                                                null,
                                                null
                                            )
                                        ).firstOrNull()?.fieldName?.let { fieldName ->
                                            Rebasing(field.name, fieldName, field.itemId)
                                        }
                                    }
                                }
                        }

                        records.forEach { (clazz, members) ->
                            val (from, to, id) = clazz
                            val members = members.filter { member -> member.from != member.to }
                            logger.info("members of ${from}->${to} member counts: ${members.size}")
                            members.forEach { (from, to, id) ->
                                logger.info("rename member $from => $to in ${clazz.from}->${clazz.to}")
                                Renamer.rename(dex, to, id)
                            }
                            // TODO() judge package name and create package & move class
                            // TODO() createPackage & removePackage did receive dot-style namings?
                        }

                        logger.error("******  begin package process ******")

                        records.map { (clazz, _) ->
                            val (from, to) = listOf(
                                clazz.from, clazz.to
                            ).map { path -> fromQualified(path) }
                            to?.first?.takeIf { it != from?.first && it.isNotBlank() }
                        }.toSet().forEach { packageName ->
                            packageName?.let { newlyCreated ->
                                logger.info("create package: $newlyCreated")
                                Renamer.createPackage(dex, newlyCreated)
                            }
                        }

                        records.forEach { (clazz, _) ->
                            // rename & move class
                            val (from, to, id) = clazz
                            if (from != to && to.isNotBlank()) {
                                fromQualified(to)?.let { (pkg, clazz) ->
                                    Renamer.rename(dex, clazz, id)
                                    logger.info("move class $clazz to package: $pkg")
                                    pkg?.let {
                                        Renamer.move(dex, pkg, id)
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: logger.error("mapping file not found !!!")
        }
    }.onFailure { error ->
        logger.catching(error, "Whooo")
        logger.error("[E]: Raytracing Cause: ${error.cause}")
        logger.error("[E]: Raytracing ${error.stackTrace.joinToString("\n") { it.toString() }}")
    }.onSuccess { elapsed ->
        logger.info("Raytracing success: take time -> $elapsed ms")
    }.let { }
}