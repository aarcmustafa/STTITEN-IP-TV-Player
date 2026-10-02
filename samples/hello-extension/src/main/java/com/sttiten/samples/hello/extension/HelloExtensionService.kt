package com.sttiten.iptv.samples.hello.extension

import android.content.Context
import android.content.res.Configuration
import com.sttiten.iptv.extension.api.ExtensionApiRange
import com.sttiten.iptv.extension.api.ExtensionApiVersions
import com.sttiten.iptv.extension.api.ExtensionCapabilityIds
import com.sttiten.iptv.extension.api.ExtensionCapabilityRequest
import com.sttiten.iptv.extension.api.ExtensionHookDeclaration
import com.sttiten.iptv.extension.api.ExtensionId
import com.sttiten.iptv.extension.api.ExtensionManifest
import com.sttiten.iptv.extension.api.ExtensionSemanticVersion
import com.sttiten.iptv.extension.api.ExtensionSettingField
import com.sttiten.iptv.extension.api.ExtensionSettingSchema
import com.sttiten.iptv.extension.api.ExtensionSettingSection
import com.sttiten.iptv.extension.api.ExtensionSettingType
import com.sttiten.iptv.extension.api.HostHookSpecs
import com.sttiten.iptv.extension.api.SettingsSchemaResult
import com.sttiten.iptv.extension.sdk.android.TypedExtensionService
import java.util.Locale
import kotlinx.serialization.json.JsonPrimitive

class HelloExtensionService : TypedExtensionService() {
    override val extensionManifest = ExtensionManifest(
        id = ExtensionId("com.sttiten.iptv.samples.hello"),
        displayName = "Hello Extension",
        extensionVersion = ExtensionSemanticVersion(1, 0, 0),
        apiRange = ExtensionApiRange(
            minimum = ExtensionApiVersions.Current,
            maximum = ExtensionApiVersions.Current,
        ),
        hooks = setOf(
            ExtensionHookDeclaration(
                hook = HostHookSpecs.SettingsSchema.hook,
                schemaVersion = HostHookSpecs.SettingsSchema.schemaVersion,
                requiredCapabilities = setOf(ExtensionCapabilityIds.SettingsContribute),
            )
        ),
        capabilities = setOf(
            ExtensionCapabilityRequest(
                capability = ExtensionCapabilityIds.SettingsContribute,
                reason = "Add settings for the current device type",
            )
        ),
        settingsSchema = ExtensionSettingSchema(
            version = 1,
            fields = listOf(
                ExtensionSettingField(
                    key = "greeting",
                    label = "Greeting",
                    type = ExtensionSettingType.TEXT,
                    defaultValue = JsonPrimitive("Hello from my extension"),
                )
            ),
        ),
        metadata = mapOf("developer" to "M3UAndroid sample"),
    )

    init {
        handle(HostHookSpecs.SettingsSchema) { request, _ ->
            val copy = helloSettingsCopy(this, request.localeTag, request.surface)
            SettingsSchemaResult(
                sections = listOf(
                    ExtensionSettingSection(
                        id = "device",
                        title = copy.sectionTitle,
                        schema = ExtensionSettingSchema(
                            version = 1,
                            fields = listOf(
                                ExtensionSettingField(
                                    key = "name",
                                    label = copy.fieldLabel,
                                    type = ExtensionSettingType.TEXT,
                                    description = copy.description,
                                    defaultValue = JsonPrimitive(copy.defaultValue),
                                )
                            ),
                        ),
                    )
                )
            )
        }
    }
}

private data class HelloSettingsCopy(
    val sectionTitle: String,
    val fieldLabel: String,
    val description: String,
    val defaultValue: String,
)

private fun helloSettingsCopy(
    context: Context,
    localeTag: String?,
    surface: String,
): HelloSettingsCopy {
    val localizedContext = context.forLocale(localeTag)
    return when (surface) {
        "phone" -> HelloSettingsCopy(
            sectionTitle = localizedContext.getString(R.string.hello_settings_section_device),
            fieldLabel = localizedContext.getString(R.string.hello_settings_phone_name),
            description = localizedContext.getString(R.string.hello_settings_phone_description),
            defaultValue = localizedContext.getString(R.string.hello_settings_phone_default),
        )
        "tv" -> HelloSettingsCopy(
            sectionTitle = localizedContext.getString(R.string.hello_settings_section_device),
            fieldLabel = localizedContext.getString(R.string.hello_settings_tv_name),
            description = localizedContext.getString(R.string.hello_settings_tv_description),
            defaultValue = localizedContext.getString(R.string.hello_settings_tv_default),
        )
        else -> HelloSettingsCopy(
            sectionTitle = localizedContext.getString(R.string.hello_settings_section_device),
            fieldLabel = localizedContext.getString(R.string.hello_settings_device_name),
            description = localizedContext.getString(R.string.hello_settings_device_description),
            defaultValue = localizedContext.getString(R.string.hello_settings_device_default),
        )
    }
}

private fun Context.forLocale(localeTag: String?): Context {
    val locale = localeTag
        ?.trim()
        ?.replace('_', '-')
        ?.takeIf(String::isNotEmpty)
        ?.let(Locale::forLanguageTag)
        ?.takeIf { candidate -> candidate.language.isNotEmpty() }
        ?: return this
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
    }
    return createConfigurationContext(configuration)
}
